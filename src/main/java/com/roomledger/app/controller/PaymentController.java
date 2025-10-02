package com.roomledger.app.controller;

import com.roomledger.app.dto.*;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.model.Commons.Enum.PaymentType;
import com.roomledger.app.service.PaymentService;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Value("${application.code}")
    private String applicationCode;

    private final PaymentService paymentService;

    public PaymentController(PaymentService service) {
        this.paymentService = service;
    }

    /* =========================
     One-off (type = PAY)
     ========================= */
    @PostMapping("/{bookingId}")
    public ResponseEntity<PaymentStartResult> startOneOffVa(
            @PathVariable UUID bookingId,
            @RequestBody @Valid CreateVaPaymentRequest req
    ) throws InvalidTransactionException {
        if (req.expectedAmount() != null && req.expectedAmount() <= 0) {
            throw new IllegalArgumentException("expectedAmount must be > 0 when provided");
        }
        PaymentStartResult result = paymentService.startPayment(
                bookingId,
                req.amount(),
                req.channelCode(),
                req.displayName(),
                req.expectedAmount()
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/inquiry")
    public ResponseService inquirePayment(
            @RequestBody @Valid InquiryPaymentRequest req) throws InvalidTransactionException {

        PaymentType type = PaymentType.valueOf(req.paymentType().toUpperCase());
        InquiryPaymentResponse result = paymentService.inquiryPayment(req.bookingId(), type);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                result
                ).getBody();
    }

    /**
     * User pays deposit only or full (deposit + all pending rent).
     * If deposit becomes PAID, booking -> ACTIVE
     */
    @PostMapping("/{bookingId}/pay")
    public ResponseService pay(
            @PathVariable UUID bookingId,
            @RequestBody BookingPaymentRequest body
    ) throws InvalidTransactionException {
        BookingPaymentResponse resp = paymentService.pay(bookingId, body);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                resp
                ).getBody();
    }
}
