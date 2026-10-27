package com.svbgua.validacion;

/** Datos que no cumplen las reglas del negocio. Se lanza ANTES de llamar al servidor y su mensaje se muestra tal cual al usuario. */
public class ValidacionException extends IllegalArgumentException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
