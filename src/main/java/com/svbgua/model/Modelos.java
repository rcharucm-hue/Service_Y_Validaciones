package com.svbgua.model;

import com.svbgua.util.Fmt;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Modelos simples que reflejan las tablas del backend. */
public final class Modelos {
    private Modelos() {}

    public record Usuario(int id, String nombreUsuario, String nombreCompleto, String rol) {
        public boolean esAdmin() {
            return "administrador".equals(rol);
        }
    }

    public record Concierto(int id, int idArtista, String titulo, String fecha, String recinto, String estado) {
        @Override
        public String toString() {
            return titulo + "  ·  " + Fmt.fecha(fecha) + "  ·  " + recinto;
        }
    }

    public record Artista(int id, String nombre, String genero, String pais) {
        @Override
        public String toString() {
            return nombre + " (" + genero + ")";
        }
    }

    public record Localidad(int id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    public record Inventario(int id, int idConcierto, int idLocalidad, BigDecimal precio, int total, int disponible) {}

    public record DetalleVenta(int idInventario, int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

    /** idUsuario / vendedor vienen del backend si los informa en GET /api/ventas (si no, quedan en null). */
    public record Venta(int id, String fecha, BigDecimal total, List<DetalleVenta> detalles, Integer idUsuario, String vendedor) {
        public Venta(int id, String fecha, BigDecimal total, List<DetalleVenta> detalles) {
            this(id, fecha, total, detalles, null, null);
        }
    }

    /** Datos que se piden en el pago. El backend no los guarda; se usan para el comprobante. */
    public record DatosPago(String nombre, boolean consumidorFinal, String nit, String tarjeta, String correo) {
        public String tarjetaEnmascarada() {
            String d = tarjeta.replaceAll("\\D", "");
            String ultimos = d.length() >= 4 ? d.substring(d.length() - 4) : d;
            return "**** **** **** " + ultimos;
        }

        public String clienteTexto() {
            return consumidorFinal ? "Consumidor Final" : nombre;
        }

        public String nitTexto() {
            return consumidorFinal ? "CF" : nit.toUpperCase();
        }
    }

    public static final class ItemCarrito {
        private final Inventario inventario;
        private final String localidad;
        private int cantidad;

        public ItemCarrito(Inventario inventario, String localidad, int cantidad) {
            this.inventario = inventario;
            this.localidad = localidad;
            this.cantidad = cantidad;
        }

        public Inventario inventario() { return inventario; }
        public String localidad() { return localidad; }
        public int cantidad() { return cantidad; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }

        public BigDecimal subtotal() {
            return inventario.precio().multiply(BigDecimal.valueOf(cantidad));
        }
    }

    public record Catalogo(List<Concierto> conciertos, Map<Integer, Localidad> localidades, Map<Integer, Artista> artistas) {
        public static Catalogo vacio() {
            return new Catalogo(List.of(), Map.of(), Map.of());
        }

        public List<Concierto> activos() {
            return conciertos.stream().filter(c -> !"finalizado".equals(c.estado())).toList();
        }

        public String nombreLocalidad(int id) {
            Localidad l = localidades.get(id);
            return l == null ? "Localidad " + id : l.nombre();
        }

        public String nombreArtista(int id) {
            Artista a = artistas.get(id);
            return a == null ? "Artista " + id : a.nombre();
        }

        public String tituloConcierto(int id) {
            return conciertos.stream().filter(c -> c.id() == id).map(Concierto::titulo).findFirst().orElse("Concierto " + id);
        }
    }
}
