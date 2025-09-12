// src/main/java/org/example/dashboard/support/DateTimeParamConverter.java
package org.example.dashboard.support;

import org.springframework.core.convert.converter.Converter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 쿼리파라미터 → LocalDateTime 변환.
 * 지원 포맷(예시):
 * - 2025-09-12T10:00:00
 * - 2025-09-12 10:00:00   (Windows/CMD에서 편한 공백 구분)
 * - 2025-09-12            (자정으로 보정)
 * - 2025-09-12T01:00:00Z  (UTC, 오프셋 포함)
 * - 2025-09-12T10:00:00+09:00
 * - 1694484000            (epoch seconds)
 * - 1694484000000         (epoch millis)
 */
public class DateTimeParamConverter implements Converter<String, LocalDateTime> {

    private final ZoneId defaultZone;

    public DateTimeParamConverter(ZoneId defaultZone) {
        this.defaultZone = defaultZone;
    }

    private static final List<DateTimeFormatter> NO_TZ_FORMATTERS = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,                       // 2025-09-12T10:00:00[.SSS]
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),          // 2025-09-12 10:00:00
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),             // 2025-09-12 10:00
        DateTimeFormatter.ISO_LOCAL_DATE                              // 2025-09-12
    );

    @Override
    public LocalDateTime convert(String value) {
        if (value == null || value.isBlank()) return null;

        String s = value.trim();

        // 1) epoch 숫자 (초/밀리초)
        if (s.matches("^\\d{10,13}$")) {
            long epoch = Long.parseLong(s);
            Instant inst = (s.length() == 13) ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch);
            return LocalDateTime.ofInstant(inst, defaultZone);
        }

        // 2) 오프셋/UTC 포함 (Z, +09:00 등)
        try {
            OffsetDateTime odt = OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return odt.atZoneSameInstant(defaultZone).toLocalDateTime();
        } catch (DateTimeParseException ignored) { }

        // 3) 타임존 없는 로컬 날짜/시간 (여러 패턴)
        for (DateTimeFormatter f : NO_TZ_FORMATTERS) {
            try {
                if (f == DateTimeFormatter.ISO_LOCAL_DATE) {
                    LocalDate d = LocalDate.parse(s, f);
                    return d.atStartOfDay();
                } else {
                    return LocalDateTime.parse(s, f);
                }
            } catch (DateTimeParseException ignored) { }
        }

        // 4) 마지막 시도: ISO_INSTANT (예: 2025-09-12T01:00:00Z)
        try {
            Instant inst = Instant.parse(s);
            return LocalDateTime.ofInstant(inst, defaultZone);
        } catch (DateTimeParseException ignored) { }

        throw new IllegalArgumentException("Unsupported datetime format: " + value);
    }
}
