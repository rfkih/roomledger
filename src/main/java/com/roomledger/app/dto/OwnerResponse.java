package com.roomledger.app.dto;

import com.roomledger.app.model.Commons.Enum.OwnerStatus;
import com.roomledger.app.model.Commons.Enum.OwnerType;

import java.util.UUID;

public record OwnerResponse(
        UUID id,
        String slug,
        String displayName,
        OwnerType type,
        OwnerStatus status,
        String timezone,
        String xenditSubId
) {}