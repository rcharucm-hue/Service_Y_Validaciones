package com.svbgua.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Contenido del diálogo de comprobante (comprobante-venta.fxml): QR, estado del correo y texto. */
public class ComprobanteVentaController {

    @FXML private ImageView imgQr;
    @FXML private Label lblCorreo;
    @FXML private TextArea area;

    @FXML
    private void initialize() {
        ocultarCorreo();
    }

    public void configurar(String texto, Image qr) {
        area.setText(texto);
        imgQr.setImage(qr);
    }

    public void ocultarCorreo() {
        lblCorreo.setVisible(false);
        lblCorreo.setManaged(false);
    }

    /** Muestra el estado del envío; con {@code enviando} se ve un círculo que gira. */
    public void setEstadoCorreo(String texto, boolean enviando) {
        lblCorreo.setText(texto);
        lblCorreo.setGraphic(enviando ? Progreso.spinner(16) : null);
        lblCorreo.setVisible(true);
        lblCorreo.setManaged(true);
    }
}
