package com.roomledger.app.controller;

import com.roomledger.app.client.XenditClientService;
import com.roomledger.app.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/xendit")
@Validated
public class XenditController {

    private final XenditClientService xenditClientService;

    public XenditController(XenditClientService svc) {
        this.xenditClientService = svc;
    }

    /* ---------------- Reusable codes ---------------- */

    @PostMapping("/reusable/va")
    public ResponseEntity<ReusableCodeResponse> createReusableVa(
            @Valid @RequestBody CreateReusableVaRequest req
    ) {
        ReusableCodeResult r = xenditClientService.createReusableVa(
                UUID.fromString(req.customerId()),
                req.displayName(),
                req.channelCode()
        );

        return ResponseEntity.ok(ReusableCodeResponse.from(r));
    }

    @PostMapping("/reusable/qris")
    public ResponseEntity<ReusableCodeResponse> createReusableQris(
            @Valid @RequestBody CreateReusableQrisRequest req
    ) {
        ReusableCodeResult r = xenditClientService.createReusableQris(req.customerId());
        return ResponseEntity.ok(ReusableCodeResponse.from(r));
    }

    /* ---------------- One-off payments (PAY) ---------------- */

//    @PostMapping("/pay/va")
//    public ResponseEntity<Map<String,Object>> createPayVa(
//            @Valid @RequestBody CreatePayVaRequest req
//    ) {
//        Map<String,Object> resp = xenditClientService.createPayVa(
//                req.bookingRef(),
//                req.amount(),
//                req.channelCode(),
//                req.displayName(),
//                req.expectedAmount()   // nullable; set to enforce closed amount if supported
//        );
//        return ResponseEntity.ok(resp);
//    }

    @PostMapping("/pay/qris")
    public ResponseEntity<Map<String,Object>> createPayQris(
            @Valid @RequestBody CreatePayQrisRequest req
    ) {
        Map<String,Object> resp = xenditClientService.createPayQris(req.bookingRef(), req.amount());
        return ResponseEntity.ok(resp);
    }



    @GetMapping("/payment-requests/{id}")
    public ResponseEntity<Map<String,Object>> getPaymentRequest(@PathVariable("id") String id) {
        return ResponseEntity.ok(xenditClientService.getPaymentRequest(id));
    }

    @GetMapping("/payment-requests")
    public ResponseEntity<Map<String,Object>> listPaymentRequestsByReference(
            @RequestParam("referenceId") @NotBlank String referenceId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "afterId", required = false) String afterId
    ) {
        return ResponseEntity.ok(xenditClientService.listPaymentRequestsByReference(referenceId, limit, afterId));
    }
}
