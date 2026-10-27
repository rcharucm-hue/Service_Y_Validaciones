package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.*;
import com.svbgua.reportes.*;
import com.svbgua.reportes.ReporteDatos.*;
import com.svbgua.reportes.ReporteService.Tipo;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/** Panel con graficas generadas por Jasper y PDF con tabla, totales y grafica. */
public class ReportesController {
    private static final Concierto TODOS_CONCIERTOS=new Concierto(0,0,"Todos los conciertos","","","");
    private static final Artista TODOS_ARTISTAS=new Artista(0,"Todos los artistas","","");
    private static final Usuario TODOS_VENDEDORES=new Usuario(0,"","Todos los vendedores","");
    private final ApiClient api=ApiClient.INSTANCE;
    private Catalogo catalogo=Catalogo.vacio();
    private Fuente fuente;
    private Vista vista;
    private boolean cargando;
    private int revision;
    @FXML private BorderPane raiz;
    @FXML private ComboBox<Tipo> cbTipo;
    @FXML private ComboBox<Concierto> cbConcierto;
    @FXML private ComboBox<Artista> cbArtista;
    @FXML private ComboBox<Usuario> cbVendedor;
    @FXML private DatePicker dpDesde,dpHasta;
    @FXML private Label lblNota,lblEstado,lblTotales,lblSinVentas,lblSinArtistas,lblSinInventario;
    @FXML private Button btnGenerar,btnActualizar,btnAplicar;
    @FXML private ImageView imgVentas,imgArtistas,imgInventario,imgRecaudacion;
    private record Vista(List<FilaVenta> ventas,List<FilaArtista> artistas,List<FilaInventario> inventario,
                         byte[] grafVentas,byte[] grafArtistas,byte[] grafInventario,byte[] grafRecaudacion,
                         Filtros filtros,String subtitulo,String subtituloInventario) {
        List<?> filas(Tipo tipo) { return switch(tipo) { case VENTAS->ventas; case ARTISTAS->artistas; case INVENTARIO->inventario; }; }
    }
    @FXML private void initialize() {
        cbTipo.getItems().setAll(Tipo.values()); cbTipo.setValue(Tipo.VENTAS);
        cbTipo.valueProperty().addListener((o,a,b)->ajustarFiltros());
        cbConcierto.setConverter(convertidor(c->c==TODOS_CONCIERTOS?c.titulo():c.toString()));
        cbArtista.setConverter(convertidor(Artista::nombre)); cbVendedor.setConverter(convertidor(Usuario::nombreCompleto));
        cbConcierto.getItems().setAll(TODOS_CONCIERTOS); cbArtista.getItems().setAll(TODOS_ARTISTAS);
        cbVendedor.getItems().setAll(TODOS_VENDEDORES);
        for(ImageView img:List.of(imgVentas,imgArtistas,imgInventario,imgRecaudacion)) {
            img.setPreserveRatio(true); img.fitWidthProperty().bind(raiz.widthProperty().subtract(90));
        }
        limpiarFiltros(); controles(false);
        lblEstado.setText("Abre esta pestana para cargar las graficas de Jasper.");
    }
    private static <T> StringConverter<T> convertidor(Function<T,String> texto) {
        return new StringConverter<>() { public String toString(T t) { return t==null?"":texto.apply(t); } public T fromString(String s) { return null; } };
    }
    public void setCatalogo(Catalogo nuevo) {
        catalogo=nuevo; Concierto c=cbConcierto.getValue(); Artista a=cbArtista.getValue();
        cbConcierto.getItems().setAll(TODOS_CONCIERTOS); cbConcierto.getItems().addAll(nuevo.conciertos());
        cbArtista.getItems().setAll(TODOS_ARTISTAS);
        nuevo.artistas().values().stream().sorted(Comparator.comparing(Artista::nombre,String.CASE_INSENSITIVE_ORDER)).forEach(cbArtista.getItems()::add);
        cbConcierto.setValue(c!=null&&cbConcierto.getItems().contains(c)?c:TODOS_CONCIERTOS);
        cbArtista.setValue(a!=null&&cbArtista.getItems().contains(a)?a:TODOS_ARTISTAS);
    }
    /** Se llama cada vez que se entra a Administracion > Reportes, sin escribir datos en la API. */
    @FXML public void actualizar() {
        if(cargando || api.usuario()==null || !api.usuario().esAdmin()) return;
        cargando=true; revision++; controles(false); btnActualizar.setDisable(true);
        lblEstado.setText("Cargando ventas, artistas e inventario...");
        Async.run(()->new Fuente(api.cargarCatalogo(),api.listarInventario(null),api.listarVentas(),api.listarUsuarios()),f->{
            cargando=false; btnActualizar.setDisable(false); fuente=f; setCatalogo(f.catalogo());
            Usuario anterior=cbVendedor.getValue(); cbVendedor.getItems().setAll(TODOS_VENDEDORES);
            f.usuarios().stream().sorted(Comparator.comparing(Usuario::nombreCompleto,String.CASE_INSENSITIVE_ORDER)).forEach(cbVendedor.getItems()::add);
            cbVendedor.setValue(anterior!=null&&cbVendedor.getItems().contains(anterior)?anterior:TODOS_VENDEDORES);
            aplicarFiltros();
        },ex->{cargando=false;btnActualizar.setDisable(false);controles(fuente!=null);
            lblEstado.setText("No se pudieron actualizar los reportes: "+Dialogs.mensaje(ex));});
    }
    private void controles(boolean listos) {
        btnGenerar.setDisable(!listos || vista==null); btnAplicar.setDisable(!listos);
    }
    private void ajustarFiltros() {
        boolean inventario=cbTipo.getValue()==Tipo.INVENTARIO;
        cbVendedor.setDisable(inventario); dpDesde.setDisable(inventario); dpHasta.setDisable(inventario);
        lblNota.setText(inventario?"Inventario: estado actual y recaudacion historica. Solo se filtra por concierto/artista.":
            "Aplica los filtros para actualizar las graficas y el PDF. Popularidad = boletos vendidos.");
    }
    @FXML private void limpiarFiltros() {
        cbConcierto.setValue(TODOS_CONCIERTOS);cbArtista.setValue(TODOS_ARTISTAS);cbVendedor.setValue(TODOS_VENDEDORES);
        dpDesde.setValue(null);dpHasta.setValue(null);ajustarFiltros();
        if(fuente!=null && !cargando) aplicarFiltros();
    }
    private Filtros filtros() {
        Concierto c=cbConcierto.getValue();Artista a=cbArtista.getValue();Usuario u=cbVendedor.getValue();
        return new Filtros(c==null||c==TODOS_CONCIERTOS?null:c.id(),a==null||a==TODOS_ARTISTAS?null:a.id(),
            u==null||u==TODOS_VENDEDORES?null:u.id(),dpDesde.getValue(),dpHasta.getValue());
    }
    private String describir(Filtros f) {
        List<String> partes=new ArrayList<>();DateTimeFormatter fmt=DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if(f.idConcierto()!=null)partes.add("Concierto: "+catalogo.tituloConcierto(f.idConcierto()));
        if(f.idArtista()!=null)partes.add("Artista: "+catalogo.nombreArtista(f.idArtista()));
        if(f.idVendedor()!=null)partes.add("Vendedor: "+cbVendedor.getValue().nombreCompleto());
        if(f.desde()!=null)partes.add("Desde: "+f.desde().format(fmt));if(f.hasta()!=null)partes.add("Hasta: "+f.hasta().format(fmt));
        return partes.isEmpty()?"Todos los datos":String.join(" | ",partes);
    }
    @FXML private void aplicarFiltros() {
        if(fuente==null || cargando)return;
        Filtros f=filtros();
        if(f.desde()!=null&&f.hasta()!=null&&f.desde().isAfter(f.hasta())) {
            lblEstado.setText("Desde no puede ser posterior a Hasta.");return;
        }
        if(f.idVendedor()!=null&&!fuente.ventas().isEmpty()&&!ReporteDatos.hayDatoDeVendedor(fuente.ventas())) {
            lblEstado.setText("La API no informa el vendedor; no se puede aplicar ese filtro.");return;
        }
        Filtros fi=new Filtros(f.idConcierto(),f.idArtista(),null,null,null);
        String subtitulo=describir(f),subInventario=describir(fi); Fuente origen=fuente; int actual=++revision;
        controles(false); lblEstado.setText("Generando graficas con JasperReports...");
        Async.run(()->{
            var ventas=ReporteDatos.ventas(origen,f);var artistas=ReporteDatos.artistas(origen,f);var inventario=ReporteDatos.inventario(origen,fi);
            return new Vista(ventas,artistas,inventario,
                ventas.isEmpty()?null:ReporteService.grafica(Tipo.VENTAS,ventas,false),
                artistas.isEmpty()?null:ReporteService.grafica(Tipo.ARTISTAS,artistas,false),
                inventario.isEmpty()?null:ReporteService.grafica(Tipo.INVENTARIO,inventario,false),
                inventario.isEmpty()?null:ReporteService.grafica(Tipo.INVENTARIO,inventario,true),f,subtitulo,subInventario);
        },v->{if(actual!=revision)return;vista=v;
            imagen(imgVentas,v.grafVentas());imagen(imgArtistas,v.grafArtistas());imagen(imgInventario,v.grafInventario());imagen(imgRecaudacion,v.grafRecaudacion());
            visible(lblSinVentas,v.ventas().isEmpty());visible(lblSinArtistas,v.artistas().isEmpty());visible(lblSinInventario,v.inventario().isEmpty());
            long ventas=v.ventas().stream().map(FilaVenta::getIdVenta).distinct().count();
            var total=v.ventas().stream().map(FilaVenta::getSubtotal).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
            lblTotales.setText(ventas+" ventas | "+com.svbgua.util.Fmt.money(total)+" recaudados | "+v.artistas().size()+" artistas");
            lblEstado.setText("Graficas actualizadas. "+v.subtitulo()+". Inventario: "+v.subtituloInventario()+".");controles(true);
        },ex->{if(actual!=revision)return;controles(vista!=null);lblEstado.setText("No se pudieron generar las graficas: "+Dialogs.mensaje(ex));});
    }
    private static void visible(javafx.scene.Node nodo,boolean valor){nodo.setVisible(valor);nodo.setManaged(valor);}
    private static void imagen(ImageView vista,byte[] png){vista.setImage(png==null?null:new Image(new ByteArrayInputStream(png)));visible(vista,png!=null);}
    @FXML private void generar() {
        if(vista==null || btnGenerar.isDisabled())return;
        Tipo tipo=cbTipo.getValue();Vista datos=vista;
        FileChooser fc=new FileChooser();fc.setTitle("Guardar reporte con grafica");
        fc.setInitialFileName(tipo.plantilla()+"-"+LocalDate.now()+".pdf");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF","*.pdf"));
        File destino=fc.showSaveDialog(raiz.getScene().getWindow());if(destino==null)return;
        String usuario=api.usuario().nombreCompleto();btnGenerar.setDisable(true);lblEstado.setText("Generando PDF con tabla y grafica...");
        Async.run(()->ReporteService.generar(tipo,datos.filas(tipo),tipo==Tipo.INVENTARIO?datos.subtituloInventario():datos.subtitulo(),usuario,destino.toPath()),ruta->{
            controles(true);lblEstado.setText("Reporte guardado: "+ruta);abrir(ruta);
        },ex->{controles(true);lblEstado.setText("Error al exportar: "+Dialogs.mensaje(ex));});
    }
    private static void abrir(Path ruta){try{if(java.awt.Desktop.isDesktopSupported())java.awt.Desktop.getDesktop().open(ruta.toFile());}catch(Exception ignorada){}}
}
