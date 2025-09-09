package com.roomledger.app.job;

import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Payment;
import com.roomledger.app.repository.BookingRepository;
import com.roomledger.app.repository.PaymentRepository;
import com.roomledger.app.service.ClockService;
import com.roomledger.app.service.ParamService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DepositExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(DepositExpiryJob.class);

    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final ParamService params;
    private final ClockService clock; // for zone

    public DepositExpiryJob(PaymentRepository payments,
                            BookingRepository bookings,
                            ParamService params,
                            ClockService clock) {
        this.payments = payments;
        this.bookings = bookings;
        this.params = params;
        this.clock = clock;
    }



    @Scheduled(cron = "0 * * * * *", zone = "Asia/Jakarta")//run for every 1 minute
    @Transactional
    public void cancelDraftsWithoutDeposit() {
        final int ttl = params.getInt("DEPOSIT_TTL_MINUTES", 1);
        log.info("Checking for deposits older than {} minutes", ttl);

        LocalDateTime cutoffLocal = LocalDateTime.now().minusMinutes(ttl);

        List<Payment> overdueDeposits = payments.findByTypeAndStatusAndCreatedAtBefore(
                Payment.Type.DEPOSIT, Payment.Status.PENDING, cutoffLocal);

        int cancelledCount = 0;

        for (Payment dep : overdueDeposits) {
            Booking b = dep.getBooking();
            if (b == null) continue;

            if (b.getStatus() != Booking.Status.DRAFT) continue;

            log.info("Booking {} has overdue deposit {}", b.getId(), dep.getId());

            b.setStatus(Booking.Status.CANCELLED);
            b.setAutoRenew(false);
            b.setUpdatedAt(clock.now());
            bookings.save(b);
            payments.cancelPendingByBooking(b.getId());
            cancelledCount++;
        }
        log.info("Cancelled {} overdue deposits/booking(s).", cancelledCount);
    }

    public void cancelDraftsWithoutDeposits() {
        final int ttl = params.getInt("DEPOSIT_TTL_MINUTES", 60);
        log.info("Checking for deposits older than {} minutes", ttl);

        var cutoff = java.time.LocalDateTime.now().minusMinutes(ttl);

        // Find overdue deposits
        var overdueDeposits = payments.findByTypeAndStatusAndCreatedAtBefore(
                Payment.Type.DEPOSIT, Payment.Status.PENDING, cutoff);

        // Extract booking IDs
        var bookingIdsToCancel = overdueDeposits.stream()
                .map(Payment::getBooking)
                .filter(java.util.Objects::nonNull)
                .map(Booking::getId)
                .collect(java.util.stream.Collectors.toSet());  // <— collector

        if (bookingIdsToCancel.isEmpty()) {
            log.info("Cancelled 0 overdue deposits/booking(s).");
            return;
        }

        List<Booking> drafts = bookings.findByIdInAndStatus(bookingIdsToCancel, Booking.Status.DRAFT);

        // Update bookings + cancel their pending payments
        drafts.forEach(b -> {
            b.setStatus(Booking.Status.CANCELLED);
            b.setAutoRenew(false);
            b.setUpdatedAt(LocalDateTime.now());
        });
        bookings.saveAll(drafts);

        int cancelledPayments = drafts.stream()
                .map(Booking::getId)
                .map(payments::cancelPendingByBooking)
                .mapToInt(Integer::intValue)
                .sum();

        log.info("Cancelled {} booking(s) and {} pending payment(s).",
                drafts.size(), cancelledPayments);
    }

}
