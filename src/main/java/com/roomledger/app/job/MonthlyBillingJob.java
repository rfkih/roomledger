package com.roomledger.app.job;

import com.roomledger.app.model.*;
import com.roomledger.app.repository.*;
import com.roomledger.app.service.BillingService;
import com.roomledger.app.service.ClockService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Optional;

@Component
public class MonthlyBillingJob {
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final BillingService billing;
    private final ClockService clockService;

    public MonthlyBillingJob(BookingRepository bookings, PaymentRepository payments, BillingService billing, ClockService clockService) {
        this.bookings = bookings; this.payments = payments;
        this.billing = billing;
        this.clockService = clockService;
    }

    @Scheduled(cron = "0 5 2 * * *", zone = "Asia/Jakarta")
//    @Scheduled(cron = "0 * * * * *", zone = "Asia/Jakarta")
    @Transactional
    public void ensureNextMonthBillsAndRenewals() {
        LocalDate today = clockService.today();

        YearMonth thisMonth = YearMonth.from(today);
        YearMonth nextMonth = thisMonth.plusMonths(1);

        LocalDate nextMonthStart = nextMonth.atDay(1);
        LocalDate nextMonthEnd   = nextMonth.atEndOfMonth();

        List<Booking> list = bookings.findAllActiveAutoRenew();
        for (Booking b : list) {

            // H-1 notice based on booking endDate
            if (b.getEndDate() != null) {
                LocalDate noticeDate = b.getEndDate().minusDays(1);
                if (today.equals(noticeDate)) {
                    // send reminder...
                }
            }

            boolean overlapsNextMonth =
                    !b.getStartDate().isAfter(nextMonthEnd) &&
                            (b.getEndDate() == null || !b.getEndDate().isBefore(nextMonthStart));
            if (!overlapsNextMonth) continue;

            LocalDate periodDay1 = nextMonthStart;
            Optional<Payment> existingForPeriod =
                    payments.findByBookingIdAndTypeAndPeriodMonth(
                            b.getId(), Payment.Type.RENT, periodDay1);

            if (existingForPeriod.isEmpty()) {
                BigDecimal amount = billing.quoteSingleMonth(
                        b.getMonthlyPrice(), b.getStartDate(), b.getEndDate(), nextMonth);
                if (amount.signum() > 0) {
                    Payment p = new Payment();
                    p.setBooking(b);
                    p.setType(com.roomledger.app.model.Payment.Type.RENT);
                    p.setStatus(com.roomledger.app.model.Payment.Status.PENDING);
                    p.setAmount(amount);
                    p.setPeriodMonth(periodDay1);
                    payments.save(p);
                }
            }
        }
    }
}

