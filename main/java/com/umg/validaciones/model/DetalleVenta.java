package com.umg.validaciones.model;

import java.math.BigDecimal;

public class DetalleVenta {
    private InventarioBoleto inventario;
    public InventarioBoleto getInventario() { return inventario; }
    public void setInventario(InventarioBoleto valor) { inventario = valor; }
    private Integer id_detalle;
    private Integer id_venta;
    private Integer id_inventario;
    private Integer cantidad;
    private BigDecimal precio_unitario;
    private BigDecimal subtotal;

    public DetalleVenta() {}
    public Integer getId_detalle() { return id_detalle; }
    public void setId_detalle(Integer valor) { this.id_detalle = valor; }
    public Integer getId_venta() { return id_venta; }
    public void setId_venta(Integer valor) { this.id_venta = valor; }
    public Integer getId_inventario() { return id_inventario; }
    public void setId_inventario(Integer valor) { this.id_inventario = valor; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer valor) { this.cantidad = valor; }
    public BigDecimal getPrecio_unitario() { return precio_unitario; }
    public void setPrecio_unitario(BigDecimal valor) { this.precio_unitario = valor; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal valor) { this.subtotal = valor; }
}
