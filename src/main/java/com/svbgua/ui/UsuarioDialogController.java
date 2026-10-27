package com.svbgua.ui;

import com.svbgua.api.ApiClient;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.function.Consumer;

/** Contenido del diálogo "Añadir usuario" (usuario-dialog.fxml). */
public class UsuarioDialogController {

    @FXML private TextField tfAdmin;
    @FXML private PasswordField pfAdmin;
    @FXML private TextField tfUsuario;
    @FXML private TextField tfNombre;
    @FXML private PasswordField pfContrasena;
    @FXML private PasswordField pfRepetir;
    @FXML private ComboBox<String> cbRol;
    @FXML private Label lblError;

    public void setError(String texto) {
        lblError.setText(texto);
    }

    /** @return mensaje de error, o null si los datos son válidos. */
    public String validar() {
        if (tfAdmin.getText().isBlank() || pfAdmin.getText().isEmpty()) return "Escribe las credenciales del administrador.";
        if (tfUsuario.getText().isBlank() || tfUsuario.getText().contains(":") || tfUsuario.getText().trim().length() > 50) {
            return "Usuario: 1 a 50 caracteres, sin dos puntos.";
        }
        if (tfNombre.getText().isBlank() || tfNombre.getText().trim().length() > 100) return "Escribe el nombre completo (máx. 100).";
        if (pfContrasena.getText().length() < 8) return "La contraseña debe tener al menos 8 caracteres.";
        if (!pfContrasena.getText().equals(pfRepetir.getText())) return "Las contraseñas no coinciden.";
        return null;
    }

    public void crear(String urlServidor, Consumer<String> alCrear, Consumer<Throwable> alFallar) {
        Async.run(() -> ApiClient.INSTANCE.crearUsuario(urlServidor, tfAdmin.getText(), pfAdmin.getText(),
                        tfUsuario.getText(), tfNombre.getText(), pfContrasena.getText(), cbRol.getValue()),
                u -> alCrear.accept(u.nombreUsuario()), alFallar);
    }
}
