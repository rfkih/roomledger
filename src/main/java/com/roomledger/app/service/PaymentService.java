package com.roomledger.app.service;

import com.roomledger.app.dto.BookingPaymentRequest;
import com.roomledger.app.dto.BookingPaymentResponse;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.Room;
import com.roomledger.app.repository.BookingRepository;
import com.roomledger.app.repository.PaymentRepository;
import com.roomledger.app.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final RoomRepository rooms;
    private final ClockService clock;

    public PaymentService(BookingRepository bookings,
                                 PaymentRepository payments,
                                 RoomRepository rooms,
                                 ClockService clock) {
        this.bookings = bookings;
        this.payments = payments;
        this.rooms = rooms;
        this.clock = clock;
    }

    @Transactional
    public BookingPaymentResponse pay(UUID bookingId, BookingPaymentRequest req) throws InvalidTransactionException {
        Booking b = bookings.findById(bookingId)
                .orElseThrow(() -> new InvalidTransactionException("Booking not found: " + bookingId));

        if (b.getStatus() == Booking.Status.CANCELLED) {
            throw new InvalidTransactionException("Booking already cancelled");
        }

        final String scope = (req.scope() == null ? "DEPOSIT" : req.scope().trim().toUpperCase(Locale.ROOT));
        if (!scope.equals("DEPOSIT") && !scope.equals("FULL")) {
            throw new InvalidTransactionException("scope must be DEPOSIT or FULL");
        }
        if (req.method() == null || req.method().isBlank()) {
            throw new InvalidTransactionException("method is required");
        }

        final LocalDateTime now     = LocalDateTime.now(clock.zone());
        final LocalDateTime paidAt  = (req.paidAt() != null ? req.paidAt() : now);
        final String method         = req.method().trim().toUpperCase(Locale.ROOT);
        final String reference      = (req.reference() == null ? null : req.reference().trim());

        // --- 1) Pastikan DEPOSIT paid/verified (idempotent) ---
        // Coba update dari PENDING -> PAID
        int depUpdated = payments.markDepositPaidByBooking(
                bookingId, method, reference, paidAt, now
        ); // <- harus return int

        // Jika tidak ada baris yang berubah, cek apakah memang sudah PAID/VERIFIED (idempotent)
        boolean depositAlreadyPaidOrVerified =
                depUpdated > 0
                        || payments.existsByBookingIdAndTypeAndStatus(bookingId, Payment.Type.DEPOSIT, Payment.Status.PAID)
                        || payments.existsByBookingIdAndTypeAndStatus(bookingId, Payment.Type.DEPOSIT, Payment.Status.VERIFIED);

        // --- 2) Jika deposit sudah paid/verified, aktifkan booking (idempotent) ---
        if (depositAlreadyPaidOrVerified && b.getStatus() != Booking.Status.ACTIVE) {
            b.setStatus(Booking.Status.ACTIVE);
            b.setUpdatedAt(now);
            bookings.saveAndFlush(b);
        }

        // --- 3) Jika FULL, lunasi semua RENT yang masih pending ---
        int rentUpdated = 0;
        if (scope.equals("FULL")) {
            rentUpdated = payments.markAllPendingRentPaidByBooking(
                    bookingId, method, reference, paidAt, now
            );
        }

        // --- 4) Ambil ID payment DEPOSIT untuk response (opsional) ---
        List<Payment> paidOrVerified = payments
                .findByBookingIdAndTypeInAndStatusInOrderByPaidAtDesc(
                        bookingId,
                        List.of(Payment.Type.DEPOSIT, Payment.Type.RENT),
                        List.of(Payment.Status.PAID)
                );

        // kalau mau list khusus menurut type:
        Optional<Payment> depositPaymentOpt = paidOrVerified.stream()
                .filter(p -> p.getType() == Payment.Type.DEPOSIT)
                .findFirst();

        Optional<Payment> rentPaymentOpt = paidOrVerified.stream()
                .filter(p -> p.getType() == Payment.Type.RENT)
                .findFirst();

        return new BookingPaymentResponse(
                scope,
                depUpdated,
                rentUpdated,
                b.getStatus().name(),
                depositPaymentOpt.get().getId(),
                rentPaymentOpt.get().getId()
        );
    }

}