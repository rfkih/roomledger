package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomType implements CodedEnum {
    STUDIO("STUDIO", "Compact open-plan room combining bedroom and living area; often with a small pantry/kitchenette."),
    SINGLE("SINGLE",   "Single-occupancy room with one single bed."),
    DOUBLE("DOUBLE",   "Room with one double/queen bed suitable for up to two occupants."),
    TWIN("TWIN",  "Room with two separate single beds for up to two occupants."),
    DELUXE("DELUXE",   "Larger upgraded room with enhanced amenities and/or better view."),
    SUITE("SUITE",    "Spacious unit featuring a separate bedroom and living/lounge area.");


    private final String code;
    private final String description;

    RoomType(String code, String description) { this.code = code; this.description = description; }

    @Override @JsonValue
    public String getCode() { return code; }
    @Override public String getDescription() { return description; }

    public static PaymentType fromCode(String code) { return EnumUtils.fromCode(PaymentType.class, code); }
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static PaymentType fromJson(String code) { return fromCode(code); }
}
