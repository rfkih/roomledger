package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BookingStatus implements CodedEnum {
    DRAFT("DRAFT", "Draft booking (editable, not confirmed)"),
    ACTIVE("ACTIVE", "Confirmed/ongoing booking"),
    ENDED("ENDED", "Booking completed"),
    CANCELLED("CANCELLED", "Booking cancelled");

    private final String code;
    private final String description;

    BookingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override @JsonValue
    public String getCode() { return code; }

    @Override
    public String getDescription() { return description; }

    public static BookingStatus fromCode(String code) {
        return EnumUtils.fromCode(BookingStatus.class, code);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BookingStatus fromJson(String code) {
        return fromCode(code);
    }
}