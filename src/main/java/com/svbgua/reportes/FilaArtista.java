package com.svbgua.reportes;

import java.math.BigDecimal;

/** Fila del reporte "Popularidad de artistas". */
public class FilaArtista {
    private final int posicion;
    private final String artista;
    private final String genero;
    private final int conciertos;
    private final int boletos;
    private final BigDecimal recaudado;

    public FilaArtista(int posicion, String artista, String genero, int conciertos, int boletos, BigDecimal recaudado) {
        this.posicion = posicion;
        this.artista = artista;
        this.genero = genero;
        this.conciertos = conciertos;
        this.boletos = boletos;
        this.recaudado = recaudado;
    }

    public int getPosicion() { return posicion; }
    public String getArtista() { return artista; }
    public String getGenero() { return genero; }
    public int getConciertos() { return conciertos; }
    public int getBoletos() { return boletos; }
    public BigDecimal getRecaudado() { return recaudado; }
}
