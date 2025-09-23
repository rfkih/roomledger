package com.roomledger.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomledger.app.client.XenditClientService;
import com.roomledger.app.dto.*;
import com.roomledger.app.exthandler.InvalidInputException;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.*;
import com.roomledger.app.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentService {

    private final BookingRepository bookingRepo;
    private final PaymentRepository paymentRepo;
    private final XenditClientService xendit;
    private final PaymentAttemptRepository attemptRepo;              // if you keep attempts
    private final CustomerPaymentCodeRepository codeRepo;            // reusable VA/QR
    private final ObjectMapper M;
    private final PaymentTransactionRepository paymentTransactionRepo;

    private final ClockService clock;

    public PaymentService(BookingRepository bookings,
                          PaymentRepository payments, XenditClientService xendit, PaymentAttemptRepository attemptRepo, CustomerPaymentCodeRepository codeRepo, ObjectMapper m, PaymentTransactionRepository paymentTransactionRepo,
                          ClockService clock) {
        this.bookingRepo = bookings;
        this.paymentRepo = payments;
        this.xendit = xendit;
        this.attemptRepo = attemptRepo;
        this.codeRepo = codeRepo;
        M = m;
        this.paymentTransactionRepo = paymentTransactionRepo;
        this.clock = clock;
    }



    @Transactional
    public InquiryPaymentResponse inquiryPayment(UUID bookingId, Payment.Type paymentType)
            throws InvalidTransactionException {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        List<Payment> payments = new ArrayList<>(
                paymentRepo.findByBookingIdAndStatus(booking.getId(), Payment.Status.WAITING_FOR_PAYMENT)
        );

        if (payments.isEmpty()) {
            switch (paymentType) {
                case DEPOSIT: {
                    payments.addAll(
                            paymentRepo.findByBookingIdAndTypeAndStatus(
                                    booking.getId(), Payment.Type.DEPOSIT, Payment.Status.PENDING
                            )
                    );
                    break;
                }
                case RENT: {
                    List<Payment> depositPending = paymentRepo.findByBookingIdAndTypeAndStatus(
                            booking.getId(), Payment.Type.DEPOSIT, Payment.Status.PENDING
                    );
                    if (!depositPending.isEmpty()) payments.addAll(depositPending);

                    List<Payment> rentPending = paymentRepo.findByBookingIdAndTypeAndStatus(
                            booking.getId(), Payment.Type.RENT, Payment.Status.PENDING
                    );
                    payments.addAll(rentPending);
                    break;
                }
                default: {
                    payments.addAll(
                            paymentRepo.findByBookingIdAndStatus(
                                    booking.getId(), Payment.Status.PENDING
                            )
                    );
                    break;
                }
            }
        }

        if (payments.isEmpty()) {
            throw new InvalidTransactionException("No Pending Payment / Booking Expired for " + bookingId);
        }

        long totalAmount = payments.stream()
                .mapToLong(p -> p.getAmount().longValue())
                .sum();

        String referenceId = null;
        for (Payment p : payments) {
            Optional<PaymentTransaction> existingTx = paymentTransactionRepo
                    .findAllByRefStatusAndPaymentId(referenceId, "WAITING_FOR_PAYMENT", p.getId());
            if (existingTx.isPresent()) {
                referenceId = existingTx.get().getReferenceId();
                break;
            }
        }
        if (referenceId == null) {
            referenceId = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        }

        for (Payment p : payments) {
            Optional<PaymentTransaction> existingTx = paymentTransactionRepo
                    .findAllByRefStatusAndPaymentId(referenceId, "WAITING_FOR_PAYMENT", p.getId());

            if (existingTx.isEmpty()) {
                PaymentTransaction tx = new PaymentTransaction();
                tx.setReferenceId(referenceId);
                tx.setAmount(p.getAmount().longValue());
                tx.setBuilding(p.getBuilding());
                tx.setPayment(p);
                tx.setStatus("WAITING_FOR_PAYMENT");
                tx.setCurrency("IDR");
                tx.setOwner(p.getOwner());
                tx.setType(p.getType().name());
                tx.setChannelCode(p.getChannelCode());
                tx.setProvider("XENDIT");
                tx.setTotalAmount(totalAmount);
                paymentTransactionRepo.save(tx);
            }

            if (p.getStatus() == Payment.Status.PENDING) {
                p.setStatus(Payment.Status.WAITING_FOR_PAYMENT);
                paymentRepo.save(p);
            }
        }
        List<InquiryPaymentInfo> paymentInfoList = payments.stream()
                .map(p -> new InquiryPaymentInfo(
                        p.getId(),
                        p.getType().name(),
                        p.getAmount().longValue(),
                        p.getStatus().name(),
                        p.getChannelCode(),
                        p.getVaNumber(),
                        p.getQrisQrString(),
                        p.getExpiresAt()
                ))
                .collect(Collectors.toList());

        return new InquiryPaymentResponse(
                bookingId,
                paymentType.name(),
                referenceId,
                totalAmount,
                "IDR",
                paymentInfoList
        );
    }


    /* =========================================================
     1) ONE-OFF VA  (type = PAY)  — closed amount per invoice
     ========================================================= */

    @Transactional
    public PaymentStartResult startPayment(UUID bookingId,
                                           long amount,
                                           String bankChannelCode,
                                           String displayName,
                                           Long expectedAmountNullable
    ) throws InvalidTransactionException {
        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        Building bldg = booking.getRoom().getBuilding();
        Owner owner = bldg.getOwner();

        // Check if the booking already has an associated VA number
        List<Payment> existingPayment = paymentRepo.findByBookingAndStatusAndChannelCode(booking, Payment.Status.WAITING_FOR_PAYMENT, bankChannelCode);

        if (!existingPayment.isEmpty()) {
            throw new InvalidTransactionException("VA already generated for this booking: " + existingPayment.getFirst().getVaNumber() + " expiry :" + existingPayment.getFirst().getExpiresAt());
        }

        // Create local Payment row first (PENDING)
        Payment p = new Payment();
        p.setBooking(booking);
        p.setOwner(owner);
        p.setBuilding(bldg);
        p.setType(Payment.Type.RENT);
        p.setStatus(Payment.Status.PENDING);
        p.setAmount(booking.getMonthlyPrice());
        p.setCurrency("IDR");
        p.setFlow(Payment.GatewayFlow.PAY);
        p.setChannelCode(bankChannelCode);
        // referenceId = bookingId (string) is a good default
        p.setReferenceId(bookingId.toString());
        p = paymentRepo.save(p);

        XenditPaymentRequestDTO xenditPaymentRequest = new XenditPaymentRequestDTO();
        ChannelProperties channelProperties = new ChannelProperties();
        channelProperties.setDisplayName(displayName);
        channelProperties.setExpectedAmount(String.valueOf(expectedAmountNullable));

        xenditPaymentRequest.setRequestAmount(amount);
        xenditPaymentRequest.setCountry("ID");
        xenditPaymentRequest.setCurrency("IDR");
        xenditPaymentRequest.setReferenceId(bookingId.toString());
        xenditPaymentRequest.setChannelCode(bankChannelCode);
        xenditPaymentRequest.setChannelProperties(channelProperties);

        // Call Xendit
        PaymentResponseDTO resp = xendit.createPayVa( xenditPaymentRequest);
        log.info("Update payment with gateway data: " + resp.getActions().getFirst().getValue());
        // Parse response
        String prId = resp.getPaymentRequestId();
//        String channel = resp.getChannelCode();
//        String vaNumber = resp.getVaNumber();
        LocalDateTime expiresAt = resp.getChannelProperties().getExpiresAt();

        p.setStatus(Payment.Status.WAITING_FOR_PAYMENT);
        p.setPrId(resp.getPaymentRequestId());
        p.setChannelCode(resp.getChannelCode());
        if (bankChannelCode.equals("QRIS")){
            p.setQrisQrString(resp.getActions().getFirst().getValue());
        }else {
            p.setVaNumber(resp.getActions().getFirst().getValue());
        }
        p.setQrisQrString(resp.getQrisQrString());
        p.setExpiresAt(expiresAt);
        p.setActionsJson(resp.getActions().toString());
        p.setChannelPropertiesJson( resp.getChannelProperties().toString());
        paymentRepo.save(p);

        if (attemptRepo != null) {
            PaymentAttempt at = new PaymentAttempt();
            at.setBookingId(bookingId.toString());
            at.setCustomerId(booking.getTenant().getId().toString());
            at.setOwner(owner);
            at.setBuilding(bldg);
            at.setChannelCode(resp.getChannelCode());
            at.setType(PaymentAttempt.Type.valueOf("PAY"));
            at.setStatus(PaymentAttempt.Status.valueOf("PENDING"));
            at.setRequestAmount(resp.getRequestAmount());
            at.setCurrency("IDR");
            at.setPrId(resp.getPaymentRequestId());
            at.setIdemKey(UUID.randomUUID());
//            at.setActions(safeJsonMap(resp.get("actions")));
            attemptRepo.save(at);
        }

        return new PaymentStartResult(
                p.getId(), resp.getPaymentRequestId(), resp.getChannelCode(), "IDR", resp.getRequestAmount(),
                p.getVaNumber(), p.getQrisQrString(), expiresAt, resp
        );
    }


     /* =========================================================
     3) REUSABLE codes (VA / static QR) — upsert per customer
     ========================================================= */

    @Transactional
    public ReusableCodeResult provisionReusableVa(UUID ownerId,
                                                        UUID buildingIdOrNull,  // null = shared across owner
                                                        UUID customerId,
                                                        String displayName,
                                                        String bankChannelCode) {
        // Try existing
        Optional<CustomerPaymentCode> existing =
                codeRepo.findActiveByOwnerAndBuildingAndCustomerAndChannel(ownerId, buildingIdOrNull, String.valueOf(customerId), bankChannelCode);
        if (existing.isPresent()) {
            CustomerPaymentCode c = existing.get();
            return new ReusableCodeResult(c.getPaymentRequestId(), null,bankChannelCode ,
                    c.getKind().name() , c.getCodeValue() , c.getExpiresAt(), null);
        }

        // Create at Xendit
        ReusableCodeResult x = xendit.createReusableVa(customerId, displayName, bankChannelCode);

        // Upsert local
        CustomerPaymentCode c = new CustomerPaymentCode();
        c.setOwner(new Owner() {{ setId(ownerId); }});         // minimal reference; or fetch Owner
        if (buildingIdOrNull != null) c.setBuilding(new Building() {{ setId(buildingIdOrNull); }});
        c.setCustomerId(String.valueOf(customerId));
        c.setChannelCode(bankChannelCode);
        c.setKind(CustomerPaymentCode.Kind.VIRTUAL_ACCOUNT);
        c.setCodeValue(x.codeValue());
        c.setPaymentRequestId(x.paymentRequestId());
        c.setStatus(CustomerPaymentCode.Status.ACTIVE);
        c.setExpiresAt(toLocalUtc(x.expiresAt() != null ? x.expiresAt().toString() : null));
        c.setActions(safeJsonMap(x.raw()));                    // if your entity uses JSONB(Map); else set text
        codeRepo.save(c);

        return new ReusableCodeResult(c.getPaymentRequestId(), null,bankChannelCode ,
                c.getKind().name() , c.getCodeValue() , c.getExpiresAt(), null);
    }

    @Transactional
    public ReusableCodeResult provisionReusableQris(UUID ownerId,
                                                          UUID buildingIdOrNull,
                                                          UUID customerId) {
        Optional<CustomerPaymentCode> existing =
                codeRepo.findActiveByOwnerAndBuildingAndCustomerAndChannel(ownerId, buildingIdOrNull, String.valueOf(customerId), "QRIS");
        if (existing.isPresent()) {
            var c = existing.get();
            return new ReusableCodeResult(c.getPaymentRequestId(), null, c.getChannelCode(),
                    c.getKind().toString(), c.getCodeValue(), c.getExpiresAt(), null);
        }

        ReusableCodeResult x = xendit.createReusableQris(String.valueOf(customerId));

        CustomerPaymentCode c = new CustomerPaymentCode();
        c.setOwner(new Owner() {{ setId(ownerId); }});
        if (buildingIdOrNull != null) c.setBuilding(new Building() {{ setId(buildingIdOrNull); }});
        c.setCustomerId(String.valueOf(customerId));
        c.setChannelCode("QRIS");
        c.setKind(CustomerPaymentCode.Kind.QR);
        c.setCodeValue(x.codeValue());
        c.setPaymentRequestId(x.paymentRequestId());
        c.setStatus(CustomerPaymentCode.Status.ACTIVE);
        c.setExpiresAt(toLocalUtc(x.expiresAt() != null ? x.expiresAt().toString() : null));
        c.setActions(safeJsonMap(x.raw()));
        codeRepo.save(c);

        return new ReusableCodeResult(c.getPaymentRequestId(), x.referenceId(), c.getChannelCode(), c.getKind().name().toString(),c.getCodeValue(),c.getExpiresAt(), null);
    }

    @Transactional
    public BookingPaymentResponse pay(UUID bookingId, BookingPaymentRequest req) throws InvalidTransactionException, InvalidInputException {
        Booking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new InvalidTransactionException("Booking not found: " + bookingId));

        if (b.getStatus() == Booking.Status.CANCELLED) {
            throw new InvalidTransactionException("Booking already cancelled");
        }

        final String scope = (req.scope() == null ? "DEPOSIT" : req.scope().trim().toUpperCase(Locale.ROOT));
        if (!scope.equals("DEPOSIT") && !scope.equals("FULL") && !scope.equals("RENT")) {
            throw new InvalidInputException("scope must be DEPOSIT, RENT or FULL");
        }
        if (req.method() == null || req.method().isBlank()) {
            throw new InvalidInputException("method is required");
        }

        final LocalDateTime now     = LocalDateTime.now(clock.zone());
        final LocalDateTime paidAt  = (req.paidAt() != null ? req.paidAt() : now);
        final String method         = req.method().trim().toUpperCase(Locale.ROOT);
        final String reference      = (req.reference() == null ? null : req.reference().trim());

        // Update pending DEPOSIT
        int depUpdated = 0;
        if (!scope.equals("RENT")) {
            depUpdated = paymentRepo.markDepositPaidByBooking(
                    bookingId, method, reference, paidAt, now
            );
        }


        boolean depositAlreadyPaidOrVerified =
                depUpdated > 0
                        || paymentRepo.existsByBookingIdAndTypeAndStatus(bookingId, Payment.Type.DEPOSIT, Payment.Status.PAID)
                        || paymentRepo.existsByBookingIdAndTypeAndStatus(bookingId, Payment.Type.DEPOSIT, Payment.Status.VERIFIED);

        if (depositAlreadyPaidOrVerified && b.getStatus() != Booking.Status.ACTIVE) {
            b.setStatus(Booking.Status.ACTIVE);
            b.setUpdatedAt(now);
            bookingRepo.saveAndFlush(b);
        }

        int rentUpdated = 0;
        if (scope.equals("FULL") || scope.equals("RENT")) {
            rentUpdated = paymentRepo.markAllPendingRentPaidByBooking(
                    bookingId, method, reference, paidAt, now
            );
        }

        // Ambil ID payment DEPOSIT untuk response ---
        List<Payment> paidOrVerified = paymentRepo
                .findByBookingIdAndTypeInAndStatusInOrderByPaidAtDesc(
                        bookingId,
                        List.of(Payment.Type.DEPOSIT, Payment.Type.RENT),
                        List.of(Payment.Status.PAID)
                );
        Optional<Payment> depositPaymentOpt = paidOrVerified.stream()
                .filter(p -> p.getType() == Payment.Type.DEPOSIT)
                .findFirst();

        Optional<Payment> rentPaymentOpt = paidOrVerified.stream()
                .filter(p -> p.getType() == Payment.Type.RENT)
                .findFirst();

        UUID depositId = depositPaymentOpt.map(Payment::getId).orElse(null);
        UUID rentId    = rentPaymentOpt.map(Payment::getId).orElse(null);

        return new BookingPaymentResponse(
                scope,
                depUpdated,
                rentUpdated,
                b.getStatus().name(),
                depositId,
                rentId
        );
    }


    /* ======================== helpers ======================== */

    private String extractVaNumber(Map<String,Object> resp) {
        Object actionsObj = resp.get("actions");
        if (actionsObj instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?,?> m) {
                    if ("VIRTUAL_ACCOUNT_NUMBER".equals(String.valueOf(m.get("descriptor")))) {
                        Object v = m.get("value");
                        if (v == null) v = m.get("account_number");
                        return v == null ? null : String.valueOf(v);
                    }
                }
            }
        }
        return null;
    }

    private String extractQrString(Map<String,Object> resp) {
        Object actionsObj = resp.get("actions");
        if (actionsObj instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?,?> m) {
                    if ("QR_CODE".equals(String.valueOf(m.get("descriptor")))) {
                        Object v = (m.get("qr_string") != null) ? m.get("qr_string") : m.get("value");
                        return v == null ? null : String.valueOf(v);
                    }
                }
            }
        }
        return null;
    }

    private LocalDateTime toLocalUtc(String iso) {
        if (iso == null) return null;
        try {
            return OffsetDateTime.parse(iso).toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(iso);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private String safeJson(Object node) {
        try { return (node == null) ? null : M.writeValueAsString(node); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private JsonNode safeJsonMap(Object node) {
        // if your entity column is JSONB(Map<String,Object>) — otherwise not needed
        if (node == null) return (JsonNode) Map.of();
        if (node instanceof Map<?,?> m) return (JsonNode) m;
        try {
            return (JsonNode) M.readValue(M.writeValueAsBytes(node), Map.class);
        } catch (Exception e) {
            return (JsonNode) Map.of();
        }
    }

    private static String asString(Object o) { return o == null ? null : String.valueOf(o); }
}

