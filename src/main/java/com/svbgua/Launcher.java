package com.svbgua;

/** Punto de entrada para el jar ejecutable (evita el error de "JavaFX runtime components are missing"). */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Main.main(args);
    }
}
