package com.svbgua.util;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fmt {
    private static final ZoneId GT = ZoneId.of("America/Guatemala");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Fmt() {}

    public static String money(BigDecimal valor) {
        return "Q" + String.format(Locale.US, "%,.2f", valor == null ? BigDecimal.ZERO : valor);
    }

    public static String fecha(String iso) {
        if (iso == null) return "";
        try {
            return OffsetDateTime.parse(iso).atZoneSameInstant(GT).format(FECHA);
        } catch (Exception e) {
            return iso;
        }
    }
}
