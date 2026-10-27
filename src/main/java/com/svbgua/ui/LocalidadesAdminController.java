package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.Catalogo;
import com.svbgua.model.Modelos.Localidad;
import com.svbgua.validacion.ValidacionException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.Comparator;

/**
 * Pestaña de localidades (localidades-tab.fxml, solo administrador): crear, editar y eliminar.
 * Los datos salen del catálogo ya cargado; al guardar se recarga el catálogo completo.
 */
public class LocalidadesAdminController {

    private final ApiClient api = ApiClient.INSTANCE;
    private Runnable recargarCatalogo = () -> { };
    private final ObservableList<Localidad> localidades = FXCollections.observableArrayList();

    @FXML private TableView<Localidad> tablaLocalidades;
    @FXML private TableColumn<Localidad, String> cLId;
    @FXML private TableColumn<Localidad, String> cLNombre;
    @FXML private TextField tfLNombre;
    @FXML private Label lblModoLocalidad;

    public void setRecargarCatalogo(Runnable recargarCatalogo) {
        this.recargarCatalogo = recargarCatalogo;
    }

    @FXML
    private void initialize() {
        cLId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        cLNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        tablaLocalidades.setItems(localidades);
        tablaLocalidades.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> llenarLocalidad(n));
    }

    public void setCatalogo(Catalogo catalogo) {
        localidades.setAll(catalogo.localidades().values().stream()
                .sorted(Comparator.comparing(Localidad::nombre, String.CASE_INSENSITIVE_ORDER)).toList());
    }

    @FXML
    private void actualizar() {
        recargarCatalogo.run();
    }

    private void llenarLocalidad(Localidad l) {
        if (l == null) return;
        lblModoLocalidad.setText("Editando localidad #" + l.id());
        tfLNombre.setText(l.nombre());
    }

    @FXML
    private void nuevaLocalidad() {
        tablaLocalidades.getSelectionModel().clearSelection();
        lblModoLocalidad.setText("Nueva localidad");
        tfLNombre.clear();
    }

    @FXML
    private void guardarLocalidad() {
        Localidad editada = tablaLocalidades.getSelectionModel().getSelectedItem();
        String nombre = tfLNombre.getText();
        Async.run(() -> {
            if (editada == null) api.crearLocalidad(nombre);
            else api.actualizarLocalidad(editada.id(), nombre);
            return Boolean.TRUE;
        }, ok -> {
            nuevaLocalidad();
            recargarCatalogo.run();
        }, ex -> avisarOError("No se pudo guardar la localidad", ex));
    }

    @FXML
    private void eliminarLocalidad() {
        Localidad sel = tablaLocalidades.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.aviso("Selecciona una localidad de la tabla.");
            return;
        }
        if (!Dialogs.confirmar("¿Eliminar la localidad \"" + sel.nombre() + "\"? No se puede si tiene inventario asignado.")) return;
        Async.run(() -> {
            api.eliminarLocalidad(sel.id());
            return Boolean.TRUE;
        }, ok -> {
            nuevaLocalidad();
            recargarCatalogo.run();
        }, ex -> Dialogs.error("No se pudo eliminar la localidad", ex));
    }

    /** Los errores de validación del cliente son avisos al usuario, no fallos del sistema. */
    private static void avisarOError(String titulo, Throwable ex) {
        if (ex instanceof ValidacionException) Dialogs.aviso(ex.getMessage());
        else Dialogs.error(titulo, ex);
    }
}