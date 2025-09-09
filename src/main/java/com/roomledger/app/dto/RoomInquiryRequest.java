package com.roomledger.app.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RoomInquiryRequest(
        UUID buildingId,
        String status,             // optional: AVAILABLE|OCCUPIED|MAINTENANCE (keep for filters like MAINTENANCE)
        BigDecimal minPrice,
        BigDecimal maxPrice,
        LocalDate startDate,       // optional; if both start & end present -> availability filter
        LocalDate endDate
) {}

