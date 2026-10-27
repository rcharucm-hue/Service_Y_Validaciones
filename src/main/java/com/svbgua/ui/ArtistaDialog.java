package com.svbgua.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** Registra un artista nuevo (nombre, género y país). */
public class ArtistaDialog extends Dialog<String[]> {

    public ArtistaDialog() {
        setTitle("Nuevo artista");
        setHeaderText("Registrar un artista");
        ButtonType crear = new ButtonType("Crear", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(crear, ButtonType.CANCEL);
        getDialogPane().getStylesheets().add(getClass().getResource("/com/svbgua/style.css").toExternalForm());

        Fxml.Vista<ArtistaDialogController> vista = Fxml.cargar("artista-dialog");
        ArtistaDialogController c = vista.controlador();
        getDialogPane().setContent(vista.raiz());

        Node boton = getDialogPane().lookupButton(crear);
        boton.addEventFilter(ActionEvent.ACTION, ev -> {
            if (!c.validar()) {
                ev.consume();
            }
        });
        setResultConverter(bt -> bt == crear ? c.valores() : null);
    }
}
