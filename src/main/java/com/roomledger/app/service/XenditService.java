package com.roomledger.app.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.PaymentTransaction;
import com.roomledger.app.model.WebhookInbox;
import com.roomledger.app.repository.PaymentRepository;
import com.roomledger.app.repository.PaymentTransactionRepository;
import com.roomledger.app.repository.WebhookInboxRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class XenditService {

    @Value("${xendit.webhook.token}")
    private String configuredToken;

    private final WebhookInboxRepository inboxRepo;
    private final PaymentRepository paymentRepo;
    private final PaymentTransactionRepository trxRepo;

    private static final ObjectMapper M = new ObjectMapper();


    public XenditService(WebhookInboxRepository inboxRepo,
                                PaymentRepository paymentRepo,
                                PaymentTransactionRepository trxRepo) {
        this.inboxRepo = inboxRepo;
        this.paymentRepo = paymentRepo;
        this.trxRepo = trxRepo;
    }

    public void verifyTokenOrThrow(String token) {
        if (configuredToken == null || configuredToken.isBlank())
            throw new IllegalStateException("Callback token not configured on server");
        if (token == null || !configuredToken.equals(token))
            throw new SecurityException("Invalid x-callback-token");
    }

    /** Accept + persist inbox; then process idempotently. Always safe to call multiple times. */
    @Transactional
    public ProcessResult accept(Map<String, Object> payload) throws InvalidTransactionException {
        String provider = "XENDIT";
        String eventId = extractEventId(payload);
        if (eventId == null) eventId = extractProviderPaymentId(payload);
        if (eventId == null) eventId = String.valueOf(payload.hashCode());

        WebhookInbox box = new WebhookInbox();
        box.setProvider(provider);
        box.setEventId(eventId);
        box.setPayload(toJson(payload));
        try {
            box = inboxRepo.save(box);
        } catch (DataIntegrityViolationException dup) {
            return new ProcessResult("duplicate webhook (ignored)");
        }

        UpsertOutcome outcome = upsertTransactionAndUpdatePayment(payload);

        box.setProcessed(true);
        inboxRepo.save(box);

        return new ProcessResult(outcome.message());
    }




    @Transactional
    protected UpsertOutcome upsertTransactionAndUpdatePayment(Map<String, Object> payload)
            throws InvalidTransactionException {
        final RoundingMode RULE = RoundingMode.HALF_UP;

        String prId      = extractPaymentRequestId(payload);
        String refId     = extractReferenceId(payload);            // booking_id (UUID string)
        String payId     = extractProviderPaymentId(payload);      // must exist
        String channel   = extractString(payload, "data.channel_code");
        String status    = extractStatus(payload);
        Long   paidMinor = extractAmountMinor(payload);            // integer rupiah from provider
        String currency  = extractString(payload, "data.currency");
        LocalDateTime paidAt = extractPaidAt(payload);

        if (payId == null || payId.isBlank()) {
            throw new InvalidTransactionException("providerPaymentId (payId) is required but missing");
        }
        if (refId == null) {
            throw new InvalidTransactionException("booking_id (refId) is required but missing");
        }

        final UUID bookingId;
        try {
            bookingId = UUID.fromString(refId);
        } catch (IllegalArgumentException e) {
            throw new InvalidTransactionException("refId is not a valid booking UUID: " + refId);
        }

        //Load all WAITING payments (DEPOSIT/RENT) for this booking
        List<Payment> waiting = paymentRepo
                .findByBookingIdAndStatus(bookingId, Payment.Status.WAITING_FOR_PAYMENT)
                .stream()
                .filter(p -> p.getType() == Payment.Type.DEPOSIT || p.getType() == Payment.Type.RENT)
                .toList();

        if (waiting.isEmpty()) {
            throw new InvalidTransactionException("No WAITING (DEPOSIT/RENT) payments for booking " + bookingId);
        }

        //Sum expected amount (normalize to integer rupiah once)
        long expectedMinor = waiting.stream()
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .map(a -> a.setScale(0, RULE))
                .mapToLong(BigDecimal::longValueExact)
                .sum();

        if (paidMinor == null) {
            throw new InvalidTransactionException("Provider amount is missing");
        }
        if (paidMinor != expectedMinor) {
            throw new InvalidTransactionException(
                    "Total Amount is not equal to %d : %d (booking=%s)".formatted(paidMinor, expectedMinor, bookingId));
        }

        // For each Payment, find exactly one existing PaymentTransaction by payment_id
        List<PaymentTransaction> txList = new ArrayList<>(waiting.size());
        Map<UUID, Long> amountByPayment = new HashMap<>();
        for (Payment p : waiting) {
            List<PaymentTransaction> matches = trxRepo.findByPaymentId(p.getId()); // param type must be UUID
            if (matches.isEmpty()) {
                throw new InvalidTransactionException("Missing PaymentTransaction for payment " + p.getId());
            }
            if (matches.size() > 1) {
                // If you prefer to pick the latest instead of throwing, sort by created_at and take last.
                throw new InvalidTransactionException(
                        "Multiple PaymentTransactions found for payment " + p.getId() + " (expected 1)");
            }
            txList.add(matches.getFirst());
            // normalize each payment amount to integer rupiah
            amountByPayment.put(p.getId(), p.getAmount().setScale(0, RULE).longValueExact());
        }

        // Update each transaction from its matching payment
        for (Payment p : waiting) {
            PaymentTransaction tx = txList.stream()
                    .filter(t -> {
                        UUID pid = t.getPayment().getId();
                        return p.getId().equals(pid);
                    })
                    .findFirst()
                    .orElseThrow(() -> new InvalidTransactionException(
                            "Internal error: matched PaymentTransaction not found for payment " + p.getId()));

            if (channel  != null) tx.setChannelCode(channel);
            if (currency != null) tx.setCurrency(currency);
            if (status   != null) tx.setStatus(status);
            if (paidAt   != null) tx.setPaidAt(paidAt);

            tx.setAmount(amountByPayment.get(p.getId())); // set per-payment amount (not the total)
            if (prId  != null && (tx.getPaymentRequestId() == null || tx.getPaymentRequestId().isBlank())) {
                tx.setPaymentRequestId(prId);
            }
            if (tx.getReferenceId() == null || tx.getReferenceId().isBlank()) {
                tx.setReferenceId(refId); // booking_id for traceability
            }
            // if providerPaymentId is blank, fill it; else keep (idempotent updates)
            if (tx.getProviderPaymentId() == null || tx.getProviderPaymentId().isBlank()) {
                tx.setProviderPaymentId(payId);
            }
            // Ensure owner/building come from Payments table
            tx.setOwner(p.getOwner());
            if (p.getBuilding() != null) {
                tx.setBuilding(p.getBuilding());
            }
            tx.setPayload(payload != null ? payload : Map.of());

            if (tx.getOwner() == null) {
                throw new InvalidTransactionException(
                        "Refusing to save PaymentTransaction without owner (payment=" + p.getId() + ", booking=" + bookingId + ")");
            }
        }

        // Save all transactions in one go
        trxRepo.saveAll(txList);

        // Reflect provider status into ALL matched payments
        if (status != null) {
            switch (status.toUpperCase(Locale.ROOT)) {
                case "SUCCEEDED", "PAID" -> {
                    LocalDateTime effectivePaidAt = (paidAt != null ? paidAt : LocalDateTime.now(ZoneOffset.UTC));
                    for (Payment p : waiting) {
                        p.setStatus(Payment.Status.PAID);
                        p.setPaidAt(effectivePaidAt);
                    }
                    paymentRepo.saveAll(waiting);
                }
                case "EXPIRED" -> {
                    waiting.forEach(p -> p.setStatus(Payment.Status.EXPIRED));
                    paymentRepo.saveAll(waiting);
                }
                case "FAILED" -> {
                    waiting.forEach(p -> p.setStatus(Payment.Status.FAILED));
                    paymentRepo.saveAll(waiting);
                }
                default -> { /* ignore interim statuses */ }
            }
        }

        return new UpsertOutcome("updated");
    }


    /* ===== helpers ===== */

    private String extractEventId(Map<String, Object> payload) {

        String v = extractString(payload, "id");
        if (v != null) return v;
        return extractString(payload, "event_id");
    }

    private String extractProviderPaymentId(Map<String, Object> payload) {
        String v = extractString(payload, "data.business_id");
        if (v != null) return v;
        return extractString(payload, "id");
    }

    private String extractPaymentRequestId(Map<String, Object> payload) {
        String v = extractString(payload, "data.payment_request_id");
        if (v != null) return v;
        return extractString(payload, "payment_request_id");
    }

    private String extractReferenceId(Map<String, Object> payload) {
        String v = extractString(payload, "data.reference_id");
        if (v != null) return v;
        return extractString(payload, "reference_id");
    }

    private String extractStatus(Map<String, Object> payload) {
        String v = extractString(payload, "data.status");
        if (v != null) return v;
        return extractString(payload, "status");
    }

    private Long extractAmountMinor(Map<String, Object> payload) {
        // amounts might be number/int/long in JSON
        Object o = extractObject(payload, "data.request_amount");
        if (o instanceof Number n) return n.longValue();
        return null;
    }

    private LocalDateTime extractPaidAt(Map<String, Object> payload) {
        String s = extractString(payload, "data.paid_at");
        if (s == null) s = extractString(payload, "paid_at");
        if (s == null) return null;
        try {
            return OffsetDateTime.parse(s).toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(s);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private JsonNode toJson(Map<String, Object> payload) {
        try { return (JsonNode) payload; }
        catch (Exception e) {  return M.createObjectNode(); }
    }

    @SuppressWarnings("unchecked")
    private JsonNode toJsonMap(Map<String, Object> payload) {
        return (JsonNode) payload;
    }

    /* ---- tiny JSON path utils ---- */
    @SuppressWarnings("unchecked")
    private Object extractObject(Map<String,Object> root, String path) {
        String[] parts = path.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (!(cur instanceof Map<?,?> m)) return null;
            cur = m.get(p);
            if (cur == null) return null;
        }
        return cur;
    }
    private String extractString(Map<String,Object> root, String path) {
        Object o = extractObject(root, path);
        return (o == null) ? null : String.valueOf(o);
    }

    public record ProcessResult(String message) {}
    private record UpsertOutcome(String message) {}
}

