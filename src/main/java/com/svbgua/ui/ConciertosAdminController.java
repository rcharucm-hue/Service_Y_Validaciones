package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.*;
import com.svbgua.util.Fmt;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** CRUD de conciertos (conciertos-tab.fxml, solo administrador) con opcion para crear artistas. */
public class ConciertosAdminController {

    private static final ZoneId GT = ZoneId.of("America/Guatemala");
    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private final ApiClient api = ApiClient.INSTANCE;
    private Runnable recargarCatalogo = () -> { };
    private Catalogo catalogo = Catalogo.vacio();
    private final ObservableList<Concierto> datos = FXCollections.observableArrayList();

    @FXML private TableView<Concierto> tabla;
    @FXML private TableColumn<Concierto, String> cId;
    @FXML private TableColumn<Concierto, String> cTitulo;
    @FXML private TableColumn<Concierto, String> cArtista;
    @FXML private TableColumn<Concierto, String> cFecha;
    @FXML private TableColumn<Concierto, String> cRecinto;
    @FXML private TableColumn<Concierto, String> cEstado;
    @FXML private ComboBox<Artista> cbArtista;
    @FXML private TextField tfTitulo;
    @FXML private DatePicker dpFecha;
    @FXML private Spinner<Integer> spHora;
    @FXML private Spinner<Integer> spMinuto;
    @FXML private TextField tfRecinto;
    @FXML private ComboBox<String> cbEstado;
    @FXML private Label lblModo;

    private record Formulario(Artista artista, String titulo, String fechaIso, String recinto, String estado) {}

    public void setRecargarCatalogo(Runnable recargarCatalogo) {
        this.recargarCatalogo = recargarCatalogo;
    }

    @FXML
    private void initialize() {
        cId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        cTitulo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().titulo()));
        cArtista.setCellValueFactory(c -> new SimpleStringProperty(catalogo.nombreArtista(c.getValue().idArtista())));
        cFecha.setCellValueFactory(c -> new SimpleStringProperty(Fmt.fecha(c.getValue().fecha())));
        cRecinto.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().recinto()));
        cEstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().estado()));
        tabla.setItems(datos);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> llenarFormulario(n));
        Campos.spinnerDosDigitos(spHora, 0, 23, 20, 1);
        Campos.spinnerDosDigitos(spMinuto, 0, 59, 0, 5);
    }

    @FXML
    private void actualizar() {
        recargarCatalogo.run();
    }

    public void setCatalogo(Catalogo nuevo) {
        this.catalogo = nuevo;
        Artista previo = cbArtista.getValue();
        cbArtista.setItems(FXCollections.observableArrayList(nuevo.artistas().values()));
        if (previo != null) cbArtista.getItems().stream().filter(a -> a.id() == previo.id()).findFirst().ifPresent(cbArtista::setValue);
        datos.setAll(nuevo.conciertos());
        tabla.refresh();
    }

    // ----------------------------------------------------------------- CRUD

    private void llenarFormulario(Concierto c) {
        if (c == null) return;
        lblModo.setText("Editando concierto #" + c.id());
        cbArtista.getItems().stream().filter(a -> a.id() == c.idArtista()).findFirst().ifPresent(cbArtista::setValue);
        tfTitulo.setText(c.titulo());
        tfRecinto.setText(c.recinto());
        cbEstado.setValue(c.estado());
        try {
            ZonedDateTime z = OffsetDateTime.parse(c.fecha()).atZoneSameInstant(GT);
            dpFecha.setValue(z.toLocalDate());
            spHora.getValueFactory().setValue(z.getHour());
            spMinuto.getValueFactory().setValue(z.getMinute());
        } catch (DateTimeParseException e) {
            dpFecha.setValue(null);
            spHora.getValueFactory().setValue(20);
            spMinuto.getValueFactory().setValue(0);
        }
    }

    @FXML
    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        lblModo.setText("Nuevo concierto");
        cbArtista.setValue(null);
        tfTitulo.clear();
        dpFecha.setValue(null);
        spHora.getValueFactory().setValue(20);
        spMinuto.getValueFactory().setValue(0);
        tfRecinto.clear();
        cbEstado.setValue("programado");
    }

    private Formulario leerFormulario() {
        Artista artista = cbArtista.getValue();
        if (artista == null) {
            Dialogs.aviso("Selecciona un artista (o crea uno con \"+ Artista\").");
            return null;
        }
        String titulo = tfTitulo.getText().trim();
        String recinto = tfRecinto.getText().trim();
        if (titulo.isEmpty() || titulo.length() > 150) {
            Dialogs.aviso("El título es obligatorio (máximo 150 caracteres).");
            return null;
        }
        if (recinto.isEmpty() || recinto.length() > 120) {
            Dialogs.aviso("El recinto es obligatorio (máximo 120 caracteres).");
            return null;
        }
        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) {
            Dialogs.aviso("Selecciona la fecha del concierto.");
            return null;
        }
        spHora.increment(0);   // confirma lo que se haya escrito
        spMinuto.increment(0);
        LocalTime hora = LocalTime.of(spHora.getValue(), spMinuto.getValue());
        String iso = LocalDateTime.of(fecha, hora).atZone(GT).format(ISO);
        return new Formulario(artista, titulo, iso, recinto, cbEstado.getValue());
    }

    @FXML
    private void guardar() {
        Formulario f = leerFormulario();
        if (f == null) return;
        Concierto editado = tabla.getSelectionModel().getSelectedItem();
        Async.run(() -> {
            if (editado == null) {
                api.crearConcierto(f.artista().id(), f.titulo(), f.fechaIso(), f.recinto(), f.estado());
            } else {
                api.actualizarConcierto(editado.id(), f.artista().id(), f.titulo(), f.fechaIso(), f.recinto(), f.estado());
            }
            return Boolean.TRUE;
        }, ok -> {
            limpiarFormulario();
            recargarCatalogo.run();
        }, ex -> Dialogs.error("No se pudo guardar el concierto", ex));
    }

    @FXML
    private void eliminar() {
        Concierto sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.aviso("Selecciona un concierto de la tabla.");
            return;
        }
        if (!Dialogs.confirmar("¿Eliminar el concierto \"" + sel.titulo() + "\"?")) return;
        Async.run(() -> {
            api.eliminarConcierto(sel.id());
            return Boolean.TRUE;
        }, ok -> {
            limpiarFormulario();
            recargarCatalogo.run();
        }, ex -> Dialogs.error("No se pudo eliminar el concierto", ex));
    }

    // -------------------------------------------------------------- artista

    @FXML
    private void nuevoArtista() {
        new ArtistaDialog().showAndWait().ifPresent(v -> Async.run(() -> {
            api.crearArtista(v[0], v[1], v[2]);
            return Boolean.TRUE;
        }, ok -> recargarCatalogo.run(), ex -> Dialogs.error("No se pudo crear el artista", ex)));
    }
}