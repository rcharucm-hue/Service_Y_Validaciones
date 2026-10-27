package com.svbgua.ui;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

/** Limita lo que se puede escribir en un campo para que no entren letras donde solo van números. */
public final class Campos {
    private Campos() {}

    /** Solo dígitos, hasta maxDigitos. */
    public static void soloEnteros(TextField campo, int maxDigitos) {
        String regex = "\\d{0," + maxDigitos + "}";
        campo.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches(regex) ? c : null));
    }

    /** Dígitos con punto decimal opcional (ej. 250.50): hasta maxEnteros enteros y maxDecimales decimales. */
    public static void soloDecimales(TextField campo, int maxEnteros, int maxDecimales) {
        String regex = "\\d{0," + maxEnteros + "}(\\.\\d{0," + maxDecimales + "})?";
        campo.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches(regex) ? c : null));
    }

    /** NIT: números, un guion opcional y el dígito verificador (0-9 o K). */
    public static void nit(TextField campo) {
        campo.setTextFormatter(new TextFormatter<>(
                c -> c.getControlNewText().matches("\\d{0,12}-?[0-9Kk]?") ? c : null));
    }

    /**
     * Selector numérico con flechas que también se puede escribir (máximo 2 dígitos, se muestra como 05, 20...).
     * Al llegar al máximo da la vuelta (23 pasa a 00).
     */
    public static void spinnerDosDigitos(Spinner<Integer> sp, int min, int max, int inicial, int paso) {
        SpinnerValueFactory.IntegerSpinnerValueFactory fabrica =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, inicial, paso);
        fabrica.setWrapAround(true);
        fabrica.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer v) {
                return v == null ? String.format("%02d", min) : String.format("%02d", v);
            }

            @Override
            public Integer fromString(String texto) {
                try {
                    return Math.max(min, Math.min(max, Integer.parseInt(texto.trim())));
                } catch (NumberFormatException e) {
                    return min;
                }
            }
        });
        sp.setValueFactory(fabrica);
        sp.setEditable(true);
        sp.getEditor().setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,2}") ? c : null));
        // Lo escrito solo se confirma con Enter; esto lo confirma también al salir del campo.
        sp.getEditor().focusedProperty().addListener((o, antes, enfocado) -> {
            if (!enfocado) sp.increment(0);
        });
    }
}
