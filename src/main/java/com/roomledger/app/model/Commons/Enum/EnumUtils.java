package com.roomledger.app.model.Commons.Enum;

public final class EnumUtils {
    private EnumUtils() {}
    public static <E extends Enum<E> & CodedEnum> E fromCode(Class<E> type, String code) {
        for (E e : type.getEnumConstants())
            if (e.getCode().equalsIgnoreCase(code)) return e;
        throw new IllegalArgumentException("Invalid code for " + type.getSimpleName() + ": " + code);
    }
}
