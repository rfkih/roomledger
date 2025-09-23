package com.roomledger.app.dto;


import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RoomInquiryRequest(
        @NotNull
        UUID buildingId,
        String status,             // optional: AVAILABLE|OCCUPIED|MAINTENANCE (keep for filters like MAINTENANCE)
        BigDecimal minPrice,
        BigDecimal maxPrice,
        @NotNull
        LocalDate startDate,
        @NotNull
        LocalDate endDate
) {}

