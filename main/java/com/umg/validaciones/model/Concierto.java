package com.umg.validaciones.model;

public class Concierto {
    private Integer id_concierto;
    private Integer id_artista;
    private String titulo_evento;
    private String fecha_concierto;
    private String recinto;
    private String estado;

    public Concierto() {}
    public Integer getId_concierto() { return id_concierto; }
    public void setId_concierto(Integer valor) { this.id_concierto = valor; }
    public Integer getId_artista() { return id_artista; }
    public void setId_artista(Integer valor) { this.id_artista = valor; }
    public String getTitulo_evento() { return titulo_evento; }
    public void setTitulo_evento(String valor) { this.titulo_evento = valor; }
    public String getFecha_concierto() { return fecha_concierto; }
    public void setFecha_concierto(String valor) { this.fecha_concierto = valor; }
    public String getRecinto() { return recinto; }
    public void setRecinto(String valor) { this.recinto = valor; }
    public String getEstado() { return estado; }
    public void setEstado(String valor) { this.estado = valor; }
}
