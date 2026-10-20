package com.umg.validaciones.service;
public class ApiException extends java.io.IOException {
    private final int estado;
    public ApiException(int estado, String mensaje) { super(mensaje); this.estado=estado; }
    public int getEstado() { return estado; }
}
