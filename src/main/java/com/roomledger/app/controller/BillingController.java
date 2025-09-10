package com.roomledger.app.controller;


import com.roomledger.app.dto.ActiveBillingResponse;
import com.roomledger.app.dto.BillingQuoteRequest;
import com.roomledger.app.dto.BillingQuoteResponse;
import com.roomledger.app.model.Room;
import com.roomledger.app.repository.RoomRepository;
import com.roomledger.app.service.BillingService;
import com.roomledger.app.util.ResponseCode;
import com.roomledger.app.util.ResponseService;
import com.roomledger.app.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Value("${application.code}")
    private String applicationCode;

    private final RoomRepository rooms;
    private final BillingService billing;

    public BillingController(RoomRepository rooms, BillingService billing) {
        this.rooms = rooms;
        this.billing = billing;
    }

    /** Returns all current unpaid bills (pending payments) for the tenant identified by the given phone number. */
    @GetMapping("/active")
    public ResponseService active(@RequestParam String phone) {
        List<ActiveBillingResponse> resp = billing.activeByPhone(phone);
        return ResponseUtil.setResponse(
                HttpStatus.OK.value(),
                applicationCode,
                ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                resp
        ).getBody();
    }

    /** Calculates a billing quote (with prorated breakdown and totals) for the specified room between the given start and end dates. */
    @PostMapping("/quote")
    public ResponseService     quote(@RequestBody BillingQuoteRequest req) {
            Room room = rooms.findById(req.roomId())
                    .orElseThrow(() -> new IllegalArgumentException("Room not found: " + req.roomId()));

            BillingQuoteResponse resp = billing.quoteForPeriod(
                    room.getId(),
                    room.getMonthlyPrice(),
                    req.startDate(),
                    req.endDate()
            );
            return ResponseUtil.setResponse(
                    HttpStatus.OK.value(),
                    applicationCode,
                    ResponseCode.SUCCESS.getCode(),
                ResponseCode.SUCCESS.getDescription(),
                resp
        ).getBody();
    }
}

