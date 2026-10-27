package com.svbgua.kiosco.vista;

import com.svbgua.kiosco.modelo.TipoLocalidad;
import javafx.scene.paint.Color;

/** Colores por tipo de localidad. */
public final class Paleta {

    public static final Color NO_DISPONIBLE = Color.web("#5B6070");

    private Paleta() {
    }

    public static Color de(TipoLocalidad tipo) {
        return switch (tipo) {
            case VIP -> Color.web("#F5A000");
            case PREFERENCIAL -> Color.web("#00D8C3");
            case GENERAL -> Color.web("#00E5A0");
            case MESA -> Color.web("#FF681C");
        };
    }
}
