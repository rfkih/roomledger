package com.roomledger.app.dto;
import com.roomledger.app.model.Commons.Enum.RoomStatus;
import com.roomledger.app.model.Commons.Enum.RoomType;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        UUID buildingId,
        String roomNo,
        BigDecimal monthlyPrice,
        RoomStatus status,
        RoomType roomType,
        Integer capacity,
        BigDecimal sizeM2
) { }
