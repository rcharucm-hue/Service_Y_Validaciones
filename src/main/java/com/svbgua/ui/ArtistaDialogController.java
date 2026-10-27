package com.svbgua.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Contenido del diálogo "Nuevo artista" (artista-dialog.fxml). */
public class ArtistaDialogController {

    @FXML private TextField tfNombre;
    @FXML private TextField tfGenero;
    @FXML private TextField tfPais;
    @FXML private Label lblError;

    /** @return true si los datos son válidos; si no, muestra el error. */
    public boolean validar() {
        if (tfNombre.getText().isBlank() || tfNombre.getText().trim().length() > 120
                || tfGenero.getText().isBlank() || tfGenero.getText().trim().length() > 50
                || tfPais.getText().trim().length() > 50) {
            lblError.setText("Nombre (máx. 120) y género (máx. 50) son obligatorios; país máx. 50.");
            return false;
        }
        return true;
    }

    public String[] valores() {
        return new String[]{tfNombre.getText(), tfGenero.getText(), tfPais.getText()};
    }
}
