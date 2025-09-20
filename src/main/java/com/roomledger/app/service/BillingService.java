package com.roomledger.app.service;

import com.roomledger.app.dto.ActiveBillingResponse;
import com.roomledger.app.dto.BillingQuoteResponse;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Booking;
import com.roomledger.app.model.Payment;
import com.roomledger.app.model.Room;
import com.roomledger.app.model.Tenant;
import com.roomledger.app.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class BillingService {
    // Keep the repository if you'll add repo-backed ops later (e.g., saving quotes or reading payments)
    private final PaymentRepository payments;

    public BillingService(PaymentRepository payments) {
        this.payments = payments;
    }

    /** Currency rounding / scales used across calculations */
    private static final int SCALE_DAILY = 6;   // precise daily rate
    private static final int SCALE_MONEY = 2;   // currency (cents)
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    /**
     * Discount tiers: key = minimum whole months, value = discount rate.
     * Example: 3 -> 5%, 6 -> 6%, 12 -> 8%
     * Adjust these in one place and both methods will honor it.
     */
    private static final NavigableMap<Integer, BigDecimal> DISCOUNT_TIERS;
    static {
        NavigableMap<Integer, BigDecimal> tiers = new TreeMap<>();
        tiers.put(3,  new BigDecimal("0.05"));
        tiers.put(6,  new BigDecimal("0.06"));
        tiers.put(12, new BigDecimal("0.08")); // change to your preferred 12-month rate
        DISCOUNT_TIERS = tiers;
    }

    /**
     * Build a multi-month quote from startDate..endDate (inclusive),
     * prorating per calendar month and applying a single discount
     * based on the count of whole months in the entire span.
     */
    public BillingQuoteResponse quoteForPeriod(
            UUID roomId,
            BigDecimal monthlyPrice,
            LocalDate startDate,
            LocalDate endDate
    ) {
        // ---- validation
        Objects.requireNonNull(monthlyPrice, "monthlyPrice is required");
        Objects.requireNonNull(startDate, "startDate is required");
        Objects.requireNonNull(endDate, "endDate is required");
        if (monthlyPrice.signum() < 0) {
            throw new IllegalArgumentException("monthlyPrice must be >= 0");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must be on/after startDate");
        }

        // ---- per-month proration
        List<BillingQuoteResponse.Line> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        YearMonth cursor = YearMonth.from(startDate);
        YearMonth last    = YearMonth.from(endDate);

        while (!cursor.isAfter(last)) {
            LocalDate monthStart = cursor.atDay(1);
            LocalDate monthEnd   = cursor.atEndOfMonth();

            LocalDate overlapStart = startDate.isAfter(monthStart) ? startDate : monthStart;
            LocalDate overlapEnd   = endDate.isBefore(monthEnd) ? endDate : monthEnd;

            int billableDays = 0;
            if (!overlapEnd.isBefore(overlapStart)) {
                billableDays = (int) ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1; // inclusive
            }

            if (billableDays > 0) {
//                int daysInMonth = monthEnd.getDayOfMonth();
                int daysInMonth = 31;
                BigDecimal daily = safeDailyRate(monthlyPrice, daysInMonth);
                BigDecimal lineSubtotal = daily
                        .multiply(BigDecimal.valueOf(billableDays))
                        .setScale(SCALE_MONEY, RM);

                lines.add(new BillingQuoteResponse.Line(
                        cursor.toString(),                // "yyyy-MM"
                        daysInMonth,
                        billableDays,
                        daily.setScale(4, RM),
                        lineSubtotal
                ));

                subtotal = subtotal.add(lineSubtotal);
            }

            cursor = cursor.plusMonths(1);
        }

        // ---- discount based on whole months over the entire booking
        int fullMonths = countWholeMonths(startDate, endDate);
        BigDecimal discountRate = tierForMonths(fullMonths);
        BigDecimal discountAmount = subtotal.multiply(discountRate).setScale(SCALE_MONEY, RM);
        BigDecimal total = subtotal.subtract(discountAmount).setScale(SCALE_MONEY, RM);

        return new BillingQuoteResponse(
                roomId, startDate, endDate, monthlyPrice,
                fullMonths, discountRate, subtotal, discountAmount, total,
                List.copyOf(lines)
        );
    }

    /**
     * Quote a single YearMonth, clipped by the booking range, and apply
     * the discount tier using the whole months between bookingStart and
     * the EFFECTIVE booking end (bookingEndOrNull or this period's monthEnd).
     * This fixes the previous behavior that always used the period's monthEnd,
     * ignoring a shorter contract end.
     */
    public BigDecimal quoteSingleMonth(
            BigDecimal monthlyPrice,
            LocalDate bookingStart,
            LocalDate bookingEndOrNull,
            YearMonth period
    ) throws InvalidTransactionException {
        Objects.requireNonNull(monthlyPrice, "monthlyPrice is required");
        Objects.requireNonNull(bookingStart, "bookingStart is required");
        Objects.requireNonNull(period, "period is required");
        if (monthlyPrice.signum() < 0) {
            throw new InvalidTransactionException("monthlyPrice must be >= 0");
        }

        LocalDate monthStart = period.atDay(1);
        LocalDate monthEnd   = period.atEndOfMonth();


        LocalDate effectiveEnd = Optional.ofNullable(bookingEndOrNull).orElse(monthEnd);

        if (effectiveEnd.isBefore(monthStart) || bookingStart.isAfter(monthEnd)) {
            return BigDecimal.ZERO.setScale(SCALE_MONEY, RM);
        }

        LocalDate start = bookingStart.isAfter(monthStart) ? bookingStart : monthStart;
        LocalDate end   = effectiveEnd.isBefore(monthEnd) ? effectiveEnd : monthEnd;

        int billableDays = (int) ChronoUnit.DAYS.between(start, end) + 1; // inclusive
        if (billableDays <= 0) {
            return BigDecimal.ZERO.setScale(SCALE_MONEY, RM);
        }

        int daysInMonth = monthEnd.getDayOfMonth();
        BigDecimal daily = safeDailyRate(monthlyPrice, daysInMonth);
        BigDecimal prorated = daily.multiply(BigDecimal.valueOf(billableDays));

        LocalDate tierEnd = Optional.ofNullable(bookingEndOrNull).orElse(monthEnd);
        int whole = countWholeMonths(bookingStart, tierEnd);
        BigDecimal discount = tierForMonths(whole);

        return prorated
                .multiply(BigDecimal.ONE.subtract(discount))
                .setScale(SCALE_MONEY, RM);
    }

    /** Helper: compute precise daily rate from a monthly price. */
    private static BigDecimal safeDailyRate(BigDecimal monthlyPrice, int daysInMonth) {
        if (daysInMonth <= 0) {
            throw new IllegalArgumentException("daysInMonth must be > 0");
        }
        return monthlyPrice.divide(BigDecimal.valueOf(daysInMonth), SCALE_DAILY, RM);
    }

    /** Counts whole months such that start + N months <= end (inclusive boundary semantics). */
    private static int countWholeMonths(LocalDate start, LocalDate end) {
        int months = 0;
        LocalDate cur = start;
        while (!cur.plusMonths(1).isAfter(end)) {
            months++;
            cur = cur.plusMonths(1);
        }
        return months;
    }

    /** Look up the applicable discount for a given number of whole months. */
    private static BigDecimal tierForMonths(int wholeMonths) {
        Map.Entry<Integer, BigDecimal> tier = DISCOUNT_TIERS.floorEntry(wholeMonths);
        return (tier == null) ? BigDecimal.ZERO : tier.getValue();
    }

    @Transactional(readOnly = true)
    public List<ActiveBillingResponse> activeByPhone(String phone) {
        List<Payment> list = payments.findActiveBillingByPhoneFetch(phone, Booking.Status.CANCELLED);
        return list.stream().map(p -> {
            Booking b = p.getBooking();
            Tenant t = b.getTenant();
            Room r = b.getRoom();
            return new ActiveBillingResponse(
                    p.getId(),
                    p.getType().name(),
                    p.getStatus().name(),
                    p.getAmount(),
                    p.getPeriodMonth(),
                    p.getCreatedAt(),

                    b.getId(),
                    b.getStatus().name(),
                    (t != null ? t.getId() : null),
                    (t != null ? t.getName() : null),
                    (t != null ? t.getPhone() : null),
                    (r != null ? r.getId() : null),
                    (r != null ? r.getRoomNo() : null)
            );
        }).toList();
    }
}
