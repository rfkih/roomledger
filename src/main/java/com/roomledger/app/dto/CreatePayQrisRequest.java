package com.roomledger.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreatePayQrisRequest(
        @NotBlank String bookingRef,
        @Positive  long amount
) {}
