package com.roomledger.app.dto;

import com.roomledger.app.model.Commons.Enum.RoomStatus;
import com.roomledger.app.model.Commons.Enum.RoomType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRoomRequest(
        UUID buildingId,                                   // optional
        @NotBlank String roomNo,
        @NotNull @Positive BigDecimal monthlyPrice,
        RoomStatus status,                                 // optional (default AVAILABLE)
        RoomType roomType,                                 // optional (default STUDIO)
        @Min(1) Integer capacity,                          // optional (default 1)
        @DecimalMin(value = "0.00") BigDecimal sizeM2      // optional
) { }
