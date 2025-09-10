package com.roomledger.app.controller;

import com.roomledger.app.dto.BookingPaymentRequest;
import com.roomledger.app.dto.BookingPaymentResponse;
import com.roomledger.app.exthandler.InvalidTransactionException;
import com.roomledger.app.service.PaymentService;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class PaymentController {

    @Value("${application.code}")
    private String applicationCode;

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
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
        BookingPaymentResponse resp = service.pay(bookingId, body);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                resp
                ).getBody();
    }
}
