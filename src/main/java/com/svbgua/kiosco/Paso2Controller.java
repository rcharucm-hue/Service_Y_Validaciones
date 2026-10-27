package com.svbgua.kiosco;

import com.svbgua.kiosco.vista.EfectosVisuales;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Paso 2 (paso2.fxml): CF o NIT con teclado numérico. */
public class Paso2Controller {

    @FXML private Button btnCf;
    @FXML private Button btnNit;
    @FXML private VBox bloqueNit;
    @FXML private Label campoNit;
    @FXML private Label errorNit;
    @FXML private Region espacio;
    @FXML private Button btnAtras;
    @FXML private Button btnContinuar;
    @FXML private TecladoNumericoController tecladoController; // <fx:include fx:id="teclado">

    private SesionKiosco sesion;
    private Navegacion nav;

    public void iniciar(SesionKiosco sesion, Navegacion nav) {
        this.sesion = sesion;
        this.nav = nav;
        tecladoController.configurar(sesion.nit, 13, true);
        sesion.nit.addListener((o, a, b) -> refrescar());
    }

    public void mostrar(boolean vertical) {
        BarraPaso.ajustar(espacio, btnAtras, btnContinuar, vertical);
        refrescar();
    }

    private boolean nitValido() {
        return sesion.nit.get().matches("\\d{1,12}[0-9K]");
    }

    private void refrescar() {
        btnCf.getStyleClass().setAll("button", "opcion-factura");
        btnNit.getStyleClass().setAll("button", "opcion-factura");
        (sesion.usaNit ? btnNit : btnCf).getStyleClass().add("opcion-factura-activa");
        bloqueNit.setVisible(sesion.usaNit);
        bloqueNit.setManaged(sesion.usaNit);
        String nit = sesion.nit.get();
        campoNit.setText(nit.isEmpty() ? "Toca los números" : SesionKiosco.formatoNit(nit));
        errorNit.setText(sesion.usaNit && !nit.isEmpty() && !nitValido() ? "El NIT todavía no es válido" : "");
        btnContinuar.setDisable(sesion.usaNit && !nitValido());
    }

    @FXML
    private void elegirCf() {
        sesion.usaNit = false;
        refrescar();
    }

    @FXML
    private void elegirNit() {
        sesion.usaNit = true;
        refrescar();
    }

    @FXML
    private void atras() {
        nav.irPaso1();
    }

    @FXML
    private void continuar() {
        if (sesion.usaNit && !nitValido()) {
            EfectosVisuales.sacudir(campoNit);
            nav.aviso("Escribe un NIT válido (números y dígito verificador)");
            return;
        }
        nav.irPaso3();
    }
}
