package com.svbgua.kiosco.vista;

import com.svbgua.kiosco.modelo.Asiento;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Asiento individual de una mesa. Se coloca en coordenadas absolutas del mapa. */
public class SillaNode extends ElementoMapa {

    private static final double R_SILLA = 9.0;
    private static final double R_TACTIL = 14.0;
    private static final double DIST = 26.0;

    private final Asiento asiento;
    private final Color base;
    private final Circle cuerpo = new Circle(R_SILLA);
    private final Text txt = new Text();

    public SillaNode(Asiento asiento) {
        super(asiento);
        this.asiento = asiento;
        this.base = Paleta.de(asiento.getTipo());

        // Posiciones en diagonal (4 por mesa), numeradas en sentido horario desde arriba a la derecha.
        double ang = -Math.PI / 4 + (asiento.getNumero() - 1) * Math.PI / 2;
        setLayoutX(asiento.getCentroX() + Math.cos(ang) * DIST);
        setLayoutY(asiento.getCentroY() + Math.sin(ang) * DIST);

        Circle tactil = new Circle(R_TACTIL);
        tactil.setFill(Color.TRANSPARENT);
        cuerpo.setStrokeWidth(1.6);
        txt.setFont(Font.font("System", FontWeight.BOLD, 10));
        txt.setTextOrigin(VPos.CENTER);
        txt.setMouseTransparent(true);

        Group dibujo = new Group(cuerpo, txt);
        dibujo.setEffect(efecto);
        getChildren().addAll(tactil, dibujo);
    }

    @Override
    protected void aplicarCantidad(int cantidad) {
        txt.setText(!asiento.estaDisponible() ? "×" : seleccionado ? "✓" : String.valueOf(asiento.getNumero()));
        txt.setX(-txt.getLayoutBounds().getWidth() / 2.0);
        txt.setY(0);
    }

    @Override
    protected void refrescarColores() {
        if (!asiento.estaDisponible()) {
            cuerpo.setFill(Color.web("#3A3D48"));
            cuerpo.setStroke(Color.web("#555A68"));
            txt.setFill(Color.web("#A0A5B3"));
        } else if (seleccionado) {
            cuerpo.setFill(base.interpolate(Color.WHITE, 0.55));
            cuerpo.setStroke(Color.WHITE);
            txt.setFill(Color.web("#140E26"));
        } else {
            double extra = hover ? 0.2 : 0.0;
            cuerpo.setFill(base.deriveColor(0, 0.85, 0.65, 0.85 + extra));
            cuerpo.setStroke(base.deriveColor(0, 1, 0.9, 0.9));
            txt.setFill(Color.WHITE);
        }
    }

    @Override
    protected double elevacion() {
        return 3.0;
    }

    @Override
    protected double escala() {
        return 1.25;
    }
}
