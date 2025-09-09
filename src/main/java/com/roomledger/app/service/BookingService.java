package com.roomledger.app.service;

import com.roomledger.app.dto.CreateBookingRequest;
import com.roomledger.app.dto.BillingQuoteResponse;
import com.roomledger.app.dto.DraftBookingResult;
import com.roomledger.app.exthandler.BadRequestException;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.*;
import com.roomledger.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final TenantRepository tenants;
    private final PaymentRepository payments;
    private final BillingService billing;

    public BookingService(BookingRepository bookings,
                          RoomRepository rooms,
                          TenantRepository tenants,
                          PaymentRepository payments,
                          BillingService billing) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.tenants = tenants;
        this.payments = payments;
        this.billing = billing;
    }

    /** Create a DRAFT booking + DEPOSIT payment + initial RENT bill(s). */
    @Transactional
    public DraftBookingResult createDraftBookingWithBills(CreateBookingRequest req) throws InvalidTransactionException {
        Tenant tenant = tenants.findById(req.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        Room room = rooms.findById(req.roomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        LocalDate start = req.startDate();
        LocalDate end   = req.endDate();
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Invalid start/end");
        }

        // prevent overlap with existing non-cancelled bookings
        boolean overlap = bookings.existsOverlap(room.getId(), start, end, Booking.Status.CANCELLED);
        if (overlap) throw new InvalidTransactionException("Room already booked in that period");

        // Create booking in DRAFT (does NOT occupy yet)
        Booking b = new Booking();
        b.setTenant(tenant);
        b.setRoom(room);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setMonthlyPrice(room.getMonthlyPrice());
        b.setStatus(Booking.Status.DRAFT);
        b.setAutoRenew(true);
        // deposit default = 1x monthly unless provided
        BigDecimal depositAmt = (req.depositAmount() != null) ? req.depositAmount() : room.getMonthlyPrice();
        b.setDepositAmount(depositAmt);
        b = bookings.save(b);

        // Create DEPOSIT payment (PENDING)
        Payment dep = new Payment();
        dep.setBooking(b);
        dep.setType(Payment.Type.DEPOSIT);
        dep.setStatus(Payment.Status.PENDING);
        dep.setAmount(depositAmt);
        dep = payments.save(dep);

        BillingQuoteResponse quote = billing.quoteForPeriod(room.getId(), room.getMonthlyPrice(), start, end);

        Payment rent = new Payment();
        rent.setBooking(b);
        rent.setType(Payment.Type.RENT);
        rent.setStatus(Payment.Status.PENDING);
        rent.setAmount(quote.totalAfterDiscount());
        payments.save(rent);


        return new DraftBookingResult(b.getId(), dep.getId());
    }

    /** After DEPOSIT is verified, activate booking and (optionally) mark room OCCUPIED. */
    @Transactional
    public void activateOnDepositVerified(UUID depositPaymentId) throws InvalidTransactionException {
        Payment p = payments.findById(depositPaymentId).orElseThrow();
        if (p.getType() != Payment.Type.DEPOSIT) throw new InvalidTransactionException("Not a deposit");
        if (p.getStatus() == Payment.Status.VERIFIED) throw new InvalidTransactionException("Deposit already verified");
        if (p.getStatus() != Payment.Status.PAID) throw new InvalidTransactionException("Deposit not paid");

        p.setStatus(Payment.Status.VERIFIED);
        payments.save(p);
    }

    @Transactional
    public void activateOnRentVerified(UUID paymentId) throws InvalidTransactionException {
        Payment p = payments.findById(paymentId).orElseThrow();
        if (p.getType() != Payment.Type.RENT) throw new InvalidTransactionException("Not a rent");
        if (p.getStatus() == Payment.Status.VERIFIED) throw new InvalidTransactionException("Rent already verified");
        if (p.getStatus() != Payment.Status.PAID) throw new InvalidTransactionException("Rent not paid");

        p.setStatus(Payment.Status.VERIFIED);
        payments.save(p);
    }


}