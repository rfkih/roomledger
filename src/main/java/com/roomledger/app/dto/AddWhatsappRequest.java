package com.roomledger.app.dto;

import com.roomledger.app.model.Commons.Enum.OwnerWhatsappNumberStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddWhatsappRequest(
        UUID buildingId,                                   // optional
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "^[0-9+]+$", message = "Only digits and + allowed")
        String phoneNumber,
        @NotBlank @Size(max = 64) String phoneNumberId,
        String accessTokenEnc,                             // optional, already encrypted if you store it that way
        OwnerWhatsappNumberStatus status                   // default ACTIVE
) {}
