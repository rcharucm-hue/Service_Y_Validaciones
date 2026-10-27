package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.*;
import com.svbgua.util.Fmt;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** CRUD de inventario de boletos (inventario-tab.fxml, solo administrador): precio y cantidades por concierto y localidad. */
public class InventarioAdminController {

    private final ApiClient api = ApiClient.INSTANCE;
    private Runnable recargarCatalogo = () -> { };
    private Catalogo catalogo = Catalogo.vacio();
    private final ObservableList<Inventario> datos = FXCollections.observableArrayList();
    private final FilteredList<Inventario> filtrados = new FilteredList<>(datos);

    /** Opción "sin filtro" del combo de filtrado (id 0 no existe en la base de datos). */
    private static final Concierto TODOS = new Concierto(0, 0, "Todos los conciertos", "", "", "");

    @FXML private TableView<Inventario> tabla;
    @FXML private TableColumn<Inventario, String> cId;
    @FXML private TableColumn<Inventario, String> cConcierto;
    @FXML private TableColumn<Inventario, String> cLocalidad;
    @FXML private TableColumn<Inventario, String> cPrecio;
    @FXML private TableColumn<Inventario, String> cTotal;
    @FXML private TableColumn<Inventario, String> cDisp;
    @FXML private ComboBox<Concierto> cbFiltro;
    @FXML private ComboBox<Concierto> cbConcierto;
    @FXML private ComboBox<Localidad> cbLocalidad;
    @FXML private TextField tfPrecio;
    @FXML private TextField tfTotal;
    @FXML private TextField tfDisponible;
    @FXML private Label lblModo;

    private record Formulario(Concierto concierto, Localidad localidad, BigDecimal precio, int total, int disponible) {}

    public void setRecargarCatalogo(Runnable recargarCatalogo) {
        this.recargarCatalogo = recargarCatalogo;
    }

