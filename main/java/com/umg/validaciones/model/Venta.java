package com.umg.validaciones.model;

import java.math.BigDecimal;

public class Venta {
    private java.util.List<DetalleVenta> detalles;
    public java.util.List<DetalleVenta> getDetalles() { return detalles; }
    public void setDetalles(java.util.List<DetalleVenta> valor) { detalles = valor; }
    private Integer id_venta;
    private Integer id_vendedor;
    private String fecha_venta;
    private BigDecimal total_venta;

    public Venta() {}
    public Integer getId_venta() { return id_venta; }
    public void setId_venta(Integer valor) { this.id_venta = valor; }
    public Integer getId_vendedor() { return id_vendedor; }
    public void setId_vendedor(Integer valor) { this.id_vendedor = valor; }
    public String getFecha_venta() { return fecha_venta; }
    public void setFecha_venta(String valor) { this.fecha_venta = valor; }
    public BigDecimal getTotal_venta() { return total_venta; }
    public void setTotal_venta(BigDecimal valor) { this.total_venta = valor; }
}
