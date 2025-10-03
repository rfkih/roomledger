package com.roomledger.app.dto;


import java.util.UUID;

public record UploadResult(UUID mediaId, String url, String storageKey, String purpose) {}
