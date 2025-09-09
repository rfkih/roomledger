package com.roomledger.app.dto;


import java.math.BigDecimal;
import java.util.UUID;

public record CreateBookingResponse(
        UUID bookingId,
        UUID depositPaymentId,
        BigDecimal depositAmount
) {}
