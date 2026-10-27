package com.svbgua.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/** Indicadores de progreso (círculo que gira) para botones, tablas y mensajes de carga. */
public final class Progreso {
    private Progreso() {}

    public static ProgressIndicator spinner(double tam) {
        ProgressIndicator p = new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS);
        p.setPrefSize(tam, tam);
        p.setMinSize(tam, tam);
        p.setMaxSize(tam, tam);
        return p;
    }

    /** Círculo que gira junto a un texto, por ejemplo "Cargando secciones...". */
    public static HBox caja(String texto) {
        Label l = new Label(texto);
        HBox h = new HBox(10, spinner(22), l);
        h.setAlignment(Pos.CENTER);
        return h;
    }

    /**
     * Pone el botón en modo "trabajando": texto nuevo y un círculo que gira. No se deshabilita (un botón
     * deshabilitado atenúa también la rueda); en su lugar se vuelve intocable y toma el estilo "trabajando".
     */
    public static void enBoton(Button b, String texto) {
        b.setText(texto);
        b.setGraphic(spinner(18));
        b.setContentDisplay(ContentDisplay.LEFT);
        if (!b.getStyleClass().contains("trabajando")) {
            b.getStyleClass().add("trabajando");
        }
        b.setMouseTransparent(true);
        b.setFocusTraversable(false);
    }

    /** true si el botón está en modo "trabajando". */
    public static boolean trabajando(Button b) {
        return b.getStyleClass().contains("trabajando");
    }

    /** Devuelve el botón a su estado normal (queda habilitado). */
    public static void finBoton(Button b, String texto) {
        b.setText(texto);
        b.setGraphic(null);
        b.getStyleClass().remove("trabajando");
        b.setMouseTransparent(false);
        b.setFocusTraversable(true);
        b.setDisable(false);
    }

    /** Muestra el círculo como contenido vacío de la tabla mientras carga; devuelve lo que hay que llamar al terminar. */
    public static Runnable cargandoTabla(TableView<?> tabla) {
        Node original = tabla.getPlaceholder();
        tabla.setPlaceholder(caja("Cargando..."));
        return () -> tabla.setPlaceholder(original);
    }
}
