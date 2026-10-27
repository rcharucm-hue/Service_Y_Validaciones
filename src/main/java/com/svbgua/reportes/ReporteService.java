package com.svbgua.reportes;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JREmptyDataSource;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Genera los reportes en PDF con JasperReports a partir de las plantillas de /com/svbgua/reportes. */
public final class ReporteService {
    private ReporteService() {}

    public enum Tipo {
        VENTAS("Detalle de ventas", "ventas-detalle"),
        ARTISTAS("Popularidad de artistas", "artistas-popularidad"),
        INVENTARIO("Inventario y recaudación por concierto", "inventario-concierto");

        private final String titulo;
        private final String plantilla;

        Tipo(String titulo, String plantilla) {
            this.titulo = titulo;
            this.plantilla = plantilla;
        }

        public String titulo() {
            return titulo;
        }

        public String plantilla() {
            return plantilla;
        }

        @Override
        public String toString() {
            return titulo;
        }
    }

    private static final Map<Tipo, JasperReport> COMPILADOS = new EnumMap<>(Tipo.class);

    /** Compila el .jrxml la primera vez y reutiliza el resultado (compilar tarda cerca de un segundo). */
    private static synchronized JasperReport compilado(Tipo tipo) throws JRException, IOException {
        JasperReport r = COMPILADOS.get(tipo);
        if (r == null) {
            String ruta = "/com/svbgua/reportes/" + tipo.plantilla() + ".jrxml";
            try (InputStream in = ReporteService.class.getResourceAsStream(ruta)) {
                if (in == null) {
                    throw new IOException("No se encontró la plantilla " + ruta);
                }
                r = JasperCompileManager.compileReport(in);
            }
            COMPILADOS.put(tipo, r);
        }
        return r;
    }

    private static JasperReport plantillaGrafica;
    /** Renderiza una grafica Jasper como PNG para mostrarla dentro de JavaFX. */
    public static byte[] grafica(Tipo tipo, Collection<?> filas, boolean recaudacion) {
        try {
            JasperReport plantilla;
            synchronized (ReporteService.class) {
                if(plantillaGrafica==null) {
                    try(InputStream in=ReporteService.class.getResourceAsStream("/com/svbgua/reportes/grafica-resumen.jrxml")) {
                        if(in==null) throw new IOException("No se encontro la plantilla de graficas.");
                        plantillaGrafica=JasperCompileManager.compileReport(in);
                    }
                }
                plantilla=plantillaGrafica;
            }
            Map<String,Object> params=new HashMap<>();
            params.put("GRAFICA",ReporteGraficas.datos(tipo,filas,recaudacion));
            params.put("TITULO_GRAFICA",ReporteGraficas.titulo(tipo,recaudacion));
            JasperPrint print=JasperFillManager.fillReport(plantilla,params,new JREmptyDataSource(1));
            BufferedImage imagen=(BufferedImage)JasperPrintManager.printPageToImage(print,0,1.25f);
            ByteArrayOutputStream salida=new ByteArrayOutputStream();
            ImageIO.write(imagen,"png",salida);
            return salida.toByteArray();
        } catch(JRException|IOException e) {
            throw new IllegalStateException("No se pudo generar la grafica Jasper: "+e.getMessage(),e);
        }
    }

    /**
     * Genera el PDF. Bloqueante: llamarlo fuera del hilo de la interfaz.
     *
     * @param filas     JavaBeans del reporte (FilaVenta, FilaArtista o FilaInventario)
     * @param subtitulo texto con los filtros aplicados
     */
    public static Path generar(Tipo tipo, Collection<?> filas, String subtitulo, String usuario, Path destino) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("TITULO", tipo.titulo());
            params.put("SUBTITULO", subtitulo);
            params.put("USUARIO", usuario);
            params.put("GENERADO", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            params.put("REPORT_LOCALE", Locale.forLanguageTag("es-GT"));
            params.put("GRAFICA", ReporteGraficas.datos(tipo, filas, false));
            params.put("GRAFICA_RECAUDACION", ReporteGraficas.datos(tipo, filas, true));
            params.put("TITULO_GRAFICA", ReporteGraficas.titulo(tipo, false));

            JasperPrint lleno = JasperFillManager.fillReport(compilado(tipo), params, new JRBeanCollectionDataSource(filas));
            JasperExportManager.exportReportToPdfFile(lleno, destino.toString());
            return destino;
        } catch (JRException | IOException e) {
            throw new IllegalStateException("No se pudo generar el reporte: " + e.getMessage(), e);
        }
    }
}
