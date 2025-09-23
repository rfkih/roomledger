package com.roomledger.app.dto;


import java.util.List;
import java.util.UUID;

public record InquiryPaymentResponse(
        UUID bookingId,
        String paymentType,
        String referenceId,
        long totalAmount,
        String currency,
        List<InquiryPaymentInfo> payments
) {}
