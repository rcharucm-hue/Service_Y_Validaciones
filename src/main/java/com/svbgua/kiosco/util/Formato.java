package com.svbgua.kiosco.util;

import java.math.BigDecimal;
import java.util.Locale;

/** Formato de moneda en quetzales. */
public final class Formato {

    private Formato() {
    }

    /** Ejemplo: Q1,190.00 */
    public static String moneda(BigDecimal v) {
        return String.format(Locale.US, "Q%,.2f", v);
    }

    /** Ejemplo: Q1,190 */
    public static String monedaCorta(BigDecimal v) {
        return String.format(Locale.US, "Q%,.0f", v);
    }
}
