package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomStatus implements CodedEnum {
    AVAILABLE("AVAILABLE", "Room is available"),
    OCCUPIED("OCCUPIED", "Room is currently occupied"),
    MAINTENANCE("MAINTENANCE", "Room unavailable due to maintenance");

    private final String code;
    private final String description;

    RoomStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override @JsonValue
    public String getCode() { return code; }

    @Override
    public String getDescription() { return description; }

    public static RoomStatus fromCode(String code) {
        return EnumUtils.fromCode(RoomStatus.class, code);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RoomStatus fromJson(String code) {
        return fromCode(code);
    }
}
