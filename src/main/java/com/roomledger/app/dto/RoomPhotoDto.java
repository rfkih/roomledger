package com.roomledger.app.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RoomPhotoDto(
        UUID mediaId,
        boolean isMain,
        int sortOrder,
        String url,
        String storageKey,
        String bucket,
        String visibility,     // PUBLIC | SENSITIVE
        String contentType,
        Long size,
        Integer width,
        Integer height,
        LocalDateTime createdAt
) {}
