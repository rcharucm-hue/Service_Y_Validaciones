package com.svbgua.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public final class Dialogs {
    private Dialogs() {}

    public static String mensaje(Throwable t) {
        if (t == null) return "Error desconocido.";
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }

    public static void error(String titulo, Throwable t) {
        Alert a = new Alert(Alert.AlertType.ERROR, mensaje(t), ButtonType.OK);
        a.setTitle("Error");
        a.setHeaderText(titulo);
        a.showAndWait();
    }

    public static void aviso(String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING, mensaje, ButtonType.OK);
        a.setTitle("Atención");
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void info(String titulo, String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        a.setTitle(titulo);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static boolean confirmar(String mensaje) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, mensaje, ButtonType.YES, ButtonType.NO);
        a.setTitle("Confirmar");
        a.setHeaderText(null);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}
