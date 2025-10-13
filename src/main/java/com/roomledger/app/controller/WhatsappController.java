package com.roomledger.app.controller;

import com.roomledger.app.client.WhatsappClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsappController {

    private final WhatsappClientService whatsappClient;

    public WhatsappController(WhatsappClientService whatsappClient) {
        this.whatsappClient = whatsappClient;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendText(@RequestParam String to, @RequestParam String message) {
        Map<String, Object> response = whatsappClient.sendTextMessage(to, message);
        return ResponseEntity.ok(response);
    }
}

