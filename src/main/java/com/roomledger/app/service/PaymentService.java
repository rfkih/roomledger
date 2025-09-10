package com.roomledger.app.service;

import com.roomledger.app.dto.BookingPaymentRequest;
import com.roomledger.app.dto.BookingPaymentResponse;
import com.roomledger.app.exthandler.InvalidInputException;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Payment;
import com.roomledger.app.repository.BookingRepository;
import com.roomledger.app.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;

    private final ClockService clock;

    public PaymentService(BookingRepository bookings,
                                 PaymentRepository payments,
                                 ClockService clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.clock = clock;
    }

    @Transactional
    public BookingPaymentResponse pay(UUID bookingId, BookingPaymentRequest req) throws InvalidTransactionException, InvalidInputException {
        Booking b = bookings.findById(bookingId)
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
            depUpdated = payments.markDepositPaidByBooking(
                    bookingId, method, reference, paidAt, now
            );
        }


        boolean depositAlreadyPaidOrVerified =
                depUpdated > 0
                        || payments.existsByBookingIdAndTypeAndStatus(bookingId, Payment.Type.DEPOSIT, Payment.Status.PAID)
                        || payments.existsByBookingIdAndTypeAndStatus(bookingId, Payment.Type.DEPOSIT, Payment.Status.VERIFIED);

        if (depositAlreadyPaidOrVerified && b.getStatus() != Booking.Status.ACTIVE) {
            b.setStatus(Booking.Status.ACTIVE);
            b.setUpdatedAt(now);
            bookings.saveAndFlush(b);
        }

        int rentUpdated = 0;
        if (scope.equals("FULL") || scope.equals("RENT")) {
            rentUpdated = payments.markAllPendingRentPaidByBooking(
                    bookingId, method, reference, paidAt, now
            );
        }

        // Ambil ID payment DEPOSIT untuk response ---
        List<Payment> paidOrVerified = payments
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

}