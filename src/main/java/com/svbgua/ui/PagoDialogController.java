package com.svbgua.ui;

import com.svbgua.model.Modelos.DatosPago;
import com.svbgua.util.Correo;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;

/** Contenido del diálogo de pago (pago-dialog.fxml). */
public class PagoDialogController {

    @FXML private RadioButton rbCf;
    @FXML private RadioButton rbNit;
    @FXML private TextField tfNit;
    @FXML private TextField tfNombre;
    @FXML private TextField tfTarjeta;
    @FXML private TextField tfCorreo;
    @FXML private Label lblError;

    @FXML
    private void initialize() {
        tfTarjeta.setTextFormatter(new TextFormatter<>(formatoTarjeta()));
        Campos.nit(tfNit);
        tfNit.disableProperty().bind(rbCf.selectedProperty());
        tfNombre.disableProperty().bind(rbCf.selectedProperty());
    }

    public void setError(String texto) {
        lblError.setText(texto);
    }

    /** @return mensaje de error, o null si los datos son válidos. */
    public String validar() {
        String digitos = tfTarjeta.getText().replaceAll("\\D", "");
        if (digitos.length() < 13 || digitos.length() > 19) {
            return "El número de tarjeta debe tener entre 13 y 19 dígitos.";
        }
        if (!tfCorreo.getText().isBlank() && !Correo.valido(tfCorreo.getText())) {
            return "El correo no es válido (ej. nombre@gmail.com) o déjalo vacío.";
        }
        if (rbNit.isSelected()) {
            if (!tfNit.getText().trim().matches("\\d{1,12}-?[0-9Kk]")) {
                return "Escribe un NIT válido (números y, al final, dígito verificador).";
            }
            if (tfNombre.getText().isBlank()) {
                return "Escribe el nombre para la factura.";
            }
        }
        return null;
    }

    public DatosPago datos() {
        return new DatosPago(tfNombre.getText().trim(), rbCf.isSelected(), tfNit.getText().trim(), tfTarjeta.getText(),
                tfCorreo.getText().trim());
    }

    /** Solo digitos, maximo 19, agrupados de 4 en 4. */
    private static UnaryOperator<TextFormatter.Change> formatoTarjeta() {
        return cambio -> {
            if (!cambio.isContentChange()) return cambio;
            String digitos = cambio.getControlNewText().replaceAll("\\D", "");
            if (digitos.length() > 19) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digitos.length(); i++) {
                if (i > 0 && i % 4 == 0) sb.append(' ');
                sb.append(digitos.charAt(i));
            }
            cambio.setRange(0, cambio.getControlText().length());
            cambio.setText(sb.toString());
            cambio.selectRange(sb.length(), sb.length());
            return cambio;
        };
    }
}
