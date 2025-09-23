package com.roomledger.app.controller;

import com.roomledger.app.service.XenditService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/callbacks/xendit")
public class XenditWebhookController {

    private final XenditService xenditService;

    public XenditWebhookController(XenditService service) {
        this.xenditService = service;
    }

    @PostMapping
    public ResponseEntity<?> handle(@RequestHeader(value = "x-callback-token", required = false) String token,
                                    @RequestBody Map<String, Object> payload) {
        xenditService.verifyTokenOrThrow(token);
        XenditService.ProcessResult r = xenditService.accept(payload);
        return ResponseEntity.ok(Map.of("status", "ok", "info", r.message()));
    }
}
