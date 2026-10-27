package com.svbgua.reportes;

import com.svbgua.model.Modelos.*;
import com.svbgua.util.Fechas;
import com.svbgua.util.Fmt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Arma las filas de cada reporte a partir de los datos del backend y aplica los filtros
 * (concierto, artista, vendedor y rango de fechas). No depende de Jasper ni de JavaFX.
 */
public final class ReporteDatos {
    private ReporteDatos() {}

    /** Filtros opcionales: null = sin filtrar por ese criterio. */
    public record Filtros(Integer idConcierto, Integer idArtista, Integer idVendedor, LocalDate desde, LocalDate hasta) {
        public static Filtros ninguno() {
            return new Filtros(null, null, null, null, null);
        }
    }

    public record Fuente(Catalogo catalogo, List<Inventario> inventarios, List<Venta> ventas, List<Usuario> usuarios) {}

    /** true si el backend informa quién hizo cada venta (sin eso no se puede filtrar por vendedor). */
    public static boolean hayDatoDeVendedor(List<Venta> ventas) {
        return ventas.stream().anyMatch(v -> v.idUsuario() != null || v.vendedor() != null);
    }

    // ------------------------------------------------------------------ detalle de ventas

    private record Linea(Venta venta, DetalleVenta detalle, Concierto concierto, Artista artista, String localidad,
                         String vendedor) {}

    private static List<Linea> lineas(Fuente f, Filtros fl) {
        Map<Integer, Inventario> inv = new HashMap<>();
        for (Inventario i : f.inventarios()) inv.put(i.id(), i);
        Map<Integer, Concierto> conciertos = new HashMap<>();
        for (Concierto c : f.catalogo().conciertos()) conciertos.put(c.id(), c);
        Map<Integer, String> usuarios = new HashMap<>();
        for (Usuario u : f.usuarios()) usuarios.put(u.id(), u.nombreCompleto());

        List<Linea> salida = new ArrayList<>();
        for (Venta v : f.ventas()) {
            if (fl.desde() != null || fl.hasta() != null) {
                LocalDate dia = Fechas.local(v.fecha());
                if (dia == null || fl.desde() != null && dia.isBefore(fl.desde())
                        || fl.hasta() != null && dia.isAfter(fl.hasta())) {
                    continue;
                }
            }
            if (fl.idVendedor() != null && !fl.idVendedor().equals(v.idUsuario())) {
                continue;
            }
            String vendedor = v.vendedor() != null ? v.vendedor()
                    : v.idUsuario() != null ? usuarios.getOrDefault(v.idUsuario(), "Usuario #" + v.idUsuario())
                    : "Sin dato";
            for (DetalleVenta d : v.detalles()) {
                Inventario i = inv.get(d.idInventario());
                Concierto c = i == null ? null : conciertos.get(i.idConcierto());
                if (fl.idConcierto() != null && (c == null || c.id() != fl.idConcierto())) continue;
                if (fl.idArtista() != null && (c == null || c.idArtista() != fl.idArtista())) continue;
                Artista a = c == null ? null : f.catalogo().artistas().get(c.idArtista());
                String localidad = i == null ? "Inventario " + d.idInventario() : f.catalogo().nombreLocalidad(i.idLocalidad());
                salida.add(new Linea(v, d, c, a, localidad, vendedor));
            }
        }
        return salida;
    }

    public static List<FilaVenta> ventas(Fuente f, Filtros fl) {
        List<FilaVenta> filas = new ArrayList<>();
        for (Linea l : lineas(f, fl)) {
            filas.add(new FilaVenta(l.venta().id(), Fmt.fecha(l.venta().fecha()),
                    l.concierto() == null ? "—" : l.concierto().titulo(),
                    l.artista() == null ? "—" : l.artista().nombre(),
                    l.localidad(), l.vendedor(), l.detalle().cantidad(), l.detalle().precioUnitario(),
                    l.detalle().subtotal()));
        }
        filas.sort(Comparator.comparingInt(FilaVenta::getIdVenta));
        return filas;
    }

