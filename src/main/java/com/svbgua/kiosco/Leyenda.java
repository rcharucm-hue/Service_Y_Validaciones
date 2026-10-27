package com.svbgua.kiosco;

import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.modelo.TipoLocalidad;
import com.svbgua.kiosco.util.Formato;
import com.svbgua.kiosco.vista.Paleta;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Llena la leyenda de colores por tipo (con precio) y de estados. Se genera desde los datos. */
final class Leyenda {
    private Leyenda() {}

    static void llenar(FlowPane destino, List<Localidad> localidades) {
        destino.getChildren().clear();
        Map<TipoLocalidad, Localidad> muestras = new EnumMap<>(TipoLocalidad.class);
        for (Localidad l : localidades) {
            muestras.putIfAbsent(l.getTipo(), l);
        }
        for (TipoLocalidad t : TipoLocalidad.values()) {
            Localidad m = muestras.get(t);
            if (m != null) {
                String nombre = t.esMesa() ? "Asiento" : m.getNombre();
                destino.getChildren().add(item(Paleta.de(t), nombre + " " + Formato.monedaCorta(m.getPrecio()), false));
            }
        }
        destino.getChildren().add(item(Color.WHITE, "Seleccionado", true));
        destino.getChildren().add(item(Paleta.NO_DISPONIBLE, "No disponible", false));
    }

    private static HBox item(Color color, String texto, boolean brillo) {
        Circle punto = new Circle(7, color);
        if (brillo) {
            punto.setEffect(new DropShadow(10, Color.WHITE));
        }
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("leyenda-texto");
        HBox h = new HBox(8, punto, etiqueta);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
