package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import com.svbgua.model.Modelos.Usuario;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Gestion de usuarios (usuarios-tab.fxml, solo administrador): crear, editar, cambiar contrasena/rol y eliminar. */
public class UsuariosAdminController {

    private final ApiClient api = ApiClient.INSTANCE;
    private final ObservableList<Usuario> datos = FXCollections.observableArrayList();

    @FXML private TableView<Usuario> tabla;
    @FXML private TableColumn<Usuario, String> cId;
    @FXML private TableColumn<Usuario, String> cUsuario;
    @FXML private TableColumn<Usuario, String> cNombre;
    @FXML private TableColumn<Usuario, String> cRol;
    @FXML private TextField tfUsuario;
    @FXML private TextField tfNombre;
    @FXML private PasswordField pfContrasena;
    @FXML private ComboBox<String> cbRol;
    @FXML private Label lblModo;

    @FXML
    private void initialize() {
        cId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().id())));
        cUsuario.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombreUsuario()));
        cNombre.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nombreCompleto()));
        cRol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().rol()));
        tabla.setItems(datos);
        tabla.getSelectionModel().selectedItemProperty().addListener((o, a, n) -> llenarFormulario(n));
    }

    @FXML
    public void cargar() {
        Runnable fin = Progreso.cargandoTabla(tabla);
        Async.run(api::listarUsuarios, lista -> {
            fin.run();
            datos.setAll(lista);
        }, ex -> {
            fin.run();
            Dialogs.error("No se pudieron cargar los usuarios", ex);
        });
    }

    private void llenarFormulario(Usuario u) {
        if (u == null) return;
        lblModo.setText("Editando usuario #" + u.id());
        tfUsuario.setText(u.nombreUsuario());
        tfNombre.setText(u.nombreCompleto());
        pfContrasena.clear();
        cbRol.setValue(u.rol());
    }

    @FXML
    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        lblModo.setText("Nuevo usuario");
        tfUsuario.clear();
        tfNombre.clear();
        pfContrasena.clear();
        cbRol.setValue("vendedor");
    }

    @FXML
    private void guardar() {
        Usuario editado = tabla.getSelectionModel().getSelectedItem();
        String usuario = tfUsuario.getText().trim();
        String nombre = tfNombre.getText().trim();
        String contrasena = pfContrasena.getText();
        String rol = cbRol.getValue();

        if (usuario.isEmpty() || usuario.length() > 50 || usuario.contains(":")) {
            Dialogs.aviso("Usuario: 1 a 50 caracteres, sin dos puntos.");
            return;
        }
        if (nombre.isEmpty() || nombre.length() > 100) {
            Dialogs.aviso("Escribe el nombre completo (máximo 100 caracteres).");
            return;
        }
        boolean pideContrasena = editado == null || !contrasena.isEmpty();
        if (pideContrasena && contrasena.length() < 8) {
            Dialogs.aviso("La contraseña debe tener al menos 8 caracteres.");
            return;
        }
        Async.run(() -> {
            if (editado == null) api.crearUsuarioSesion(usuario, nombre, contrasena, rol);
            else api.actualizarUsuario(editado.id(), usuario, nombre, rol, contrasena);
            return Boolean.TRUE;
        }, ok -> {
            limpiarFormulario();
            cargar();
        }, ex -> Dialogs.error("No se pudo guardar el usuario", ex));
    }

    @FXML
    private void eliminar() {
        Usuario sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            Dialogs.aviso("Selecciona un usuario de la tabla.");
            return;
        }
        if (!Dialogs.confirmar("¿Eliminar al usuario \"" + sel.nombreUsuario() + "\"?")) return;
        Async.run(() -> {
            api.eliminarUsuario(sel.id());
            return Boolean.TRUE;
        }, ok -> {
            limpiarFormulario();
            cargar();
        }, ex -> Dialogs.error("No se pudo eliminar el usuario", ex));
    }
}
