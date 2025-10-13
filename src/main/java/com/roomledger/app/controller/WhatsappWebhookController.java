package com.roomledger.app.controller;

import com.roomledger.app.client.WhatsappClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/callbacks/whatsapp")
@Slf4j
public class WhatsappWebhookController {

    private final WhatsappClientService whatsappClientService;

    public WhatsappWebhookController(WhatsappClientService whatsappClientService) {
        this.whatsappClientService = whatsappClientService;
    }

    /** Step 1: For verification when you connect webhook in Meta dashboard */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String verifyToken
    ) {
        if ("subscribe".equals(mode) && "YOUR_VERIFY_TOKEN".equals(verifyToken)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /** Step 2: For receiving events/messages */
    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> body) {
        log.info("Incoming webhook event: {}", body);

        try {
            var entry = ((List<Map<String, Object>>) body.get("entry")).get(0);
            var changes = (List<Map<String, Object>>) entry.get("changes");
            var value = (Map<String, Object>) changes.get(0).get("value");

            var metadata = (Map<String, Object>) value.get("metadata");
            String phoneNumberId = (String) metadata.get("phone_number_id");
            log.info("\uD83D\uDCE9 Received message on phone number ID: {}", phoneNumberId);

            // Skip fake test payloads from Meta
            if (phoneNumberId == null || phoneNumberId.equals("123456123")) {
                log.info("⚠️ Skipping test webhook event from Meta");
                return ResponseEntity.ok().build();
            }

            var messages = (List<Map<String, Object>>) value.get("messages");
            if (messages != null && !messages.isEmpty()) {
                var msg = messages.get(0);
                var from = (String) msg.get("from");
                var textObj = (Map<String, Object>) msg.get("text");
                var text = textObj != null ? textObj.get("body").toString() : "(non-text message)";

                whatsappClientService.sendTextMessageDynamic(phoneNumberId, from,
                        "Thanks for your message! You said: " + text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }


}

