package com.roomledger.app.job;

import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.*;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.Commons.Enum.PaymentStatus;
import com.roomledger.app.model.Commons.Enum.PaymentType;
import com.roomledger.app.repository.*;
import com.roomledger.app.service.ClockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class MonthlyBillingJob {
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final ClockService clockService;

    public MonthlyBillingJob(BookingRepository bookings, PaymentRepository payments, ClockService clockService) {
        this.bookings = bookings; this.payments = payments;
        this.clockService = clockService;
    }

    @Scheduled(cron = "0 5 2 * * *", zone = "Asia/Jakarta")
    @Transactional
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Jakarta")
    public void ensureNextMonthBillsAndRenewals() throws InvalidTransactionException {
        LocalDate today = clockService.today();
        log.info("Monthly Billing Job running at {}", today);

        YearMonth thisMonth = YearMonth.from(today);
        YearMonth nextMonth = thisMonth.plusMonths(1);

        LocalDate nextMonthStart = nextMonth.atDay(1);

        List<Booking> list = bookings.findAllActiveAutoRenew();
        for (Booking b : list) {

            // H-1 notice based on booking endDate
            if (b.getEndDate() != null) {
                LocalDate noticeDate = b.getEndDate().minusDays(1);
                if (today.equals(noticeDate)) {
                    log.info("[MonthlyBillingJob] Sending notice for booking {}.", b.getId());
                    // send reminder...
                }
            }


            Optional<Payment> existingForPeriod =
                    payments.findByBookingIdAndTypeAndPeriodMonth(
                            b.getId(), PaymentType.RENT, nextMonthStart);

            if (existingForPeriod.isEmpty()) {
                log.info("Creates new Billing for next month for booking {} start date {}, end date {}, next month {}."
                        , b.getId(), b.getStartDate(), b.getEndDate(), nextMonth);

                Payment p = new Payment();
                p.setBooking(b);
                p.setType(PaymentType.RENT);
                p.setStatus(PaymentStatus.PENDING);
                p.setAmount(b.getMonthlyPrice());
                p.setPeriodMonth(nextMonthStart);
                payments.save(p);

            }
        }
    }
}

