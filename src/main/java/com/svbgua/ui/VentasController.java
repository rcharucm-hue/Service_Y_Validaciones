package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.*;
import com.svbgua.util.Fechas;
import com.svbgua.util.Fmt;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Historial de ventas (ventas-tab.fxml) con filtros por artista y por fecha.
 * El vendedor ve las suyas; el administrador, todas.
 */
public class VentasController {

    private static final Artista TODOS = new Artista(0, "Todos los artistas", "", "");

    private final ApiClient api = ApiClient.INSTANCE;
    private Catalogo catalogo = Catalogo.vacio();
    private Map<Integer, Inventario> inventariosPorId = new HashMap<>();
    private Map<Integer, String> nombresUsuarios = new HashMap<>();
    private final ObservableList<Venta> ventas = FXCollections.observableArrayList();
    private final FilteredList<Venta> filtradas = new FilteredList<>(ventas, v -> true);

    @FXML private TableView<Venta> tabla;
    @FXML private TableColumn<Venta, String> cId;
    @FXML private TableColumn<Venta, String> cFecha;
    @FXML private TableColumn<Venta, String> cVendedor;
    @FXML private TableColumn<Venta, String> cBoletos;
    @FXML private TableColumn<Venta, String> cTotal;
    @FXML private ListView<String> detalle;
    @FXML private Label lblResumen;
    @FXML private ComboBox<Artista> cbArtista;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    @FXML
    private void initialize() {
        cId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        cFecha.setCellValueFactory(c -> new SimpleStringProperty(Fmt.fecha(c.getValue().fecha())));
        cVendedor.setCellValueFactory(c -> new SimpleStringProperty(vendedorDe(c.getValue())));
        cBoletos.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(
                c.getValue().detalles().stream().mapToInt(DetalleVenta::cantidad).sum())));
        cTotal.setCellValueFactory(c -> new SimpleStringProperty(Fmt.money(c.getValue().total())));
        tabla.setItems(filtradas);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> mostrarDetalle(n));

        cbArtista.setConverter(new StringConverter<>() {
            @Override
            public String toString(Artista a) {
                return a == null ? "" : a.nombre();
            }

            @Override
            public Artista fromString(String s) {
                return null;
            }
        });
        cbArtista.getItems().setAll(TODOS);
        cbArtista.setValue(TODOS);
        cbArtista.valueProperty().addListener((o, a, n) -> aplicarFiltros());
        dpDesde.valueProperty().addListener((o, a, n) -> aplicarFiltros());
        dpHasta.valueProperty().addListener((o, a, n) -> aplicarFiltros());
    }

    public void setCatalogo(Catalogo nuevo) {
        this.catalogo = nuevo;
        Artista actual = cbArtista.getValue();
        cbArtista.getItems().setAll(TODOS);
        nuevo.artistas().values().stream().sorted(Comparator.comparing(Artista::nombre, String.CASE_INSENSITIVE_ORDER))
                .forEach(cbArtista.getItems()::add);
        cbArtista.setValue(actual != null && cbArtista.getItems().contains(actual) ? actual : TODOS);
        aplicarFiltros();
        mostrarDetalle(tabla.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void cargar() {
        record Datos(List<Venta> ventas, List<Inventario> inventarios, List<Usuario> usuarios) {}
        Runnable fin = Progreso.cargandoTabla(tabla);
        Async.run(() -> {
            // los nombres de los vendedores solo los puede consultar un administrador
            List<Usuario> usuarios = api.usuario() != null && api.usuario().esAdmin() ? api.listarUsuarios() : List.of();
            return new Datos(api.listarVentas(), api.listarInventario(null), usuarios);
        }, d -> {
            fin.run();
            inventariosPorId = new HashMap<>();
            for (Inventario i : d.inventarios()) inventariosPorId.put(i.id(), i);
            nombresUsuarios = new HashMap<>();
            for (Usuario u : d.usuarios()) nombresUsuarios.put(u.id(), u.nombreCompleto());
            ventas.setAll(d.ventas());
            aplicarFiltros();
        }, ex -> {
            fin.run();
            Dialogs.error("No se pudo cargar el historial", ex);
        });
    }

    @FXML
    private void limpiarFiltros() {
        cbArtista.setValue(TODOS);
        dpDesde.setValue(null);
        dpHasta.setValue(null);
    }

    private void aplicarFiltros() {
        Artista artista = cbArtista.getValue();
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        filtradas.setPredicate(v -> coincide(v, artista, desde, hasta));

        BigDecimal suma = filtradas.stream().map(Venta::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean filtrando = artista != TODOS && artista != null || desde != null || hasta != null;
        lblResumen.setText(filtradas.size() + (filtrando ? " de " + ventas.size() : "") + " ventas · " + Fmt.money(suma));
    }

    private boolean coincide(Venta v, Artista artista, LocalDate desde, LocalDate hasta) {
        if (desde != null || hasta != null) {
            LocalDate f = Fechas.local(v.fecha());
            if (f == null || desde != null && f.isBefore(desde) || hasta != null && f.isAfter(hasta)) {
                return false;
            }
        }
        if (artista != null && artista != TODOS) {
            for (DetalleVenta d : v.detalles()) {
                Inventario inv = inventariosPorId.get(d.idInventario());
                if (inv != null && catalogo.conciertos().stream()
                        .anyMatch(c -> c.id() == inv.idConcierto() && c.idArtista() == artista.id())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private String vendedorDe(Venta v) {
        if (v.vendedor() != null) return v.vendedor();
        if (v.idUsuario() != null) return nombresUsuarios.getOrDefault(v.idUsuario(), "Usuario #" + v.idUsuario());
        return "—";
    }

    private void mostrarDetalle(Venta v) {
        if (v == null) {
            detalle.getItems().clear();
            return;
        }
        ObservableList<String> lineas = FXCollections.observableArrayList();
        for (DetalleVenta d : v.detalles()) {
            Inventario inv = inventariosPorId.get(d.idInventario());
            String seccion = inv == null ? "Inventario " + d.idInventario() : catalogo.nombreLocalidad(inv.idLocalidad());
            String evento = inv == null ? "" : catalogo.tituloConcierto(inv.idConcierto()) + " · ";
            lineas.add(evento + seccion + "  —  " + d.cantidad() + " x " + Fmt.money(d.precioUnitario())
                    + " = " + Fmt.money(d.subtotal()));
        }
        lineas.add("TOTAL: " + Fmt.money(v.total()));
        detalle.setItems(lineas);
    }
}
