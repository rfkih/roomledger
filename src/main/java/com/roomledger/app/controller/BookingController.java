package com.roomledger.app.controller;


import com.roomledger.app.dto.*;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Commons.Enum.BookingStatus;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.Commons.Enum.PaymentStatus;
import com.roomledger.app.model.Commons.Enum.PaymentType;
import com.roomledger.app.model.Room;
import com.roomledger.app.repository.BookingRepository;
import com.roomledger.app.repository.PaymentRepository;
import com.roomledger.app.service.BillingService;
import com.roomledger.app.service.BookingService;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    @Value("${application.code}")
    private String applicationCode;

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final BookingService bookingService;
    private final BillingService billingService ;

    public BookingController(BookingRepository bookings,  PaymentRepository payments, BookingService bookingService, BillingService billingService) {
        this.bookings = bookings;
        this.payments = payments;
        this.bookingService = bookingService;
        this.billingService = billingService;
    }

    /**
     * Activate booking (and mark room OCCUPIED if AVAILABLE)
     * after the DEPOSIT payment (depositPaymentId) is already VERIFIED.
     *
     * POST /api/payments/{depositPaymentId}/activate-booking
     */
    @PostMapping("/payments/{paymentId}/activate-booking")
    public ResponseService activateOnVerifiedDeposit(
            @PathVariable UUID paymentId
    ) throws InvalidTransactionException {

        Payment p = payments.findByIdWithBookingAndRoom(paymentId).orElseThrow();

        if (p.getType() == PaymentType.DEPOSIT) {
            bookingService.activateOnDepositVerified(paymentId);
        } else if (p.getType() == PaymentType.RENT) {
            bookingService.activateOnRentVerified(paymentId);
        }

        // Reload untuk status final
        Payment reloaded = payments.findByIdWithBookingAndRoom(paymentId).orElseThrow();
        Booking b = reloaded.getBooking();
        Room r = (b != null ? b.getRoom() : null);

        ActivationResponse body = new ActivationResponse(
                b != null ? b.getId() : null,
                r != null ? r.getId() : null,
                b != null ? b.getStatus().name() : null,
                r != null ? reloaded.getStatus().name() : null
        );

        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                body
                ).getBody();
    }


    /** Create a booking -> creates a PENDING DEPOSIT payment and blocks the room. */
    @PostMapping("/draft")
    public ResponseService createDraft(@RequestBody CreateBookingRequest req) throws InvalidTransactionException {
        DraftBookingResult res = bookingService.createDraftBookingWithBills(req);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                res
                ).getBody();
    }

    /** Mark a payment as PAID (user action) */
    @PostMapping("/payments/{paymentId}/mark-paid")
    @Transactional
    public ResponseService markPaid(@PathVariable UUID paymentId,
                                      @RequestParam(required = false) String method,
                                      @RequestParam(required = false) String reference) {
        var p = payments.findById(paymentId).orElseThrow();
        p.setStatus(PaymentStatus.PAID);
//        p.setMethod(method);
//        p.setReference(reference);
        p.setPaidAt(LocalDateTime.now());
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                Map.of("paymentId", p.getId(), "status", p.getStatus().name())
                ).getBody();
    }

    /** Admin verifies a payment (moves PAID -> VERIFIED) */
    @PostMapping("/payments/{paymentId}/verify")
    @Transactional
    public ResponseService verify(@PathVariable UUID paymentId) {
        var p = payments.findById(paymentId).orElseThrow();
        p.setStatus(PaymentStatus.VERIFIED);
        payments.save(p);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                Map.of("paymentId", p.getId(), "status", p.getStatus().name())
                ).getBody();
    }

    @GetMapping("/active")
    public ResponseService activeByPhone(@RequestParam String phone) {
        List<ActiveBookingDto> list = bookingService.activeWithBillsByPhone(phone);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                list
                ).getBody();
    }

    @PostMapping("/{bookingId}/renewals/decision")
    @Transactional
    public ResponseService renewalDecision(
            @PathVariable UUID bookingId,
            @RequestParam String period,
            @RequestParam boolean willContinue
    ) throws InvalidTransactionException {
        Booking b = bookings.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        final YearMonth ym;
        try {
            ym = YearMonth.parse(period);
        } catch (DateTimeParseException ex) {
            throw new InvalidTransactionException("Invalid period format. Use YYYY-MM");
        }
        var day1 = ym.atDay(1);

        var maybeRent = payments.findByBookingIdAndTypeAndPeriodMonth(
                b.getId(), PaymentType.RENT, day1
        );

        if (!willContinue) {
            // If already paid/verified for that month, don’t allow decline
            if (maybeRent.isPresent() &&
                    (maybeRent.get().getStatus() == PaymentStatus.PAID ||
                            maybeRent.get().getStatus() == PaymentStatus.VERIFIED)) {
                return ResponseUtil.setResponse(
                        HttpStatus.OK.value(),
                        applicationCode,
                        ResponseCode.SUCCESS.getCode(),
                        ResponseCode.SUCCESS.getDescription(),
                        Map.of(
                                "error", "Rent for this period is already paid/verified",
                                "paymentId", maybeRent.get().getId(),
                                "status", maybeRent.get().getStatus().name()
                        )
                        ).getBody();
            }

            // Delete pending bill if present
            maybeRent.ifPresent(payments::delete);

            // End booking at end of previous month and stop auto-renew
            var endOfPrev = ym.minusMonths(1).atEndOfMonth();
            b.setEndDate(endOfPrev);
            b.setStatus(BookingStatus.ENDED);
            b.setAutoRenew(false);
            bookings.save(b);

            return  ResponseUtil.setResponse(
                    HttpStatus.OK.value(),
                    applicationCode,
                    ResponseCode.SUCCESS.getCode(),
                    ResponseCode.SUCCESS.getDescription(),
                    Map.of(
                            "bookingId", b.getId(),
                            "period", ym.toString(),
                            "continue", false,
                            "bookingStatus", b.getStatus().name(),
                            "endDate", b.getEndDate().toString()
                    )
                    ).getBody();
        }

        if (maybeRent.isEmpty()) {
            BigDecimal amount = billingService.quoteSingleMonth(
                    b.getMonthlyPrice(), b.getStartDate(), b.getEndDate(), ym
            );
            if (amount.signum() <= 0) {
                throw new InvalidTransactionException("No billable amount for period " + ym);
            }

            var rent = new Payment();
            rent.setBooking(b);
            rent.setType(PaymentType.RENT);
            rent.setStatus(PaymentStatus.PENDING);
            rent.setAmount(amount);
            rent.setPeriodMonth(day1);
            payments.save(rent);

            b.setEndDate(b.getEndDate().plusMonths(1));
            bookings.save(b);

            return ResponseUtil.setResponse(
                    HttpStatus.OK.value(),
                    applicationCode,
                    ResponseCode.SUCCESS.getCode(),
                    ResponseCode.SUCCESS.getDescription(),
                    Map.of(
                            "bookingId", b.getId(),
                            "period", ym.toString(),
                            "continue", true,
                            "paymentId", rent.getId(),
                            "amount", rent.getAmount()
                    )
                ).getBody();
        } else {
            var p = maybeRent.get();
            return ResponseUtil.setResponse(
                    HttpStatus.OK.value(),
                    applicationCode,
                    ResponseCode.SUCCESS.getCode(),
                    ResponseCode.SUCCESS.getDescription(),
                    Map.of(
                            "bookingId", b.getId(),
                            "period", ym.toString(),
                            "continue", true,
                            "paymentId", p.getId(),
                            "status", p.getStatus().name(),
                            "amount", p.getAmount()
                    )
            ).getBody();
        }
    }
}