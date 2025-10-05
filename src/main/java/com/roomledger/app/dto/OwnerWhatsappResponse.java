package com.roomledger.app.dto;


import com.roomledger.app.model.Commons.Enum.OwnerWhatsappNumberStatus;

import java.util.UUID;

public record OwnerWhatsappResponse(
        UUID id,
        UUID ownerId,
        UUID buildingId,
        String phoneNumber,
        String phoneNumberId,
        OwnerWhatsappNumberStatus status
) {}
