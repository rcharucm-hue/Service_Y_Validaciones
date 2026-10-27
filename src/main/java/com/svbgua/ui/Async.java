package com.svbgua.ui;

import javafx.concurrent.Task;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ejecuta llamadas al backend fuera del hilo de JavaFX y devuelve el resultado en el hilo de la interfaz. */
public final class Async {
    private Async() {}

    public static <T> void run(Supplier<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() {
                return trabajo.get();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> alFallar.accept(tarea.getException()));
        Thread hilo = new Thread(tarea, "api-worker");
        hilo.setDaemon(true);
        hilo.start();
    }
}
