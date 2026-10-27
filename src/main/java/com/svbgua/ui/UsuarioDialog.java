package com.svbgua.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** Crea un usuario nuevo. El backend solo lo permite a un administrador, asi que se piden sus credenciales. */
public class UsuarioDialog extends Dialog<String> {

    public UsuarioDialog(String urlServidor) {
        setTitle("Añadir usuario");
        setHeaderText("Crear un usuario nuevo");

        ButtonType crear = new ButtonType("Crear usuario", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(crear, ButtonType.CANCEL);

        Fxml.Vista<UsuarioDialogController> vista = Fxml.cargar("usuario-dialog");
        UsuarioDialogController c = vista.controlador();
        getDialogPane().setContent(vista.raiz());
        getDialogPane().getStylesheets().add(getClass().getResource("/com/svbgua/style.css").toExternalForm());

        Node boton = getDialogPane().lookupButton(crear);
        boton.addEventFilter(ActionEvent.ACTION, ev -> {
            ev.consume(); // se cierra solo si el backend acepta
            if (Progreso.trabajando((Button) boton)) {
                return;
            }
            String err = c.validar();
            if (err != null) {
                c.setError(err);
                return;
            }
            c.setError("");
            Progreso.enBoton((Button) boton, "Creando...");
            c.crear(urlServidor, usuario -> {
                setResult(usuario);
                close();
            }, ex -> {
                c.setError(Dialogs.mensaje(ex));
                Progreso.finBoton((Button) boton, "Crear usuario");
            });
        });
    }
}
