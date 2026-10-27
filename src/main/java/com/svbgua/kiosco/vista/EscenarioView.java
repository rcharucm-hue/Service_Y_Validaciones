package com.svbgua.kiosco.vista;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

/**
 * Escenario 2.5D: plataforma con grosor, torres laterales, pantalla luminosa, truss con
 * focos y haces de luz que oscilan suavemente. Los haces viven en un grupo aparte
 * ({@link #getHaces()}) para dibujarlos DETRÁS de las zonas.
 */
public class EscenarioView extends Group {

    private final Group haces = new Group();

    public EscenarioView() {
        setMouseTransparent(true);
        haces.setMouseTransparent(true);
        construirHaces();
        construirEscenario();
    }

    public Group getHaces() {
        return haces;
    }

    private void construirHaces() {
        double[] xs = {480, 533, 587, 640};
        Color[] colores = {Color.web("#FF4F8B"), Color.web("#4FC3FF"), Color.web("#FFC857"), Color.web("#B65CFF")};
        double[] centros = {14, 5, -5, -14};
        double[] amplitudes = {9, 7, 8, 10};
        double[] segundos = {3.6, 4.4, 3.9, 4.8};
        for (int i = 0; i < xs.length; i++) {
            crearHaz(xs[i], 24, 52, 330, colores[i], centros[i], amplitudes[i], segundos[i], i * 0.9);
        }
    }

    private void crearHaz(double x, double y, double semiAncho, double largo, Color color,
                          double centro, double amplitud, double segundos, double fase) {
        Polygon haz = new Polygon(0, 0, -semiAncho, largo, semiAncho, largo);
        haz.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 1, 1, 0.34)),
                new Stop(1, color.deriveColor(0, 1, 1, 0.0))));
        haz.setBlendMode(BlendMode.SCREEN);
        haz.setMouseTransparent(true);
        haz.setTranslateX(x);
        haz.setTranslateY(y);
        Rotate giro = new Rotate(centro - amplitud, 0, 0); // pivote en el foco
        haz.getTransforms().add(giro);

        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(giro.angleProperty(), centro - amplitud)),
                new KeyFrame(Duration.seconds(segundos),
                        new KeyValue(giro.angleProperty(), centro + amplitud, Interpolator.EASE_BOTH)));
        t.setAutoReverse(true);
        t.setCycleCount(Animation.INDEFINITE);
        t.jumpTo(Duration.seconds(fase));
        t.play();
        haces.getChildren().add(haz);
    }

    private void construirEscenario() {
        Color borde = Color.web("#5A607A");

        Polygon sueloGrosor = EfectosVisuales.poligono(
                new double[]{318, 96, 802, 96, 836, 130, 284, 130}, Color.web("#0C0D12"), null);
        sueloGrosor.setTranslateY(9);
        Polygon suelo = EfectosVisuales.poligono(
                new double[]{318, 96, 802, 96, 836, 130, 284, 130},
                EfectosVisuales.gradienteVertical(Color.web("#3A3F52"), Color.web("#1C1E29")), borde);

        Polygon torreIzq = EfectosVisuales.poligono(new double[]{340, 28, 436, 28, 436, 96, 340, 96},
                EfectosVisuales.gradienteVertical(Color.web("#343846"), Color.web("#1A1C25")), borde);
        Polygon torreDer = EfectosVisuales.poligono(new double[]{684, 28, 780, 28, 780, 96, 684, 96},
                EfectosVisuales.gradienteVertical(Color.web("#343846"), Color.web("#1A1C25")), borde);

        Rectangle marco = new Rectangle(440, 18, 240, 82);
        marco.setArcWidth(12);
        marco.setArcHeight(12);
        marco.setFill(Color.web("#0F1016"));
        marco.setStroke(borde);
        marco.setMouseTransparent(true);

        Rectangle pantalla = new Rectangle(448, 26, 224, 66);
        pantalla.setArcWidth(8);
        pantalla.setArcHeight(8);
        pantalla.setMouseTransparent(true);
        pantalla.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FF3D7F")), new Stop(0.55, Color.web("#7B2CFF")),
                new Stop(1, Color.web("#1E88FF"))));
        pantalla.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web("#FF4F8B", 0.65), 32, 0.2, 0, 0));
        Timeline brillo = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(pantalla.opacityProperty(), 0.82)),
                new KeyFrame(Duration.seconds(2.2), new KeyValue(pantalla.opacityProperty(), 1.0)));
        brillo.setAutoReverse(true);
        brillo.setCycleCount(Animation.INDEFINITE);
        brillo.play();

        Line truss = new Line(452, 16, 668, 16);
        truss.setStroke(Color.web("#8A8FA3"));
        truss.setStrokeWidth(3);
        truss.setMouseTransparent(true);

        getChildren().addAll(sueloGrosor, suelo, torreIzq, torreDer, marco, pantalla, truss);
        for (double x : new double[]{480, 533, 587, 640}) {
            Circle foco = new Circle(x, 16, 6, Color.WHITE);
            foco.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.WHITE, 16, 0.5, 0, 0));
            foco.setMouseTransparent(true);
            getChildren().add(foco);
        }
        getChildren().add(EfectosVisuales.textoCentrado("E S C E N A R I O", 560, 114,
                Font.font("System", FontWeight.BOLD, 14), Color.web("#C9CCDA")));
    }
}
