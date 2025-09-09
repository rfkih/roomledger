package com.roomledger.app.dto;

import java.time.LocalDateTime;

public record BookingPaymentRequest(
        String scope,              // "DEPOSIT" | "FULL"
        String method,             // e.g. TRANSFER | QRIS
        String reference,          // optional bank ref / note
        LocalDateTime paidAt       // optional; if null -> server now()
) {}