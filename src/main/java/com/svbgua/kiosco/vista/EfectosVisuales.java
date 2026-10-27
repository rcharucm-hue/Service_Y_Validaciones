package com.svbgua.kiosco.vista;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

/** Utilidades pequeñas de dibujo y animación compartidas por las vistas. */
public final class EfectosVisuales {

    private EfectosVisuales() {
    }

    public static LinearGradient gradienteVertical(Color arriba, Color abajo) {
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, arriba), new Stop(1, abajo));
    }

    public static Polygon poligono(double[] puntos, Paint relleno, Paint borde) {
        Polygon p = new Polygon(puntos);
        p.setFill(relleno);
        p.setStroke(borde);
        p.setMouseTransparent(true);
        return p;
    }

    /** Texto centrado en (cx, cy), transparente a los eventos del ratón/tacto. */
    public static Text textoCentrado(String texto, double cx, double cy, Font fuente, Paint color) {
        Text t = new Text(texto);
        t.setFont(fuente);
        t.setFill(color);
        t.setTextOrigin(VPos.CENTER);
        t.setX(cx - t.getLayoutBounds().getWidth() / 2.0);
        t.setY(cy);
        t.setMouseTransparent(true);
        return t;
    }

    /** Vibración horizontal breve: indica "no permitido". */
    public static void sacudir(Node nodo) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(nodo.translateXProperty(), 0.0)),
                new KeyFrame(Duration.millis(60), new KeyValue(nodo.translateXProperty(), -7.0)),
                new KeyFrame(Duration.millis(120), new KeyValue(nodo.translateXProperty(), 7.0)),
                new KeyFrame(Duration.millis(180), new KeyValue(nodo.translateXProperty(), -5.0)),
                new KeyFrame(Duration.millis(240), new KeyValue(nodo.translateXProperty(), 5.0)),
                new KeyFrame(Duration.millis(300), new KeyValue(nodo.translateXProperty(), 0.0)));
        t.play();
    }
}
