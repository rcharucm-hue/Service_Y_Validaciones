package com.svbgua.reportes;
import com.svbgua.model.Modelos.*;
import com.svbgua.reportes.ReporteDatos.*;
import com.svbgua.reportes.ReporteService.Tipo;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
/** Prueba de integridad y renderizado Jasper sin API, correo ni base de datos. */
public class ReportesPrueba {
    private static int pruebas;
    private static void igual(boolean ok,String mensaje){if(!ok)throw new AssertionError(mensaje);pruebas++;}
    public static void main(String[] args)throws Exception {
        Path destino=Path.of(args.length==0?"target/pruebas-reportes":args[0]);Files.createDirectories(destino);
        var conciertos=List.of(new Concierto(1,1,"Noche de jazz","2026-11-20T20:00:00-06:00","Teatro","activo"),
            new Concierto(2,2,"Festival de rock","2026-12-20T20:00:00-06:00","Estadio","programado"));
        var catalogo=new Catalogo(conciertos,Map.of(1,new Localidad(1,"General"),2,new Localidad(2,"VIP")),
            Map.of(1,new Artista(1,"Artista Jazz","Jazz","Guatemala"),2,new Artista(2,"Artista Rock","Rock","Guatemala")));
        var inventario=List.of(new Inventario(1,1,1,new BigDecimal("200"),100,95),
            new Inventario(2,2,2,new BigDecimal("150"),100,97));
        var ventas=List.of(new Venta(1,"2026-10-25T12:00:00-06:00",new BigDecimal("500"),
            List.of(new DetalleVenta(1,5,new BigDecimal("100"),new BigDecimal("500"))),7,"Ana"),
            new Venta(2,"2026-10-26T12:00:00-06:00",new BigDecimal("450"),
            List.of(new DetalleVenta(2,3,new BigDecimal("150"),new BigDecimal("450"))),7,"Ana"));
        var fuente=new Fuente(catalogo,inventario,ventas,List.of(new Usuario(7,"ana","Ana","vendedor")));
        var f=Filtros.ninguno();var vf=ReporteDatos.ventas(fuente,f);var af=ReporteDatos.artistas(fuente,f);var inf=ReporteDatos.inventario(fuente,f);
        igual(vf.size()==2,"Dos detalles");igual(af.get(0).getBoletos()==5,"Ranking por boletos");
        igual(inf.get(0).getRecaudado().compareTo(new BigDecimal("450"))==0 || inf.get(1).getRecaudado().compareTo(new BigDecimal("450"))==0,"Ingresos rock");
        var jazz=inf.stream().filter(i->i.getConcierto().equals("Noche de jazz")).findFirst().orElseThrow();
        igual(jazz.getRecaudado().compareTo(new BigDecimal("500"))==0,"No revalorizar ventas con precio actual");
        igual(jazz.getVendidos()==5,"Boletos confirmados");
        var sinVentas=ReporteDatos.inventario(new Fuente(catalogo,inventario,List.of(),List.of()),f);
        igual(sinVentas.stream().allMatch(i->i.getRecaudado().signum()==0 && i.getVendidos()==0),"Ajuste de stock no se cuenta como venta");
        igual(ReporteDatos.ventas(fuente,new Filtros(1,null,null,null,null)).size()==1,"Filtro concierto");
        var repetidas=List.of(vf.get(0),vf.get(0));
        igual(ReporteGraficas.datos(Tipo.VENTAS,repetidas,false).get(0).getValor().compareTo(vf.get(0).getSubtotal().multiply(new BigDecimal("2")))==0,"Categorias repetidas se suman");
        for(Tipo tipo:Tipo.values()) {
            List<?> filas=switch(tipo){case VENTAS->vf;case ARTISTAS->af;case INVENTARIO->inf;};
            byte[] png=ReporteService.grafica(tipo,filas,false);Files.write(destino.resolve(tipo.plantilla()+".png"),png);
            igual(png.length>1000,"Grafica Jasper no vacia: "+tipo);
            Path pdf=ReporteService.generar(tipo,filas,"Datos ficticios de verificacion","Prueba local",destino.resolve(tipo.plantilla()+".pdf"));
            igual(Files.size(pdf)>1000,"PDF Jasper: "+tipo);
            Path vacio=ReporteService.generar(tipo,List.of(),"Sin datos","Prueba local",destino.resolve(tipo.plantilla()+"-vacio.pdf"));
            igual(Files.size(vacio)>1000,"PDF sin datos: "+tipo);
        }
        Files.write(destino.resolve("recaudacion.png"),ReporteService.grafica(Tipo.INVENTARIO,inf,true));
        igual(ReporteService.grafica(Tipo.ARTISTAS,List.of(),false).length>0,"Grafica sin categorias no falla");
        System.out.println(pruebas+" comprobaciones de reportes correctas.");
    }
}
