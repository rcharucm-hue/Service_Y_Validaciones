package com.svbgua.kiosco;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/** Una tarjeta de la lista de conciertos (concierto-opcion.fxml). */
public class ConciertoOpcionController {

    private static final double ALTO = 104;

    @FXML private Button raiz;
    @FXML private Label lblNombre;
    @FXML private Label lblDetalle;

    public Button raiz() {
        return raiz;
    }

    public void configurar(String nombre, String detalle, double ancho, Runnable alElegir) {
        lblNombre.setText(nombre);
        lblDetalle.setText(detalle);
        lblNombre.setMaxWidth(ancho - 36);
        lblDetalle.setMaxWidth(ancho - 36);
        raiz.setPrefSize(ancho, ALTO);
        raiz.setMinSize(ancho, ALTO);
        raiz.setMaxSize(ancho, ALTO);
        raiz.setOnAction(e -> alElegir.run());
    }
}
