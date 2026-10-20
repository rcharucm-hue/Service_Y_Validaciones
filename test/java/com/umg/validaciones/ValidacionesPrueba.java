package com.umg.validaciones;
import com.umg.validaciones.model.*;
import com.umg.validaciones.service.*;
import java.math.BigDecimal;
import java.util.List;
public class ValidacionesPrueba {
    private static int pruebas;
    private static void rechaza(Runnable accion) {
        try { accion.run(); } catch(ValidacionException e) { pruebas++; return; }
        throw new AssertionError("Se aceptaron datos invalidos");
    }
    public static void main(String[] args) {
        Artista a=new Artista(); a.setNombre_artistico(" "); a.setGenero_musical("Rock");
        rechaza(()->ValidacionesService.validar(a));
        a.setNombre_artistico("Artista"); ValidacionesService.validar(a); pruebas++;
        a.setNombre_artistico("x".repeat(121)); rechaza(()->ValidacionesService.validar(a));
        rechaza(()->ValidacionesService.dinero(new BigDecimal("0"),"Precio"));
        rechaza(()->ValidacionesService.dinero(new BigDecimal("0.001"),"Precio"));
        rechaza(()->ValidacionesService.dinero(new BigDecimal("100000000.00"),"Precio"));
        ValidacionesService.dinero(new BigDecimal("99999999.99"),"Precio"); pruebas++;
        InventarioBoleto i=new InventarioBoleto(); i.setId_concierto(1); i.setId_localidad(1);
        i.setPrecio(new BigDecimal("25.00")); i.setCantidad_total(10); i.setCantidad_disponible(11);
        rechaza(()->ValidacionesService.validar(i));
        i.setCantidad_disponible(0); ValidacionesService.validar(i); pruebas++;
        Concierto c=new Concierto(); c.setId_artista(1); c.setTitulo_evento("Concierto"); c.setRecinto("Recinto");
        c.setFecha_concierto("2026-02-30T20:00:00"); rechaza(()->ValidacionesService.validar(c));
        c.setFecha_concierto("2026-11-20T20:00:00-06:00"); c.setEstado("CANCELADO");
        rechaza(()->ValidacionesService.validar(c)); c.setEstado("programado"); ValidacionesService.validar(c); pruebas++;
        DetalleVenta d=new DetalleVenta(); d.setId_inventario(1); d.setCantidad(3);
        d.setPrecio_unitario(new BigDecimal("0.10")); d.setSubtotal(new BigDecimal("0.30"));
        if(ValidacionesService.calcularTotal(List.of(d)).compareTo(new BigDecimal("0.30"))!=0) throw new AssertionError("Total inexacto");
        pruebas++; d.setSubtotal(new BigDecimal("0.31")); rechaza(()->ValidacionesService.validar(d));
        rechaza(()->ValidacionesService.calcularTotal(List.of()));
        rechaza(()->ValidacionesService.validar((Venta)null));
        System.out.println(pruebas+" pruebas locales correctas.");
    }
}
