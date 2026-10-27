package com.svbgua.ui;

import com.svbgua.kiosco.KioscoConfig;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

/** Configuración inicial del autoservicio: cuenta de la terminal y PIN del personal. */
public class KioscoConfigDialog extends Dialog<KioscoConfig> {

    public KioscoConfigDialog(String usuario, String clave) {
        setTitle("Configurar autoservicio");
        setHeaderText("Primera vez en este equipo");

        ButtonType guardar = new ButtonType("Guardar y abrir", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);

        Fxml.Vista<KioscoConfigDialogController> vista = Fxml.cargar("kiosco-config-dialog");
        KioscoConfigDialogController c = vista.controlador();
        c.precargar(usuario, clave);
        getDialogPane().setContent(vista.raiz());
        getDialogPane().getStylesheets().add(getClass().getResource("/com/svbgua/style.css").toExternalForm());

        Node boton = getDialogPane().lookupButton(guardar);
        boton.addEventFilter(ActionEvent.ACTION, ev -> {
            String err = c.validar();
            if (err != null) {
                c.setError(err);
                ev.consume();
            }
        });
        setResultConverter(bt -> bt == guardar ? c.config() : null);
    }
}
