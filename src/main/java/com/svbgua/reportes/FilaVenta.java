package com.svbgua.reportes;

import java.math.BigDecimal;

/** Fila del reporte "Detalle de ventas" (un renglón por localidad vendida). JavaBean: Jasper lee los getters. */
public class FilaVenta {
    private final int idVenta;
    private final String fecha;
    private final String concierto;
    private final String artista;
    private final String localidad;
    private final String vendedor;
    private final int cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal subtotal;

    public FilaVenta(int idVenta, String fecha, String concierto, String artista, String localidad, String vendedor,
                     int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
        this.idVenta = idVenta;
        this.fecha = fecha;
        this.concierto = concierto;
        this.artista = artista;
        this.localidad = localidad;
        this.vendedor = vendedor;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    public int getIdVenta() { return idVenta; }
    public String getFecha() { return fecha; }
    public String getConcierto() { return concierto; }
    public String getArtista() { return artista; }
    public String getLocalidad() { return localidad; }
    public String getVendedor() { return vendedor; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}
