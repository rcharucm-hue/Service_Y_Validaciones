package com.svbgua.util;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Conversión de las fechas ISO que devuelve el backend a fecha local (Guatemala). */
public final class Fechas {
    private static final ZoneId GT = ZoneId.of("America/Guatemala");

    private Fechas() {}

    /** @return la fecha local, o null si el texto no se puede interpretar. */
    public static LocalDate local(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(GT).toLocalDate();
        } catch (RuntimeException e) {
            try {
                return LocalDate.parse(iso.substring(0, 10));
            } catch (RuntimeException e2) {
                return null;
            }
        }
    }
}
