package com.svbgua.ui;

import com.svbgua.model.Modelos.DatosPago;
import com.svbgua.util.Fmt;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import java.math.BigDecimal;

/** Pide numero de tarjeta y si la factura es CF o a nombre de un NIT. */
public class PagoDialog extends Dialog<DatosPago> {

    public PagoDialog(BigDecimal total) {
        setTitle("Pago");
        setHeaderText("Total a pagar: " + Fmt.money(total));

        ButtonType pagar = new ButtonType("Pagar", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(pagar, ButtonType.CANCEL);

        Fxml.Vista<PagoDialogController> vista = Fxml.cargar("pago-dialog");
        PagoDialogController c = vista.controlador();
        getDialogPane().setContent(vista.raiz());
        getDialogPane().getStylesheets().add(getClass().getResource("/com/svbgua/style.css").toExternalForm());

        Node botonPagar = getDialogPane().lookupButton(pagar);
        botonPagar.addEventFilter(ActionEvent.ACTION, ev -> {
            String err = c.validar();
            if (err != null) {
                c.setError(err);
                ev.consume();
            }
        });

        setResultConverter(bt -> bt == pagar ? c.datos() : null);
    }
}
