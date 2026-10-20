package com.umg.validaciones.model;

import java.math.BigDecimal;

public class InventarioBoleto {
    private Integer id_inventario;
    private Integer id_concierto;
    private Integer id_localidad;
    private BigDecimal precio;
    private Integer cantidad_total;
    private Integer cantidad_disponible;

    public InventarioBoleto() {}
    public Integer getId_inventario() { return id_inventario; }
    public void setId_inventario(Integer valor) { this.id_inventario = valor; }
    public Integer getId_concierto() { return id_concierto; }
    public void setId_concierto(Integer valor) { this.id_concierto = valor; }
    public Integer getId_localidad() { return id_localidad; }
    public void setId_localidad(Integer valor) { this.id_localidad = valor; }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal valor) { this.precio = valor; }
    public Integer getCantidad_total() { return cantidad_total; }
    public void setCantidad_total(Integer valor) { this.cantidad_total = valor; }
    public Integer getCantidad_disponible() { return cantidad_disponible; }
    public void setCantidad_disponible(Integer valor) { this.cantidad_disponible = valor; }
}
