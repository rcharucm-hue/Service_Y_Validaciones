package com.svbgua.kiosco;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/** Teclado numérico en pantalla (teclado-numerico.fxml): escribe en la propiedad que se le indique. */
public class TecladoNumericoController {

    @FXML private Button btnK;

    private StringProperty destino;
    private int maximo = 19;

    public void configurar(StringProperty destino, int maximo, boolean permiteK) {
        this.destino = destino;
        this.maximo = maximo;
        btnK.setVisible(permiteK);
    }

    @FXML
    private void teclaPulsada(javafx.event.ActionEvent e) {
        if (destino == null) {
            return;
        }
        String t = ((Button) e.getSource()).getText();
        String actual = destino.get() == null ? "" : destino.get();
        if (t.equals("⌫")) {
            if (!actual.isEmpty()) {
                destino.set(actual.substring(0, actual.length() - 1));
            }
        } else if (actual.length() < maximo) {
            destino.set(actual + t);
        }
    }
}
