package com.svbgua.kiosco;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/** Teclado de letras en pantalla (teclado-texto.fxml). */
public class TecladoTextoController {

    private StringProperty destino;
    private int maximo = 26;

    public void configurar(StringProperty destino, int maximo) {
        this.destino = destino;
        this.maximo = maximo;
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
            return;
        }
        if (t.equals("ESPACIO")) {
            t = " ";
            if (actual.isEmpty() || actual.endsWith(" ")) {
                return;
            }
        }
        if (actual.length() + t.length() <= maximo) {
            destino.set(actual + t);
        }
    }
}
