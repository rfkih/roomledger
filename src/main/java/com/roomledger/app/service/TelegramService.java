package com.roomledger.app.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramService {
    private final RestClient http;
    private final String token;

    public TelegramService(@Value("${telegram.bot.token}") String token) {
        this.token = token;
        this.http = RestClient.create("https://api.telegram.org");
    }

    public void sendMessage(String chatId, String text) {
        http.post()
                .uri("/bot{token}/sendMessage", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity();
    }

    // optional overload with parseMode if needed later
    public void sendMessage(String chatId, String text, String parseMode) {
        http.post()
                .uri("/bot{token}/sendMessage", token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("chat_id", chatId, "text", text, "parse_mode", parseMode))
                .retrieve()
                .toBodilessEntity();
    }
}