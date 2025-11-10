package com.roomledger.app.controller;


import com.roomledger.app.dto.TelegramDto.Update;
import com.roomledger.app.service.TelegramService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telegram/webhook")
public class TelegramWebhookController {

    @Value("${telegram.webhook.secret}") String secretToken;
    private final TelegramService telegramService;

    public TelegramWebhookController(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @PostMapping
    public ResponseEntity<Void> onUpdate(
            @RequestHeader(name="X-Telegram-Bot-Api-Secret-Token", required=false) String headerToken,
            @RequestBody Update update
    ) {
        if (headerToken == null || !headerToken.equals(secretToken)) {
            return ResponseEntity.status(403).build();
        }

        if (update != null && update.message != null && update.message.chat != null) {
            String chatId = String.valueOf(update.message.chat.id);
            String text   = update.message.text != null ? update.message.text : "";
            telegramService.sendMessage(
                    chatId,
                    text.startsWith("/start") ? "Webhook OK " : "Pesan diterima via webhook."
            );
        }
        return ResponseEntity.ok().build();
    }
}