    // ------------------------------------------------------------------ popularidad de artistas

    public static List<FilaArtista> artistas(Fuente f, Filtros fl) {
        record Acum(Artista artista, Set<Integer> conciertos, int[] boletos, BigDecimal[] recaudado) {}
        Map<Integer, Acum> porArtista = new LinkedHashMap<>();

        // Todos los artistas con conciertos en agenda (aunque aún no hayan vendido).
        for (Concierto c : f.catalogo().conciertos()) {
            if (fl.idConcierto() != null && c.id() != fl.idConcierto()) continue;
            if (fl.idArtista() != null && c.idArtista() != fl.idArtista()) continue;
            Artista a = f.catalogo().artistas().get(c.idArtista());
            if (a == null) continue;
            Acum acum = porArtista.computeIfAbsent(a.id(),
                    k -> new Acum(a, new HashSet<>(), new int[1], new BigDecimal[]{BigDecimal.ZERO}));
            acum.conciertos().add(c.id());
        }
        for (Linea l : lineas(f, fl)) {
            if (l.artista() == null) continue;
            Acum acum = porArtista.get(l.artista().id());
            if (acum == null) continue;
            acum.boletos()[0] += l.detalle().cantidad();
            acum.recaudado()[0] = acum.recaudado()[0].add(l.detalle().subtotal());
        }

        List<Acum> orden = new ArrayList<>(porArtista.values());
        orden.sort(Comparator.<Acum>comparingInt(a -> -a.boletos()[0])
                .thenComparing(a -> a.recaudado()[0], Comparator.reverseOrder())
                .thenComparing(a -> a.artista().nombre(), String.CASE_INSENSITIVE_ORDER));
        List<FilaArtista> filas = new ArrayList<>();
        int pos = 1;
        for (Acum a : orden) {
            filas.add(new FilaArtista(pos++, a.artista().nombre(), a.artista().genero(), a.conciertos().size(),
                    a.boletos()[0], a.recaudado()[0]));
        }
        return filas;
    }

    // ------------------------------------------------------------------ inventario y recaudación

    public static List<FilaInventario> inventario(Fuente f, Filtros fl) {
        Map<Integer, Concierto> conciertos = new HashMap<>();
        for (Concierto c : f.catalogo().conciertos()) conciertos.put(c.id(), c);

        Map<Integer, Integer> vendidosPorInventario = new HashMap<>();
        Map<Integer, BigDecimal> ingresosPorInventario = new HashMap<>();
        for (Venta venta : f.ventas()) {
            for (DetalleVenta detalle : venta.detalles()) {
                vendidosPorInventario.merge(detalle.idInventario(), detalle.cantidad(), Integer::sum);
                ingresosPorInventario.merge(detalle.idInventario(), detalle.subtotal(), BigDecimal::add);
            }
        }
        List<FilaInventario> filas = new ArrayList<>();
        for (Inventario i : f.inventarios()) {
            Concierto c = conciertos.get(i.idConcierto());
            if (c == null) continue;
            if (fl.idConcierto() != null && c.id() != fl.idConcierto()) continue;
            if (fl.idArtista() != null && c.idArtista() != fl.idArtista()) continue;
            int vendidos = vendidosPorInventario.getOrDefault(i.id(), 0);
            String ocupacion = i.total() == 0 ? "—" : String.format(Locale.ROOT, "%.1f %%", (i.total() - i.disponible()) * 100.0 / i.total());
            filas.add(new FilaInventario(c.titulo(), f.catalogo().nombreArtista(c.idArtista()),
                    f.catalogo().nombreLocalidad(i.idLocalidad()), i.precio(), i.total(), vendidos, i.disponible(),
                    ocupacion, ingresosPorInventario.getOrDefault(i.id(), BigDecimal.ZERO)));
        }
        filas.sort(Comparator.comparing(FilaInventario::getConcierto, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(FilaInventario::getLocalidad, String.CASE_INSENSITIVE_ORDER));
        return filas;
    }
}
