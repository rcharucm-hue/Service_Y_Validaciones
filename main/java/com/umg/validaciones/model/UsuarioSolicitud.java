package com.umg.validaciones.model;
public record UsuarioSolicitud(String nombre_usuario, String nombre_completo, String contrasena, String rol) {
    @Override public String toString() { return "UsuarioSolicitud[datos privados]"; }
}
