package com.roomledger.app.dto;

import java.time.LocalDate;
import java.util.UUID;

public record BillingQuoteRequest(
        UUID roomId,
        LocalDate startDate,
        LocalDate endDate
) {}