package com.svbgua.kiosco;

import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.util.Formato;
import com.svbgua.kiosco.vista.EfectosVisuales;
import com.svbgua.kiosco.vista.MapaRecintoView;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;

/** Paso 1 (paso1.fxml): mapa interactivo, panel de selección y barra inferior con totales. */
public class Paso1Controller {

    @FXML private BorderPane raiz;
    @FXML private VBox zonaMapa;
    @FXML private StackPane contenedorMapa;
    @FXML private FlowPane leyenda;
    @FXML private StackPane contenedorPanel;
    @FXML private VBox zonaInferior;
    @FXML private HBox filaDatos;
    @FXML private HBox filaBotones;
    @FXML private Label lblBoletos;
    @FXML private Label lblMesas;
    @FXML private Label lblTotal;
    @FXML private Button btnLimpiar;
    @FXML private Button btnContinuar;
    @FXML private PanelSeleccionController panelController; // <fx:include fx:id="panel">

    private SesionKiosco sesion;
    private Navegacion nav;
    private MapaRecintoView mapa;

    public void iniciar(SesionKiosco sesion, Navegacion nav) {
        this.sesion = sesion;
        this.nav = nav;
        panelController.iniciar(sesion.seleccion, nav::aviso);
    }

    /** Dibuja el mapa para las localidades y la orientación indicadas. */
    public void mostrar(List<Localidad> localidades, boolean vertical) {
        acomodar(vertical);

        mapa = new MapaRecintoView(localidades, vertical);
        mapa.setAlTocar(this::alTocarLocalidad);
        contenedorMapa.getChildren().setAll(mapa);
        Leyenda.llenar(leyenda, localidades);

        panelController.setHorizontal(vertical);
        refrescar();
    }

    private void acomodar(boolean vertical) {
        // Los botones viven en la fila de datos (horizontal) o en su propia fila (vertical).
        filaDatos.getChildren().removeAll(btnLimpiar, btnContinuar);
        filaBotones.getChildren().removeAll(btnLimpiar, btnContinuar);
        if (vertical) {
            raiz.setRight(null);
            if (!zonaInferior.getChildren().contains(contenedorPanel)) {
                zonaInferior.getChildren().add(0, contenedorPanel);
            }
            contenedorPanel.setPrefWidth(Region.USE_COMPUTED_SIZE);
            contenedorPanel.setMinWidth(Region.USE_COMPUTED_SIZE);
            contenedorPanel.setPrefHeight(470);
            contenedorPanel.setMinHeight(260);
            for (Button b : new Button[]{btnLimpiar, btnContinuar}) {
                b.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(b, Priority.ALWAYS);
            }
            filaBotones.getChildren().addAll(btnLimpiar, btnContinuar);
            filaBotones.setVisible(true);
            filaBotones.setManaged(true);
            filaDatos.setSpacing(0);
            zonaMapa.setPadding(new Insets(8, 16, 8, 16));
        } else {
            zonaInferior.getChildren().remove(contenedorPanel);
            raiz.setRight(contenedorPanel);
            contenedorPanel.setPrefWidth(420);
            contenedorPanel.setMinWidth(380);
            contenedorPanel.setPrefHeight(Region.USE_COMPUTED_SIZE);
            contenedorPanel.setMinHeight(Region.USE_COMPUTED_SIZE);
            for (Button b : new Button[]{btnLimpiar, btnContinuar}) {
                b.setMaxWidth(Region.USE_COMPUTED_SIZE);
                HBox.setHgrow(b, Priority.NEVER);
            }
            filaDatos.getChildren().addAll(btnLimpiar, btnContinuar);
            filaBotones.setVisible(false);
            filaBotones.setManaged(false);
            filaDatos.setSpacing(30);
            zonaMapa.setPadding(new Insets(8, 10, 8, 20));
        }
    }

    private void alTocarLocalidad(Localidad l) {
        if (!sesion.seleccion.tocar(l)) {
            mapa.sacudir(l);
            nav.aviso(l.getNombre() + " no está disponible");
        }
    }

    /** Se llama cada vez que cambia la selección. */
    public void refrescar() {
        if (mapa == null) {
            return;
        }
        mapa.refrescar(sesion.seleccion);
        panelController.refrescar();
        int b = sesion.seleccion.totalBoletos();
        int m = sesion.seleccion.totalAsientos();
        lblBoletos.setText(b + (b == 1 ? " boleto" : " boletos"));
        lblMesas.setText(m + (m == 1 ? " asiento" : " asientos"));
        lblTotal.setText(Formato.moneda(sesion.seleccion.total()));
        btnLimpiar.setDisable(sesion.seleccion.estaVacia());
    }

    @FXML
    private void limpiar() {
        sesion.seleccion.limpiar();
    }

    @FXML
    private void continuar() {
        if (sesion.seleccion.estaVacia()) {
            EfectosVisuales.sacudir(btnContinuar);
            nav.aviso("Selecciona al menos una localidad para continuar");
            return;
        }
        nav.irPaso2();
    }
}
