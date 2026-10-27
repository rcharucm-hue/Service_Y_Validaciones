package com.svbgua.kiosco;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Controlador común de los modales sencillos: cargando, mensaje y decisión (modal-*.fxml). */
public class ModalController {

    @FXML private VBox raiz;
    @FXML private Label lblTitulo;
    @FXML private Label lblTexto;
    @FXML private Button btnOk;
    @FXML private Button btnSecundario;
    @FXML private ProgressBar barra; // solo existe en modal-cargando.fxml

    /** Progreso de adorno: sube rápido al principio y se frena cerca del final, sin llegar a 100 %. */
    @FXML
    private void initialize() {
        if (barra == null) {
            return;
        }
        Timeline avance = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(barra.progressProperty(), 0.0)),
                new KeyFrame(Duration.millis(700), new KeyValue(barra.progressProperty(), 0.35)),
                new KeyFrame(Duration.millis(2500), new KeyValue(barra.progressProperty(), 0.70)),
                new KeyFrame(Duration.millis(8000), new KeyValue(barra.progressProperty(), 0.94)));
        avance.play();
    }

    private Runnable alAceptar = () -> { };
    private Runnable alSecundario = () -> { };

    public VBox raiz() {
        return raiz;
    }

    public void setAncho(double ancho) {
        raiz.setPrefWidth(ancho);
    }

    public void setTitulo(String titulo) {
        lblTitulo.setText(titulo);
    }

    public void setTexto(String texto) {
        lblTexto.setText(texto);
    }

    public void setBotonOk(String texto, Runnable accion) {
        btnOk.setText(texto);
        this.alAceptar = accion;
    }

    /** @param texto null deja el texto del FXML. */
    public void setBotonSecundario(String texto, Runnable accion) {
        if (texto != null) {
            btnSecundario.setText(texto);
        }
        this.alSecundario = accion;
    }

    public void ocultarSecundario() {
        btnSecundario.setVisible(false);
        btnSecundario.setManaged(false);
    }

    @FXML
    private void aceptar() {
        alAceptar.run();
    }

    @FXML
    private void secundario() {
        alSecundario.run();
    }
}
