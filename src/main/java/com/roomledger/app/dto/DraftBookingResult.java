package com.roomledger.app.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DraftBookingResult(
        UUID bookingId,
        UUID depositPaymentId,
        UUID rentPaymentId,
        BigDecimal depositAmount,
        BigDecimal rentAmount,
        UUID roomId,
        UUID buildingId
) {}


