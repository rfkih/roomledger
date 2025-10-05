package com.roomledger.app.dto;

import com.roomledger.app.model.Commons.Enum.BuildingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBuildingRequest(
        @NotBlank @Size(max = 50)  String code,
        @NotBlank @Size(max = 120) String name,
        String address,
        BuildingStatus status,            // default ACTIVE if null
        UUID defaultWhatsappId            // optional: WA row id to bind as default
) {}
