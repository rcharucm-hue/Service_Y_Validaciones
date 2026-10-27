package com.svbgua.kiosco;

import com.svbgua.kiosco.vista.EfectosVisuales;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Modal con teclado numérico para el PIN del personal (modal-pin.fxml). */
public class ModalPinController {

    @FXML private VBox raiz;
    @FXML private Label campoPin;
    @FXML private Label lblError;
    @FXML private TecladoNumericoController tecladoController; // <fx:include fx:id="teclado">

    private final StringProperty pin = new SimpleStringProperty("");
    private String esperado = "";
    private Runnable alCorrecto = () -> { };
    private Runnable alCancelar = () -> { };

    @FXML
    private void initialize() {
        tecladoController.configurar(pin, 8, false);
        pin.addListener((o, a, b) -> mostrar());
        mostrar();
    }

    public VBox raiz() {
        return raiz;
    }

    public void configurar(String esperado, Runnable alCorrecto, Runnable alCancelar) {
        this.esperado = esperado;
        this.alCorrecto = alCorrecto;
        this.alCancelar = alCancelar;
    }

    private void mostrar() {
        campoPin.setText(pin.get().isEmpty() ? "PIN" : "•".repeat(pin.get().length()));
        lblError.setText("");
    }

    @FXML
    private void aceptar() {
        if (!esperado.isEmpty() && esperado.equals(pin.get())) {
            alCorrecto.run();
            return;
        }
        pin.set("");
        lblError.setText("PIN incorrecto");
        EfectosVisuales.sacudir(campoPin);
    }

    @FXML
    private void cancelar() {
        pin.set("");
        alCancelar.run();
    }
}
