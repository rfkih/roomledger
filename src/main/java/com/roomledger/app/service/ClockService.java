package com.roomledger.app.service;


import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
public class ClockService {
    private final ParamService params;
    private final Instant bootInstant = Instant.now();
    public ClockService(ParamService params) { this.params = params; }

    public ZoneId zone() {
        return params.getZone("APP_ZONE", "Asia/Jakarta");
    }

    public LocalDate today() {
        String mode = params.getString("APP_DATE_MODE", "REALTIME");
        switch (mode) {
            case "MANUAL":
            case "AUTO_TICK":
                return params.getDate("APP_DATE", LocalDate.now(zone()));
            case "REALTIME":
            default:
                return LocalDate.now(zone());
        }
    }

    /** LocalDateTime mengikuti mode:
     *  REALTIME   -> LocalDateTime.now(zone)
     *  MANUAL     -> dari APP_DATETIME (ISO) atau APP_DATE+APP_TIME
     *  AUTO_TICK  -> base manual + durasi sejak aplikasi start
     */
    public LocalDateTime now() {
        final String mode = params.getString("APP_DATE_MODE", "REALTIME");
        final ZoneId z = zone();

        switch (mode) {
            case "MANUAL":
                return resolveManualDateTime(z);
            case "AUTO_TICK":
                return resolveManualDateTime(z).plus(Duration.between(bootInstant, Instant.now()));
            case "REALTIME":
            default:
                return LocalDateTime.now(z);
        }
    }

    // --- Helpers ---
    private LocalDateTime resolveManualDateTime(ZoneId z) {
        // 1) Coba APP_DATETIME (contoh: 2025-09-10T12:34 atau 2025-09-10T12:34:56)
        String dtStr = params.getString("APP_DATETIME", null);
        if (dtStr != null && !dtStr.isBlank()) {
            LocalDateTime ldt = tryParseLocalDateTime(dtStr);
            if (ldt != null) return ldt;

            // Jika user memberi offset (mis. 2025-09-10T12:34:56+07:00), normalisasi ke zona app
            OffsetDateTime odt = tryParseOffsetDateTime(dtStr);
            if (odt != null) return odt.atZoneSameInstant(z).toLocalDateTime();

            throw new IllegalArgumentException("Invalid APP_DATETIME format: " + dtStr);
        }

        // 2) Fallback: APP_DATE + APP_TIME (ISO). TIME opsional, default 00:00
        LocalDate date = params.getDate("APP_DATE", LocalDate.now(z));
        String timeStr = params.getString("APP_TIME", "00:00");
        LocalTime time = tryParseLocalTime(timeStr);
        if (time == null) throw new IllegalArgumentException("Invalid APP_TIME format: " + timeStr);

        return LocalDateTime.of(date, time);
    }

    private static LocalDateTime tryParseLocalDateTime(String s) {
        try {
            // ISO_LOCAL_DATE_TIME (detik/frac detik opsional)
            return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    private static OffsetDateTime tryParseOffsetDateTime(String s) {
        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }

    private static LocalTime tryParseLocalTime(String s) {
        try {
            return LocalTime.parse(s, DateTimeFormatter.ISO_LOCAL_TIME); // "HH:mm" atau "HH:mm:ss"
        } catch (DateTimeParseException ignore) {
            return null;
        }
    }
    public int renewHMinus() {
        return params.getInt("RENEW_H_MINUS", 1);
    }
}

