package com.svbgua.kiosco;

import com.svbgua.kiosco.modelo.LineaSeleccion;
import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.util.Formato;
import com.svbgua.util.Correo;
import com.svbgua.kiosco.vista.Paleta;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** Paso 3 (paso3.fxml): resumen y pago con tarjeta (titular, número y CVV ficticios). */
public class Paso3Controller {

    private static final int TITULAR = 0;
    private static final int NUMERO = 1;
    private static final int CVV = 2;
    private static final int CORREO = 3;

    @FXML private GridPane contenido;
    @FXML private ColumnConstraints colResumen;
    @FXML private ColumnConstraints colTarjeta;
    @FXML private VBox resumen;
    @FXML private ScrollPane scrollLineas;
    @FXML private VBox lineas;
    @FXML private Label lblFactura;
    @FXML private Label lblTotal;
    @FXML private VBox bloqueTarjeta;
    @FXML private Label campoTitular;
    @FXML private Label campoTarjeta;
    @FXML private Label campoCvv;
    @FXML private Label campoCorreo;
    @FXML private Node tecTitular;
    @FXML private Node tecNumero;
    @FXML private Node tecCvv;
    @FXML private Node tecCorreo;
    @FXML private TecladoTextoController tecTitularController;
    @FXML private TecladoNumericoController tecNumeroController;
    @FXML private TecladoNumericoController tecCvvController;
    @FXML private TecladoTextoController tecCorreoController;
    @FXML private Region espacio;
    @FXML private Button btnAtras;
    @FXML private Button btnPagar;

    private SesionKiosco sesion;
    private Navegacion nav;
    private int activo = TITULAR;

    public void iniciar(SesionKiosco sesion, Navegacion nav) {
        this.sesion = sesion;
        this.nav = nav;
        tecTitularController.configurar(sesion.titular, 26);
        tecNumeroController.configurar(sesion.tarjeta, 19, false);
        tecCvvController.configurar(sesion.cvv, 4, false);
        tecCorreoController.configurar(sesion.correo, 50);
        tecNumero.getStyleClass().add("teclado-compacto");
        tecCvv.getStyleClass().add("teclado-compacto");

        sesion.tarjeta.addListener((o, a, b) -> {
            // al completar el número (16) pasa solo al CVV
            if (activo == NUMERO && sesion.tarjeta.get().length() >= 16) {
                activo = CVV;
            }
            refrescar();
        });
        sesion.titular.addListener((o, a, b) -> refrescar());
        sesion.cvv.addListener((o, a, b) -> refrescar());
        sesion.correo.addListener((o, a, b) -> refrescar());
    }

    public void mostrar(boolean vertical) {
        BarraPaso.ajustar(espacio, btnAtras, btnPagar, vertical);
        btnPagar.setText("Pagar  " + Formato.moneda(sesion.seleccion.total()));

        // Horizontal: resumen y tarjeta lado a lado. Vertical: uno debajo del otro.
        if (vertical) {
            GridPane.setConstraints(resumen, 0, 0, 2, 1);
            GridPane.setConstraints(bloqueTarjeta, 0, 1, 2, 1);
            colResumen.setPercentWidth(100);
            colTarjeta.setPercentWidth(0);
            contenido.setHgap(0);
        } else {
            GridPane.setConstraints(resumen, 0, 0, 1, 1);
            GridPane.setConstraints(bloqueTarjeta, 1, 0, 1, 1);
            colResumen.setPercentWidth(50);
            colTarjeta.setPercentWidth(50);
            contenido.setHgap(28);
        }
        scrollLineas.setPrefViewportHeight(vertical ? 150 : 260);
        scrollLineas.setMaxHeight(vertical ? 190 : 340);

        lineas.getChildren().clear();
        for (LineaSeleccion ln : sesion.seleccion.lineas()) {
            Localidad l = ln.localidad();
            Label nombre = new Label(l.getTipo().esMesa() ? l.getNombre() : ln.cantidad() + " × " + l.getNombre());
            nombre.getStyleClass().add("linea-nombre");
            Region e = new Region();
            HBox.setHgrow(e, Priority.ALWAYS);
            Label sub = new Label(Formato.moneda(ln.subtotal()));
            sub.getStyleClass().add("linea-subtotal");
            HBox fila = new HBox(10, new Circle(7, Paleta.de(l.getTipo())), nombre, e, sub);
            fila.setAlignment(Pos.CENTER_LEFT);
            lineas.getChildren().add(fila);
        }
        lblFactura.setText("Factura: " + sesion.facturaTexto());
        lblTotal.setText(Formato.moneda(sesion.seleccion.total()));

        activo = TITULAR;
        refrescar();
    }

    private void refrescar() {
        if (sesion == null) {
            return;
        }
        String digitos = sesion.tarjeta.get();
        ponerCampo(campoTitular, sesion.titular.get(), "NOMBRE APELLIDO", activo == TITULAR);
        ponerCampo(campoTarjeta, SesionKiosco.agrupar(digitos), "0000 0000 0000 0000", activo == NUMERO);
        ponerCampo(campoCvv, "•".repeat(sesion.cvv.get().length()), "•••", activo == CVV);
        String correo = sesion.correo.get().trim();
        ponerCampo(campoCorreo, correo, "tucorreo@gmail.com", activo == CORREO);
        mostrarSolo(activo == TITULAR ? tecTitular : activo == NUMERO ? tecNumero : activo == CVV ? tecCvv : tecCorreo);
        boolean correoOk = correo.isEmpty() || Correo.valido(correo);
        btnPagar.setDisable(digitos.length() < 13 || sesion.cvv.get().length() < 3
                || sesion.titular.get().trim().length() < 3 || !correoOk);
    }

    private void mostrarSolo(Node teclado) {
        for (Node t : new Node[]{tecTitular, tecNumero, tecCvv, tecCorreo}) {
            boolean ver = t == teclado;
            t.setVisible(ver);
            t.setManaged(ver);
        }
    }

    private static void ponerCampo(Label campo, String valor, String vacio, boolean activo) {
        campo.setText(valor.isEmpty() ? vacio : valor);
        campo.getStyleClass().removeAll("campo-vacio", "campo-activo");
        if (valor.isEmpty()) {
            campo.getStyleClass().add("campo-vacio");
        }
        if (activo) {
            campo.getStyleClass().add("campo-activo");
        }
    }

    @FXML
    private void activarTitular() {
        activo = TITULAR;
        refrescar();
    }

    @FXML
    private void activarNumero() {
        activo = NUMERO;
        refrescar();
    }

    @FXML
    private void activarCvv() {
        activo = CVV;
        refrescar();
    }

    @FXML
    private void activarCorreo() {
        activo = CORREO;
        refrescar();
    }

    @FXML
    private void atras() {
        nav.irPaso2();
    }

    @FXML
    private void pagar() {
        nav.pagar();
    }
}
