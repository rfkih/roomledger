package com.roomledger.app.dto;

import java.util.UUID;

public record DraftBookingResult(UUID bookingId, UUID depositPaymentId) {}
