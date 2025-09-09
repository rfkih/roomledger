package com.roomledger.app.dto;


import java.util.UUID;

public record BookingPaymentResponse(
        String scope,
        int depositUpdated,        // number of deposit payments flipped to PAID (0/1)
        int rentUpdated,           // number of rent payments flipped to PAID (0/1)
        String bookingStatus,      // final booking status
        UUID depositPaymentId,
        UUID rentPaymentId
) {}
