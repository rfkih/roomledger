package com.roomledger.app.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ActiveBookingDto(
        UUID bookingId,
        String status,              // ACTIVE
        LocalDate startDate,
        LocalDate endDate,          // can be null
        boolean autoRenew,
        UUID tenantId,
        String tenantName,
        String phone,
        UUID roomId,
        String roomNo,
        List<PendingBillDto> pendingBills
) {}
