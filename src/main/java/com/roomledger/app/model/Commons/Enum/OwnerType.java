package com.roomledger.app.model.Commons.Enum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OwnerType implements CodedEnum {
    COMPANY("COMPANY", "Company / Organization"),
    PERSON("PERSON", "Individual person");

    private final String code;
    private final String description;

    OwnerType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override @JsonValue
    public String getCode() { return code; }

    @Override
    public String getDescription() { return description; }

    public static OwnerType fromCode(String code) {
        return EnumUtils.fromCode(OwnerType.class, code);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OwnerType fromJson(String code) {
        return fromCode(code);
    }
}
