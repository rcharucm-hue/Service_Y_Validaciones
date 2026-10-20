package com.umg.validaciones.service;

import com.umg.validaciones.model.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

public final class ValidacionesService {
    private ValidacionesService() {}
    private static void exigir(boolean cumple, String mensaje) {
        if (!cumple) throw new ValidacionException(mensaje);
    }
    private static void objeto(Object valor) { exigir(valor != null, "Debe proporcionar los datos."); }
    public static void id(Integer valor, String campo) {
        exigir(valor != null && valor > 0, campo + " debe ser un entero positivo.");
    }
    private static void texto(String valor, String campo, int maximo, boolean obligatorio) {
        exigir(!obligatorio || (valor != null && !valor.isBlank()), campo + " es obligatorio.");
        exigir(valor == null || valor.trim().length() <= maximo,
                campo + " supera " + maximo + " caracteres.");
    }
    private static void opcion(String valor, String campo, Set<String> opciones) {
        exigir(valor != null && opciones.contains(valor), campo + " debe ser uno de: " + opciones);
    }
    public static void dinero(BigDecimal valor, String campo) {
        exigir(valor != null && valor.signum() > 0, campo + " debe ser mayor que cero.");
        try {
            BigDecimal exacto = valor.setScale(2, RoundingMode.UNNECESSARY);
            exigir(exacto.compareTo(new BigDecimal("99999999.99")) <= 0,
                    campo + " supera DECIMAL(10,2).");
        } catch (ArithmeticException e) {
            throw new ValidacionException(campo + " admite como maximo dos decimales.");
        }
    }
    private static void fecha(String valor, String campo, boolean obligatorio) {
        if (valor == null) {
            exigir(!obligatorio, campo + " es obligatorio.");
            return;
        }
        try { OffsetDateTime.parse(valor); }
        catch (DateTimeParseException e) {
            try { LocalDateTime.parse(valor); }
            catch (DateTimeParseException error) {
                throw new ValidacionException(campo + " debe usar fecha y hora ISO 8601.");
            }
        }
    }
    public static void validar(Artista m) {
        objeto(m); texto(m.getNombre_artistico(), "Nombre artistico",120,true);
        texto(m.getGenero_musical(), "Genero musical",50,true);
        texto(m.getPais_origen(), "Pais de origen",50,false);
    }
    public static void validar(Concierto m) {
        objeto(m); id(m.getId_artista(), "Artista");
        texto(m.getTitulo_evento(), "Titulo",150,true);
        fecha(m.getFecha_concierto(), "Fecha del concierto",true);
        texto(m.getRecinto(), "Recinto",120,true);
        // Al crear, un estado nulo se omite para que la API aplique su valor por defecto.
        if(m.getEstado()!=null) opcion(m.getEstado(),"Estado",Set.of("programado","activo","finalizado"));
    }
    public static void validar(Localidad m) {
        objeto(m); texto(m.getNombre(), "Localidad",50,true);
    }
    public static void validar(InventarioBoleto m) {
        objeto(m); id(m.getId_concierto(),"Concierto"); id(m.getId_localidad(),"Localidad");
        dinero(m.getPrecio(),"Precio"); id(m.getCantidad_total(),"Cantidad total");
        exigir(m.getCantidad_disponible()!=null && m.getCantidad_disponible()>=0,
                "Cantidad disponible debe ser un entero no negativo.");
        exigir(m.getCantidad_disponible()<=m.getCantidad_total(),
                "Cantidad disponible no puede superar la cantidad total.");
    }
    public static void validar(Usuario m) {
        objeto(m); texto(m.getNombre_usuario(),"Nombre de usuario",50,true);
        texto(m.getNombre_completo(),"Nombre completo",100,true);
        opcion(m.getRol(),"Rol",Set.of("administrador","vendedor"));
        fecha(m.getFecha_creacion(),"Fecha de creacion",false);
    }
    public static void validar(UsuarioSolicitud m, boolean parcial) {
        objeto(m);
        exigir(!parcial || m.nombre_usuario()!=null || m.nombre_completo()!=null || m.contrasena()!=null || m.rol()!=null,
                "Envia al menos un campo para actualizar.");
        if (!parcial || m.nombre_usuario()!=null) {
            texto(m.nombre_usuario(),"Nombre de usuario",50,true);
            exigir(!m.nombre_usuario().contains(":"),"El nombre de usuario no puede contener dos puntos.");
        }
        if (!parcial || m.nombre_completo()!=null) texto(m.nombre_completo(),"Nombre completo",100,true);
        if (!parcial || m.rol()!=null) opcion(m.rol(),"Rol",Set.of("administrador","vendedor"));
        if (!parcial || m.contrasena()!=null) exigir(m.contrasena()!=null && m.contrasena().length()>=8
                && m.contrasena().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72,
                "La contrasena debe tener al menos 8 caracteres y como maximo 72 bytes UTF-8.");
    }
    public static void validarDetalles(List<DetalleSolicitud> detalles) {
        exigir(detalles!=null && !detalles.isEmpty() && detalles.size()<=50,
                "La venta debe incluir entre 1 y 50 tipos de boleto.");
        Set<Integer> incluidos=new java.util.HashSet<>();
        for(DetalleSolicitud detalle:detalles) {
            objeto(detalle); id(detalle.id_inventario(),"Inventario"); id(detalle.cantidad(),"Cantidad");
            exigir(incluidos.add(detalle.id_inventario()),"No repitas inventario; agrupa las cantidades.");
        }
    }
    public static void validar(DetalleVenta m) {
        objeto(m); id(m.getId_inventario(),"Inventario"); id(m.getCantidad(),"Cantidad");
        dinero(m.getPrecio_unitario(),"Precio unitario"); dinero(m.getSubtotal(),"Subtotal");
        exigir(m.getPrecio_unitario().multiply(BigDecimal.valueOf(m.getCantidad())).compareTo(m.getSubtotal())==0,
                "Subtotal debe ser precio unitario por cantidad.");
    }
    public static void validar(Venta m) {
        objeto(m); id(m.getId_vendedor(),"Vendedor"); dinero(m.getTotal_venta(),"Total");
        fecha(m.getFecha_venta(),"Fecha de venta",false);
    }
    public static BigDecimal calcularTotal(List<DetalleVenta> detalles) {
        exigir(detalles!=null && !detalles.isEmpty(),"Debe seleccionar al menos un detalle.");
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleVenta detalle : detalles) {
            validar(detalle); total=total.add(detalle.getSubtotal());
        }
        dinero(total,"Total"); return total.setScale(2, RoundingMode.UNNECESSARY);
    }
}
