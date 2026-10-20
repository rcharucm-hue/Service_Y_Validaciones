package com.umg.validaciones.model;

public class Artista {
    private Integer id_artista;
    private String nombre_artistico;
    private String genero_musical;
    private String pais_origen;

    public Artista() {}
    public Integer getId_artista() { return id_artista; }
    public void setId_artista(Integer valor) { this.id_artista = valor; }
    public String getNombre_artistico() { return nombre_artistico; }
    public void setNombre_artistico(String valor) { this.nombre_artistico = valor; }
    public String getGenero_musical() { return genero_musical; }
    public void setGenero_musical(String valor) { this.genero_musical = valor; }
    public String getPais_origen() { return pais_origen; }
    public void setPais_origen(String valor) { this.pais_origen = valor; }
}
