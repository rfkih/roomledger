package com.roomledger.app.job;

import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.Room;
import com.roomledger.app.repository.BookingRepository;
import com.roomledger.app.repository.PaymentRepository;
import com.roomledger.app.repository.RoomRepository;
import com.roomledger.app.service.ClockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoomOccupancyJob {

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final PaymentRepository payments;
    private final ClockService clock;

    /**
     * Runs every 10 minutes (configurable).
     */
    @Transactional
    @Scheduled(cron = "0 5 2 * * *", zone = "Asia/Jakarta")
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Jakarta")
    public void roomVerification() {
        final LocalDate today = clock.today();
        final LocalDateTime now = LocalDateTime.now(clock.zone());
        log.info("[RoomOccupancyJob] Running at {}", now);

        // OCCUPY room when rent is VERIFIED
        List<Booking> toOccupy = bookings
                .findActiveStartingTodayWithVerifiedRentAndRoomNotOccupied(
                        today,
                        Booking.Status.ACTIVE,
                        Room.Status.OCCUPIED,
                        Payment.Type.RENT,
                        Payment.Status.VERIFIED
                );

        int occupied = 0;
        for (Booking b : toOccupy) {
            Room r = b.getRoom();
            if (r != null && r.getStatus() != Room.Status.OCCUPIED) {
                r.setStatus(Room.Status.OCCUPIED);
                rooms.save(r);
                occupied++;
                log.info("[RoomOccupancyJob] OCCUPIED room {} for booking {} (startDate={}).", r.getId(), b.getId(), today);
            }
        }

        //  RELEASE room when start is today and rent still PENDING (no VERIFIED),
        //         and cancel all PENDING rent payments for the booking
        List<Booking> toRelease = bookings
                .findActiveStartingTodayWithPendingRentOnlyAndRoomNotAvailable(
                        today,
                        Booking.Status.ACTIVE,
                        Room.Status.AVAILABLE,
                        Payment.Type.RENT,
                        Payment.Status.PENDING,
                        Payment.Status.VERIFIED
                );

        int released = 0;
        int cancelledPayments = 0;
        for (Booking b : toRelease) {
            Room r = b.getRoom();
            if (r != null && r.getStatus() != Room.Status.AVAILABLE) {
                r.setStatus(Room.Status.AVAILABLE);
                rooms.save(r);
                released++;
                log.warn("[RoomOccupancyJob] AVAILABLE room {} for booking {} (rent still PENDING at startDate={}).",
                        r.getId(), b.getId(), today);
            }

            int cancelled = payments.cancelPendingRentByBooking(
                    b.getId(), Payment.Type.RENT, Payment.Status.PENDING, Payment.Status.CANCELLED, now
            );
            cancelledPayments += cancelled;

            if (cancelled > 0) {
                log.warn("[RoomOccupancyJob] CANCELLED {} pending RENT payment(s) for booking {}.", cancelled, b.getId());
            }
        }

        log.info("[RoomOccupancyJob] Done for {}. occupied={}, released={}, paymentsCancelled={}",
                today, occupied, released, cancelledPayments);
    }

    /**
     * Nightly pass: if today > endDate, set room to AVAILABLE.
     * Default 00:15 WIB; override via application properties:
     *   job.room-release-after-end.cron=0 15 0 * * *
     */
    @Transactional
    @Scheduled(
            cron = "${job.room-release-after-end.cron:0 * * * * *}",
            zone = "Asia/Jakarta"
    )
    public void releaseRoomsPastEndDate() {
        final LocalDate today = clock.today();

        List<Booking> ended = bookings.findActiveEndedWithRoomNotAvailable(
                today,
                Booking.Status.ACTIVE,
                Room.Status.AVAILABLE
        );

        if (ended.isEmpty()) {
            log.debug("[RoomOccupancyJob] No ended bookings to release for {}.", today);
            return;
        }

        int changed = 0;
        for (Booking b : ended) {
            Room r = b.getRoom();
            if (r != null && r.getStatus() != Room.Status.AVAILABLE) {
                r.setStatus(Room.Status.AVAILABLE);
                rooms.save(r);
                changed++;
                log.info("[RoomOccupancyJob] Room {} set to AVAILABLE after booking {} ended (endDate={}, today={}).",
                        r.getId(), b.getId(), b.getEndDate(), today);
            }
        }

        log.info("[RoomOccupancyJob] Released {} room(s) past end date for {}.", changed, today);
    }
}

