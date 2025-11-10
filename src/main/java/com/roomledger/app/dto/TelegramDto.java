package com.roomledger.app.dto;

public class TelegramDto {
    public static class Update { public Message message; }
    public static class Message { public Long message_id; public Chat chat; public String text; }
    public static class Chat { public Long id; public String type; }
}