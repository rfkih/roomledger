package com.roomledger.app.service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.*;
import java.util.Map;
import java.util.Optional;

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
    public ProcessResult accept(Map<String, Object> payload) {
        String provider = "XENDIT";
        String eventId = extractEventId(payload);            // stable ID for inbox de-dup
        if (eventId == null) eventId = extractProviderPaymentId(payload); // fallback
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


    private UpsertOutcome upsertTransactionAndUpdatePayment(Map<String, Object> payload) {
        String prId     = extractPaymentRequestId(payload);
        String refId    = extractReferenceId(payload);
        String payId    = extractProviderPaymentId(payload);
        String channel  = extractString(payload, "data.channel_code");
        String status   = extractStatus(payload);
        Long   amount   = extractAmountMinor(payload);
        String currency = extractString(payload, "data.currency");
        LocalDateTime paidAt = extractPaidAt(payload);

        PaymentTransaction trx = trxRepo.findByProviderPaymentId(payId).orElseGet(PaymentTransaction::new);
        boolean isNew = trx.getId() == null;

        if (isNew) {
            trx.setProvider("XENDIT");
            trx.setProviderPaymentId(payId != null ? payId : "unknown");
            trx.setPaymentRequestId(prId);
            trx.setReferenceId(refId);
            trx.setChannelCode(channel);
        }

        if (amount != null) trx.setAmount(amount);
        if (currency != null) trx.setCurrency(currency);
        if (status != null) trx.setStatus(status);
        if (paidAt != null) trx.setPaidAt(paidAt);
        trx.setPayload(payload != null ? payload : Map.of());

        Optional<Payment> maybe = Optional.empty();
        if (prId != null) maybe = paymentRepo.findByPrId(prId);
        if (maybe.isEmpty() && refId != null) maybe = paymentRepo.findByReferenceId(refId);

        if (maybe.isPresent()) {
            Payment p = maybe.get();
            trx.setPayment(p);
            log.info("Linked transaction {} to owner {}", trx.getId(), p.getOwner().getId());
            trx.setOwner(p.getOwner());
            trx.setBuilding(p.getBuilding());

            if ("SUCCEEDED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
                p.setStatus(Payment.Status.PAID);
                p.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now(ZoneOffset.UTC));
                if (payId != null) p.setProviderPaymentId(payId);
                paymentRepo.save(p);
            } else if ("EXPIRED".equalsIgnoreCase(status)) {
                p.setStatus(Payment.Status.EXPIRED);
                paymentRepo.save(p);
            } else if ("FAILED".equalsIgnoreCase(status)) {
                p.setStatus(Payment.Status.FAILED);
                paymentRepo.save(p);
            }
        }
        trxRepo.save(trx);
        return new UpsertOutcome(isNew ? "inserted" : "updated");
    }

    /* ===== helpers ===== */

    private String extractEventId(Map<String, Object> payload) {

        String v = extractString(payload, "id");
        if (v != null) return v;
        return extractString(payload, "event_id");
    }

    private String extractProviderPaymentId(Map<String, Object> payload) {
        String v = extractString(payload, "data.id");
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
        Object o = extractObject(payload, "data.amount");
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

