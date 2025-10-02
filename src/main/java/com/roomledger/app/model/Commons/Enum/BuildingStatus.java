package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BuildingStatus implements CodedEnum {
    ACTIVE("ACTIVE", "Building is active"),
    INACTIVE("INACTIVE", "Building is inactive");

    private final String code;
    private final String description;

    BuildingStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override @JsonValue
    public String getCode() { return code; }

    @Override
    public String getDescription() { return description; }

    public static BuildingStatus fromCode(String code) {
        return EnumUtils.fromCode(BuildingStatus.class, code);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static BuildingStatus fromJson(String code) {
        return fromCode(code);
    }
}