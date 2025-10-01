package com.roomledger.app.service;

import com.roomledger.app.dto.*;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.*;
import com.roomledger.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final RoomRepository rooms;
    private final TenantRepository tenants;
    private final PaymentRepository payments;
    private final BillingService billing;
    private final ClockService clockService;
    private final OwnerRepository ownerRepository;
    private final BuildingRepository buildingRepository;

    public BookingService(BookingRepository bookings,
                          RoomRepository rooms,
                          TenantRepository tenants,
                          PaymentRepository payments,
                          BillingService billing, ClockService clockService, OwnerRepository ownerRepository, BuildingRepository buildingRepository) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.tenants = tenants;
        this.payments = payments;
        this.billing = billing;
        this.clockService = clockService;
        this.ownerRepository = ownerRepository;
        this.buildingRepository = buildingRepository;
    }

    /** Create a DRAFT booking + DEPOSIT payment + initial RENT bill(s). */
    @Transactional
    public DraftBookingResult createDraftBookingWithBills(CreateBookingRequest req) throws InvalidTransactionException {
        Tenant tenant = tenants.findById(req.tenantId())
                .orElseThrow(() -> new InvalidTransactionException("Tenant not found"));
        Room room = rooms.findById(req.roomId())
                .orElseThrow(() -> new InvalidTransactionException("Room not found"));

        Building bldg = room.getBuilding();
        if (bldg == null) throw new InvalidTransactionException("Room has no building");
        Owner owner = bldg.getOwner();
        if (owner == null) throw new InvalidTransactionException("Building has no owner");

        LocalDate start = req.startDate();
        LocalDate end   = req.endDate();
        if (start == null || end == null || end.isBefore(start)) {
            throw new InvalidTransactionException("Invalid start/end");
        }

        boolean overlap = bookings.existsOverlap(room.getId(), start, end, "CANCELLED");
        if (overlap) throw new InvalidTransactionException("Room already booked in that period");

        // Booking draft
        Booking b = new Booking();
        b.setTenant(tenant);
        b.setRoom(room);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setMonthlyPrice(room.getMonthlyPrice());
        b.setStatus(Booking.Status.DRAFT);
        b.setAutoRenew(true);
        b = bookings.save(b);

        // Deposit
        BigDecimal depositAmt = (req.depositAmount() != null) ? req.depositAmount() : room.getMonthlyPrice();
        Payment dep = new Payment();
        dep.setBooking(b);
        dep.setOwner(owner);
        dep.setBuilding(bldg);
        dep.setType(Payment.Type.DEPOSIT);
        dep.setStatus(Payment.Status.PENDING);
        dep.setAmount(depositAmt);
        dep.setCurrency("IDR");
        dep = payments.save(dep);

        // First rent bill
        BillingQuoteResponse quote = billing.quoteForPeriod(room.getId(), room.getMonthlyPrice(), start, end);
        Payment rent = new Payment();
        rent.setBooking(b);
        rent.setOwner(owner);          // <-- REQUIRED
        rent.setBuilding(bldg);        // <-- REQUIRED
        rent.setType(Payment.Type.RENT);
        rent.setStatus(Payment.Status.PENDING);
        rent.setAmount(quote.totalAfterDiscount());
        rent.setCurrency("IDR");
        payments.save(rent);

        return new DraftBookingResult(b.getId(), dep.getId(), rent.getId(), dep.getAmount(), rent.getAmount(), b.getRoom().getId(), b.getRoom().getBuilding().getId());
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

    @Transactional(readOnly = true)
    public List<ActiveBookingDto> activeWithBillsByPhone(String phone) {
        var today = clockService.today();

        // 1) Active bookings for phone
        var active = bookings.findActiveByPhoneFetch(phone, today);
        if (active.isEmpty()) return List.of();

        // 2) Pending payments for those bookings, grouped by bookingId
        var ids = active.stream().map(Booking::getId).collect(Collectors.toSet());
        var pending = payments.findPendingByBookingIds(ids);
        var byBooking = pending.stream().collect(Collectors.groupingBy(p -> p.getBooking().getId()));

        // 3) Map to DTOs
        return active.stream().map(b -> {
            var t = b.getTenant();
            var r = b.getRoom();

            var bills = byBooking.getOrDefault(b.getId(), List.of())
                    .stream()
                    .map(p -> new PendingBillDto(
                            p.getId(),
                            p.getType().name(),
                            p.getAmount(),
                            p.getPeriodMonth(),
                            p.getCreatedAt()
                    ))
                    .toList();

            return new ActiveBookingDto(
                    b.getId(),
                    b.getStatus().name(),
                    b.getStartDate(),
                    b.getEndDate(),
                    b.isAutoRenew(),
                    t != null ? t.getId() : null,
                    t != null ? t.getName() : null,
                    t != null ? t.getPhone() : null,                                            // adapt
                    r != null ? r.getId() : null,
                    r != null ? r.getRoomNo() : null,
                    bills
            );
        }).toList();
    }


}