package com.roomledger.app.dto;


import com.roomledger.app.model.Commons.Enum.OwnerStatus;
import com.roomledger.app.model.Commons.Enum.OwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOwnerRequest(
        @Size(max = 50) String slug,                      // optional; auto if blank
        @NotBlank @Size(max = 120) String displayName,
        OwnerType type,                                   // default PERSON
        OwnerStatus status,                               // default ACTIVE
        @NotBlank String timezone,                        // default Asia/Jakarta
        @Size(max = 64) String xenditSubId
) {}
