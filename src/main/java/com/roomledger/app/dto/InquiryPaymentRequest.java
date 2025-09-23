package com.roomledger.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InquiryPaymentRequest(
        @NotNull UUID bookingId,
        @NotBlank String paymentType // DEPOSIT | FULL
) {}
