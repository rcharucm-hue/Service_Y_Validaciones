package com.umg.validaciones.model;

public class Usuario {
    private Integer id_usuario;
    private String nombre_usuario;
    private String nombre_completo;
    private String rol;
    private String fecha_creacion;

    public Usuario() {}
    public Integer getId_usuario() { return id_usuario; }
    public void setId_usuario(Integer valor) { this.id_usuario = valor; }
    public String getNombre_usuario() { return nombre_usuario; }
    public void setNombre_usuario(String valor) { this.nombre_usuario = valor; }
    public String getNombre_completo() { return nombre_completo; }
    public void setNombre_completo(String valor) { this.nombre_completo = valor; }
    public String getRol() { return rol; }
    public void setRol(String valor) { this.rol = valor; }
    public String getFecha_creacion() { return fecha_creacion; }
    public void setFecha_creacion(String valor) { this.fecha_creacion = valor; }
}
