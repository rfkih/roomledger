package com.roomledger.app.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record BillingQuoteRequest(
        @NotNull UUID roomId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {}