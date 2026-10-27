package com.svbgua.kiosco;

import com.svbgua.kiosco.modelo.LineaSeleccion;
import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.util.Formato;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import com.svbgua.ui.Progreso;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Paso 4 (comprobante.fxml): comprobante de la compra con cuenta regresiva para volver al inicio. */
public class ComprobanteController {

    private static final int SEGUNDOS_REINICIO = 45;

    @FXML private Label lblVenta;
    @FXML private Label lblCorreo;
    @FXML private VBox lineas;
    @FXML private Label lblDetalles;
    @FXML private Label lblTotal;
    @FXML private Label lblCuenta;

    private SesionKiosco sesion;
    private Navegacion nav;
    private Timeline reinicio;

    public void iniciar(SesionKiosco sesion, Navegacion nav) {
        this.sesion = sesion;
        this.nav = nav;
    }

    public void mostrar() {
        lblVenta.setText("Venta No. " + sesion.ultimaVenta.id());

        lineas.getChildren().clear();
        for (LineaSeleccion ln : sesion.ultimasLineas) {
            Localidad l = ln.localidad();
            Label nombre = new Label(l.getTipo().esMesa() ? l.getNombre() : ln.cantidad() + " × " + l.getNombre());
            nombre.getStyleClass().add("res-celda");
            Region e = new Region();
            HBox.setHgrow(e, Priority.ALWAYS);
            Label sub = new Label(Formato.moneda(ln.subtotal()));
            sub.getStyleClass().add("linea-subtotal");
            HBox fila = new HBox(10, nombre, e, sub);
            fila.setAlignment(Pos.CENTER_LEFT);
            lineas.getChildren().add(fila);
        }
        lblDetalles.setText(sesion.info.artista() + " · " + sesion.info.fecha()
                + "\nTitular: " + sesion.ultimoTitular
                + "\nPago: " + sesion.ultimaTarjeta
                + "\nFactura: " + sesion.ultimaFactura);
        lblTotal.setText(Formato.moneda(sesion.ultimaVenta.total()));
        setEstadoCorreo(null, false);

        detener();
        int[] restante = {SEGUNDOS_REINICIO};
        lblCuenta.setText("Volviendo al inicio en " + restante[0] + " s");
        reinicio = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            restante[0]--;
            lblCuenta.setText("Volviendo al inicio en " + restante[0] + " s");
            if (restante[0] <= 0) {
                nav.nuevaCompra();
            }
        }));
        reinicio.setCycleCount(SEGUNDOS_REINICIO);
        reinicio.play();
    }

    /** Estado del envío del comprobante por correo. {@code texto} null oculta el mensaje. */
    public void setEstadoCorreo(String texto, boolean enviando) {
        boolean ver = texto != null;
        lblCorreo.setVisible(ver);
        lblCorreo.setManaged(ver);
        lblCorreo.setText(texto == null ? "" : texto);
        lblCorreo.setGraphic(enviando ? Progreso.spinner(16) : null);
    }

    public void detener() {
        if (reinicio != null) {
            reinicio.stop();
            reinicio = null;
        }
    }

    @FXML
    private void nuevaCompra() {
        nav.nuevaCompra();
    }
}
