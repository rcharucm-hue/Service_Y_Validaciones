package com.svbgua.kiosco;

import com.svbgua.kiosco.modelo.InfoConcierto;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Encabezado del autoservicio (encabezado.fxml): logo, título del paso, concierto y pasos. */
public class EncabezadoController {

    @FXML private GridPane raiz;
    @FXML private Button btnRegresar;
    @FXML private VBox titulos;
    @FXML private Label lblTitulo;
    @FXML private Label lblConcierto;
    @FXML private Label lblDetalle;
    @FXML private Button btnCambiar;
    @FXML private Label lblPaso1;
    @FXML private Label lblPaso2;
    @FXML private Label lblPaso3;

    private Runnable alRegresar = () -> { };
    private Runnable alCambiarConcierto = () -> { };
    private Runnable alGestoPersonal = () -> { };
    private int toques;
    private long ultimoToque;

    /** Gesto oculto del personal: 5 toques seguidos al logo. */
    public void setAlGestoPersonal(Runnable r) {
        this.alGestoPersonal = r;
    }

    @FXML
    private void logoTocado() {
        long ahora = System.currentTimeMillis();
        if (ahora - ultimoToque > 1500) {
            toques = 0;
        }
        ultimoToque = ahora;
        if (++toques >= 5) {
            toques = 0;
            alGestoPersonal.run();
        }
    }

    public void setAlRegresar(Runnable r) {
        this.alRegresar = r;
    }

    public void setAlCambiarConcierto(Runnable r) {
        this.alCambiarConcierto = r;
    }

    @FXML
    private void regresar() {
        alRegresar.run();
    }

    @FXML
    private void cambiarConcierto() {
        alCambiarConcierto.run();
    }

    public void configurar(String titulo, InfoConcierto info, int paso, boolean conRegresar, boolean conCambiar,
                           boolean vertical) {
        lblTitulo.setText(titulo);
        lblConcierto.setText(info.artista() + " · " + info.gira());
        lblDetalle.setText(info.fecha() + "   |   " + info.recinto());
        btnRegresar.setVisible(conRegresar);
        btnRegresar.setManaged(conRegresar);
        btnCambiar.setVisible(conCambiar);
        btnCambiar.setManaged(conCambiar);
        ponerPaso(lblPaso1, 1, "Localidad", paso);
        ponerPaso(lblPaso2, 2, "Datos", paso);
        ponerPaso(lblPaso3, 3, "Pago", paso);

        // Horizontal: una sola fila. Vertical: el título baja a una segunda fila.
        if (vertical) {
            raiz.setHgap(18);
            raiz.setVgap(10);
            GridPane.setConstraints(titulos, 0, 1, 6, 1);
            GridPane.setHalignment(titulos, HPos.CENTER);
        } else {
            raiz.setHgap(26);
            raiz.setVgap(0);
            GridPane.setConstraints(titulos, 3, 0, 1, 1);
            GridPane.setHalignment(titulos, HPos.LEFT);
        }
        raiz.setVisible(true);
    }

    private static void ponerPaso(Label l, int n, String texto, int paso) {
        l.setText((paso > n || paso == 4 ? "✓" : String.valueOf(n)) + "  " + texto);
        l.getStyleClass().removeAll("paso-activo", "paso-inactivo");
        l.getStyleClass().add(paso == n ? "paso-activo" : "paso-inactivo");
    }
}
