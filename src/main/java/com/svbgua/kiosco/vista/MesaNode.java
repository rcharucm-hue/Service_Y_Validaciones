package com.svbgua.kiosco.vista;

import com.svbgua.kiosco.modelo.TipoLocalidad;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Mesa decorativa (no interactiva). Los asientos son {@link SillaNode} independientes. */
public class MesaNode extends Group {

    static final double R_MESA = 15.0;

    public MesaNode(String id, double cx, double cy, boolean hayDisponibles) {
        setLayoutX(cx);
        setLayoutY(cy);
        setMouseTransparent(true);
        Color base = Paleta.de(TipoLocalidad.MESA);

        Circle grosor = new Circle(R_MESA);
        grosor.setTranslateY(4);
        Circle cara = new Circle(R_MESA);
        cara.setStrokeWidth(1.8);
        Text txt = new Text(id);
        txt.setFont(Font.font("System", FontWeight.BOLD, 10));
        txt.setTextOrigin(javafx.geometry.VPos.CENTER);
        txt.setX(-txt.getLayoutBounds().getWidth() / 2.0);

        if (hayDisponibles) {
            grosor.setFill(base.deriveColor(0, 1, 0.4, 0.95));
            cara.setFill(EfectosVisuales.gradienteVertical(base.deriveColor(0, 1, 1, 0.75),
                    base.deriveColor(0, 1, 0.7, 0.55)));
            cara.setStroke(base);
            txt.setFill(Color.WHITE);
        } else {
            grosor.setFill(Color.web("#1F2128"));
            cara.setFill(EfectosVisuales.gradienteVertical(Color.web("#454957"), Color.web("#2F323C")));
            cara.setStroke(Color.web("#6A6F7E"));
            txt.setFill(Color.web("#A0A5B3"));
        }
        getChildren().addAll(grosor, cara, txt);
    }
}
