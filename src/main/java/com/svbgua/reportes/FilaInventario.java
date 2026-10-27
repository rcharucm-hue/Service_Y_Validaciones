package com.svbgua.reportes;

import java.math.BigDecimal;

/** Fila del reporte "Inventario y recaudación por concierto". */
public class FilaInventario {
    private final String concierto;
    private final String artista;
    private final String localidad;
    private final BigDecimal precio;
    private final int total;
    private final int vendidos;
    private final int disponibles;
    private final String ocupacion;
    private final BigDecimal recaudado;

    public FilaInventario(String concierto, String artista, String localidad, BigDecimal precio, int total,
                          int vendidos, int disponibles, String ocupacion, BigDecimal recaudado) {
        this.concierto = concierto;
        this.artista = artista;
        this.localidad = localidad;
        this.precio = precio;
        this.total = total;
        this.vendidos = vendidos;
        this.disponibles = disponibles;
        this.ocupacion = ocupacion;
        this.recaudado = recaudado;
    }

    public String getConcierto() { return concierto; }
    public String getArtista() { return artista; }
    public String getLocalidad() { return localidad; }
    public BigDecimal getPrecio() { return precio; }
    public int getTotal() { return total; }
    public int getVendidos() { return vendidos; }
    public int getDisponibles() { return disponibles; }
    public String getOcupacion() { return ocupacion; }
    public BigDecimal getRecaudado() { return recaudado; }
}
