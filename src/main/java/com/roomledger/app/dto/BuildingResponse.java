package com.roomledger.app.dto;

import com.roomledger.app.model.Commons.Enum.BuildingStatus;

import java.util.UUID;

public record BuildingResponse(
        UUID id,
        UUID ownerId,
        String code,
        String name,
        String address,
        BuildingStatus status,
        UUID defaultWhatsappId,
        String defaultWhatsappPhone // null if not set
) {}