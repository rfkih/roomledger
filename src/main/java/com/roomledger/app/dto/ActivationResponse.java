package com.roomledger.app.dto;

import java.util.UUID;

public record ActivationResponse(
        UUID bookingId,
        UUID roomId,
        String bookingStatus,
        String payementStatus
) {}
