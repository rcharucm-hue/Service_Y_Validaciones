package com.svbgua.kiosco;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Ajusta la barra inferior de los pasos 2 y 3: en vertical los dos botones se reparten el ancho. */
final class BarraPaso {
    private BarraPaso() {}

    static void ajustar(Region espacio, Button izquierda, Button derecha, boolean vertical) {
        espacio.setVisible(!vertical);
        espacio.setManaged(!vertical);
        for (Button b : new Button[]{izquierda, derecha}) {
            b.setMaxWidth(vertical ? Double.MAX_VALUE : Region.USE_COMPUTED_SIZE);
            HBox.setHgrow(b, vertical ? Priority.ALWAYS : Priority.NEVER);
        }
    }
}
