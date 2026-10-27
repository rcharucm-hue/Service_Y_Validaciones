package com.svbgua.reportes;
import java.math.BigDecimal;
import java.util.*;
import com.svbgua.reportes.ReporteService.Tipo;
/** Agrupa antes de graficar: una categoria repetida suma, no reemplaza el valor previo. */
public final class ReporteGraficas {
    private ReporteGraficas() {}
    public static List<FilaGrafica> datos(Tipo tipo, Collection<?> filas, boolean recaudacion) {
        Map<String, BigDecimal> principales=new HashMap<>(), disponibles=new HashMap<>();
        for(Object fila:filas) {
            if(tipo==Tipo.VENTAS) {
                FilaVenta v=(FilaVenta)fila; principales.merge(v.getConcierto(),v.getSubtotal(),BigDecimal::add);
            } else if(tipo==Tipo.ARTISTAS) {
                FilaArtista a=(FilaArtista)fila; principales.merge(a.getArtista(),BigDecimal.valueOf(a.getBoletos()),BigDecimal::add);
            } else {
                FilaInventario i=(FilaInventario)fila;
                principales.merge(i.getConcierto(),recaudacion?i.getRecaudado():BigDecimal.valueOf(i.getVendidos()),BigDecimal::add);
                disponibles.merge(i.getConcierto(),BigDecimal.valueOf(i.getDisponibles()),BigDecimal::add);
            }
        }
        List<String> orden=new ArrayList<>(principales.keySet());
        orden.sort(Comparator.<String,BigDecimal>comparing(principales::get).reversed().thenComparing(Comparator.naturalOrder()));
        List<FilaGrafica> datos=new ArrayList<>();
        for(String nombre:orden.subList(0,Math.min(10,orden.size()))) {
            String serie=tipo==Tipo.ARTISTAS?"Boletos vendidos":tipo==Tipo.INVENTARIO&&!recaudacion?"Vendidos":"Recaudacion Q";
            datos.add(new FilaGrafica(serie,nombre,principales.get(nombre)));
            if(tipo==Tipo.INVENTARIO&&!recaudacion) datos.add(new FilaGrafica("Disponibles",nombre,disponibles.get(nombre)));
        }
        return datos;
    }
    public static String titulo(Tipo tipo, boolean recaudacion) {
        return (tipo==Tipo.ARTISTAS?"Popularidad: boletos vendidos por artista":tipo==Tipo.INVENTARIO&&!recaudacion
            ?"Inventario: vendidos y disponibles por concierto":"Recaudacion confirmada por concierto")+" (10 principales)";
    }
}
