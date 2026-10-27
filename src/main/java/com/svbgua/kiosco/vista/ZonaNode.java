package com.svbgua.kiosco.vista;

import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.util.Formato;
import java.util.Locale;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineJoin;
import javafx.util.Duration;

/**
 * Zona de admisión (VIP, Preferencial, General) dibujada como polígono con "grosor":
 * una copia desplazada hacia abajo y oscurecida simula el volumen 2.5D.
 */
public class ZonaNode extends ElementoMapa {

    private static final double PROFUNDIDAD = 10.0;
    private static final Color TEXTO_OSCURO = Color.web("#140E26");

    private final Color base;
    private final Polygon grosor;
    private final Polygon cara;
    private final Polygon contorno;
    private final Label lblNombre = new Label();
    private final Label lblPrecio = new Label();
    private final Label lblInfo = new Label();
    private Timeline pulso;

    public ZonaNode(Localidad localidad) {
        super(localidad);
        base = Paleta.de(localidad.getTipo());
        double[] puntos = localidad.getPuntos();

        grosor = new Polygon(puntos);
        grosor.setTranslateY(PROFUNDIDAD);

        cara = new Polygon(puntos);
        cara.setStrokeWidth(2.0);
        cara.setStrokeLineJoin(StrokeLineJoin.ROUND);

        contorno = new Polygon(puntos);
        contorno.setFill(null);
        contorno.setStroke(base.interpolate(Color.WHITE, 0.75));
        contorno.setStrokeWidth(5.0);
        contorno.setStrokeLineJoin(StrokeLineJoin.ROUND);
        contorno.setOpacity(0.0);
        contorno.setMouseTransparent(true);

        Group cuerpo = new Group(grosor, cara, contorno);
        cuerpo.setEffect(efecto);

        lblNombre.getStyleClass().add("zona-nombre");
        lblPrecio.getStyleClass().add("zona-precio");
        lblInfo.getStyleClass().add("zona-info");
        lblNombre.setText(localidad.getNombre().toUpperCase(Locale.ROOT));
        lblPrecio.setText(Formato.monedaCorta(localidad.getPrecio()) + " c/u");

        VBox etiqueta = new VBox(1, lblNombre, lblPrecio, lblInfo);
        etiqueta.setAlignment(Pos.CENTER);
        etiqueta.setMouseTransparent(true);
        Bounds b = cara.getLayoutBounds();
        etiqueta.setMinSize(b.getWidth(), b.getHeight());
        etiqueta.setPrefSize(b.getWidth(), b.getHeight());
        etiqueta.setMaxSize(b.getWidth(), b.getHeight());
        etiqueta.setLayoutX(b.getMinX());
        etiqueta.setLayoutY(b.getMinY());

        getChildren().addAll(cuerpo, etiqueta);
    }

    @Override
    protected void aplicarCantidad(int cantidad) {
        if (!localidad.estaDisponible()) {
            lblInfo.setText("AGOTADO");
        } else if (seleccionado) {
            lblInfo.setText("✓ " + cantidad + (cantidad == 1 ? " boleto" : " boletos"));
        } else {
            lblInfo.setText(localidad.getDisponibles() + " disponibles");
        }
    }

    @Override
    protected void refrescarColores() {
        if (!localidad.estaDisponible()) {
            cara.setFill(EfectosVisuales.gradienteVertical(Color.web("#3A3E4A"), Color.web("#2B2E37")));
            cara.setStroke(Color.web("#666B7A"));
            grosor.setFill(Color.web("#1F2128"));
            colorearTextos(Color.web("#9AA0AD"));
        } else if (seleccionado) {
            cara.setFill(EfectosVisuales.gradienteVertical(base.interpolate(Color.WHITE, 0.35), base));
            cara.setStroke(Color.WHITE);
            grosor.setFill(base.deriveColor(0, 1, 0.6, 1));
            colorearTextos(TEXTO_OSCURO);
        } else {
            double extra = hover ? 0.22 : 0.0;
            cara.setFill(EfectosVisuales.gradienteVertical(
                    base.deriveColor(0, 1, 1, 0.55 + extra), base.deriveColor(0, 1, 0.75, 0.30 + extra)));
            cara.setStroke(base);
            grosor.setFill(base.deriveColor(0, 1, 0.42, 0.95));
            colorearTextos(Color.WHITE);
        }
    }

    private void colorearTextos(Color c) {
        lblNombre.setTextFill(c);
        lblPrecio.setTextFill(c);
        lblInfo.setTextFill(c);
    }

    @Override
    protected double elevacion() {
        return 8.0;
    }

    @Override
    protected double escala() {
        return 1.025;
    }

    @Override
    protected void alCambiarSeleccion(boolean estaSeleccionado) {
        if (pulso != null) {
            pulso.stop();
        }
        if (estaSeleccionado) {
            pulso = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(contorno.opacityProperty(), 0.45)),
                    new KeyFrame(Duration.millis(750), new KeyValue(contorno.opacityProperty(), 1.0)));
            pulso.setAutoReverse(true);
            pulso.setCycleCount(Animation.INDEFINITE);
            pulso.play();
        } else {
            contorno.setOpacity(0.0);
        }
    }
}