    @FXML
    private void initialize() {
        cId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        cConcierto.setCellValueFactory(c -> new SimpleStringProperty(catalogo.tituloConcierto(c.getValue().idConcierto())));
        cLocalidad.setCellValueFactory(c -> new SimpleStringProperty(catalogo.nombreLocalidad(c.getValue().idLocalidad())));
        cPrecio.setCellValueFactory(c -> new SimpleStringProperty(Fmt.money(c.getValue().precio())));
        cTotal.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().total())));
        cDisp.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().disponible())));
        tabla.setItems(filtrados);

        cbFiltro.setCellFactory(lv -> celdaFiltro());
        cbFiltro.setButtonCell(celdaFiltro());
        cbFiltro.setItems(FXCollections.observableArrayList(TODOS));
        cbFiltro.setValue(TODOS);
        cbFiltro.valueProperty().addListener((o, a, n) -> aplicarFiltro(n));
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> llenarFormulario(n));
        Campos.soloDecimales(tfPrecio, 7, 2);
        Campos.soloEnteros(tfTotal, 6);
        Campos.soloEnteros(tfDisponible, 6);
    }

    public void setCatalogo(Catalogo nuevo) {
        this.catalogo = nuevo;
        cbConcierto.setItems(FXCollections.observableArrayList(nuevo.conciertos()));
        cbLocalidad.setItems(FXCollections.observableArrayList(nuevo.localidades().values()));

        // Opciones del filtro: "Todos" + conciertos; se conserva el filtro elegido si el concierto sigue existiendo.
        int idFiltro = cbFiltro.getValue() == null ? 0 : cbFiltro.getValue().id();
        List<Concierto> opciones = new ArrayList<>();
        opciones.add(TODOS);
        opciones.addAll(nuevo.conciertos());
        cbFiltro.setItems(FXCollections.observableArrayList(opciones));
        cbFiltro.setValue(opciones.stream().filter(c -> c.id() == idFiltro).findFirst().orElse(TODOS));

        tabla.refresh();
        cargar();
    }

    @FXML
    public void cargar() {
        Runnable fin = Progreso.cargandoTabla(tabla);
        Async.run(() -> api.listarInventario(null), lista -> {
            fin.run();
            datos.setAll(lista);
            tabla.refresh();
        }, ex -> {
            fin.run();
            Dialogs.error("No se pudo cargar el inventario", ex);
        });
    }

    // --------------------------------------------------------------- filtro

    private ListCell<Concierto> celdaFiltro() {
        return new ListCell<>() {
            @Override
            protected void updateItem(Concierto c, boolean vacio) {
                super.updateItem(c, vacio);
                setText(vacio || c == null ? null : (c.id() == 0 ? "Todos los conciertos" : c.toString()));
            }
        };
    }

    /** Muestra solo el inventario del concierto elegido (o todo si se elige "Todos los conciertos"). */
    private void aplicarFiltro(Concierto elegido) {
        boolean todos = elegido == null || elegido.id() == 0;
        filtrados.setPredicate(inv -> todos || inv.idConcierto() == elegido.id());
        limpiarFormulario();
        if (!todos) {
            // Comodidad: el formulario de la derecha ya queda con ese concierto para agregar secciones.
            cbConcierto.getItems().stream().filter(x -> x.id() == elegido.id()).findFirst().ifPresent(cbConcierto::setValue);
        }
    }

    // ----------------------------------------------------------------- CRUD

    private void llenarFormulario(Inventario inv) {
        if (inv == null) return;
        lblModo.setText("Editando registro #" + inv.id());
        cbConcierto.getItems().stream().filter(c -> c.id() == inv.idConcierto()).findFirst().ifPresent(cbConcierto::setValue);
        cbLocalidad.getItems().stream().filter(l -> l.id() == inv.idLocalidad()).findFirst().ifPresent(cbLocalidad::setValue);
        tfPrecio.setText(inv.precio().toPlainString());
        tfTotal.setText(String.valueOf(inv.total()));
        tfDisponible.setText(String.valueOf(inv.disponible()));
    }

    @FXML
    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        lblModo.setText("Nuevo registro");
        cbConcierto.setValue(null);
        cbLocalidad.setValue(null);
        tfPrecio.clear();
        tfTotal.clear();
        tfDisponible.clear();
    }

    private Formulario leerFormulario() {
        Concierto c = cbConcierto.getValue();
        Localidad l = cbLocalidad.getValue();
        if (c == null || l == null) {
            Dialogs.aviso("Selecciona el concierto y la sección.");
            return null;
        }
        try {
            BigDecimal precio = new BigDecimal(tfPrecio.getText().trim());
            int total = Integer.parseInt(tfTotal.getText().trim());
            int disp = Integer.parseInt(tfDisponible.getText().trim());
            if (precio.signum() <= 0 || precio.scale() > 2) {
                Dialogs.aviso("El precio debe ser mayor que cero y tener hasta 2 decimales.");
                return null;
            }
            if (total < 1 || disp < 0 || disp > total) {
                Dialogs.aviso("Total debe ser al menos 1 y disponible no puede ser mayor que el total.");
                return null;
            }
            return new Formulario(c, l, precio, total, disp);
        } catch (NumberFormatException e) {
            Dialogs.aviso("Revisa que precio y cantidades sean números válidos.");
            return null;
        }
    }

    @FXML
    private void guardar() {
        Formulario f = leerFormulario();
        if (f == null) return;
        Inventario editado = tabla.getSelectionModel().getSelectedItem();
        Async.run(() -> editado == null
                        ? api.crearInventario(f.concierto().id(), f.localidad().id(), f.precio(), f.total(), f.disponible())
                        : api.actualizarInventario(editado.id(), f.concierto().id(), f.localidad().id(), f.precio(), f.total(), f.disponible()),
                r -> {
                    limpiarFormulario();
                    cargar();
                },
                ex -> Dialogs.error("No se pudo guardar", ex));
    }

    @FXML
    private void eliminar() {
        Inventario sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.aviso("Selecciona un registro de la tabla.");
            return;
        }
        if (!Dialogs.confirmar("¿Eliminar el registro #" + sel.id() + "?")) return;
        Async.run(() -> {
            api.eliminarInventario(sel.id());
            return Boolean.TRUE;
        }, ok -> {
            limpiarFormulario();
            cargar();
        }, ex -> Dialogs.error("No se pudo eliminar", ex));
    }

    @FXML
    private void crearLocalidad() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Nueva localidad");
        d.setHeaderText("Ej.: Preferencial, VIP de pie, Mesa");
        d.setContentText("Nombre:");
        d.showAndWait().ifPresent(nombre -> {
            if (nombre.isBlank()) return;
            Async.run(() -> api.crearLocalidad(nombre), l -> recargarCatalogo.run(),
                    ex -> Dialogs.error("No se pudo crear la localidad", ex));
        });
    }
}
