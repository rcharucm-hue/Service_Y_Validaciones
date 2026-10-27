package com.svbgua.ui;

import com.svbgua.kiosco.KioscoConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

/** Contenido del diálogo de configuración inicial del autoservicio (kiosco-config-dialog.fxml). */
public class KioscoConfigDialogController {

    @FXML private TextField tfUsuario;
    @FXML private PasswordField pfClave;
    @FXML private PasswordField pfPin;
    @FXML private PasswordField pfPin2;
    @FXML private Label lblError;

    @FXML
    private void initialize() {
        for (PasswordField pf : new PasswordField[]{pfPin, pfPin2}) {
            pf.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,8}") ? c : null));
        }
    }

    public void precargar(String usuario, String clave) {
        tfUsuario.setText(usuario == null ? "" : usuario);
        pfClave.setText(clave == null ? "" : clave);
    }

    /** @return mensaje de error, o null si los datos son válidos. */
    public String validar() {
        if (tfUsuario.getText().isBlank() || pfClave.getText().isEmpty()) {
            return "Escribe el usuario y la contraseña de la cuenta de la terminal.";
        }
        if (!pfPin.getText().matches("\\d{4,8}")) {
            return "El PIN debe tener entre 4 y 8 números.";
        }
        if (!pfPin.getText().equals(pfPin2.getText())) {
            return "Los PIN no coinciden.";
        }
        return null;
    }

    public void setError(String texto) {
        lblError.setText(texto);
    }

    public KioscoConfig config() {
        return new KioscoConfig(tfUsuario.getText().trim(), pfClave.getText(), pfPin.getText());
    }
}
