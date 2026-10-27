package com.svbgua.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Carga los archivos .fxml de /com/svbgua/fxml y devuelve la raíz junto con su controlador. */
public final class Fxml {
    private Fxml() {}

    public record Vista<C>(Parent raiz, C controlador) {}

    public static <C> Vista<C> cargar(String nombre) {
        FXMLLoader loader = new FXMLLoader(Fxml.class.getResource("/com/svbgua/fxml/" + nombre + ".fxml"));
        try {
            Parent raiz = loader.load();
            return new Vista<>(raiz, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar " + nombre + ".fxml", e);
        }
    }
}
