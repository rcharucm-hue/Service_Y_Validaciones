package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.Artista;
import com.svbgua.model.Modelos.Catalogo;
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
 * Pestaña de artistas (artistas-tab.fxml, solo administrador): crear, editar y eliminar.
 * Los datos salen del catálogo ya cargado; al guardar se recarga el catálogo completo.
 */
public class ArtistasAdminController {

    private final ApiClient api = ApiClient.INSTANCE;
    private Runnable recargarCatalogo = () -> { };
    private final ObservableList<Artista> artistas = FXCollections.observableArrayList();

    @FXML private TableView<Artista> tablaArtistas;
    @FXML private TableColumn<Artista, String> cAId;
    @FXML private TableColumn<Artista, String> cANombre;
    @FXML private TableColumn<Artista, String> cAGenero;
    @FXML private TableColumn<Artista, String> cAPais;
    @FXML private TextField tfANombre;
    @FXML private TextField tfAGenero;
    @FXML private TextField tfAPais;
    @FXML private Label lblModoArtista;

    public void setRecargarCatalogo(Runnable recargarCatalogo) {
        this.recargarCatalogo = recargarCatalogo;
    }

    @FXML
    private void initialize() {
        cAId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        cANombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombre()));
        cAGenero.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().genero()));
        cAPais.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().pais()));
        tablaArtistas.setItems(artistas);
        tablaArtistas.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> llenarArtista(n));
    }

    public void setCatalogo(Catalogo catalogo) {
        artistas.setAll(catalogo.artistas().values().stream()
                .sorted(Comparator.comparing(Artista::nombre, String.CASE_INSENSITIVE_ORDER)).toList());
    }

    @FXML
    private void actualizar() {
        recargarCatalogo.run();
    }

    private void llenarArtista(Artista a) {
        if (a == null) return;
        lblModoArtista.setText("Editando artista #" + a.id());
        tfANombre.setText(a.nombre());
        tfAGenero.setText(a.genero());
        tfAPais.setText(a.pais());
    }

    @FXML
    private void nuevoArtista() {
        tablaArtistas.getSelectionModel().clearSelection();
        lblModoArtista.setText("Nuevo artista");
        tfANombre.clear();
        tfAGenero.clear();
        tfAPais.clear();
    }

    @FXML
    private void guardarArtista() {
        Artista editado = tablaArtistas.getSelectionModel().getSelectedItem();
        String nombre = tfANombre.getText();
        String genero = tfAGenero.getText();
        String pais = tfAPais.getText();
        Async.run(() -> {
            if (editado == null) api.crearArtista(nombre, genero, pais);
            else api.actualizarArtista(editado.id(), nombre, genero, pais);
            return Boolean.TRUE;
        }, ok -> {
            nuevoArtista();
            recargarCatalogo.run();
        }, ex -> avisarOError("No se pudo guardar el artista", ex));
    }

    @FXML
    private void eliminarArtista() {
        Artista sel = tablaArtistas.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.aviso("Selecciona un artista de la tabla.");
            return;
        }
        if (!Dialogs.confirmar("¿Eliminar a \"" + sel.nombre() + "\"? No se puede si tiene conciertos registrados.")) return;
        Async.run(() -> {
            api.eliminarArtista(sel.id());
            return Boolean.TRUE;
        }, ok -> {
            nuevoArtista();
            recargarCatalogo.run();
        }, ex -> Dialogs.error("No se pudo eliminar el artista", ex));
    }

    /** Los errores de validación del cliente son avisos al usuario, no fallos del sistema. */
    private static void avisarOError(String titulo, Throwable ex) {
        if (ex instanceof ValidacionException) Dialogs.aviso(ex.getMessage());
        else Dialogs.error(titulo, ex);
    }
}