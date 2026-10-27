package com.svbgua.kiosco;

import com.svbgua.model.Modelos.Concierto;
import com.svbgua.ui.Fxml;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/** Modal "Elige tu concierto" (modal-concierto.fxml): cuadrícula compacta de tarjetas. */
public class ModalConciertoController {

    public record Opcion(Concierto concierto, String nombre, String detalle) {}

    private static final double GAP = 10;
    private static final double ALTO_TARJETA = 104;

    @FXML private VBox raiz;
    @FXML private ScrollPane scroll;
    @FXML private TilePane lista;

    public VBox raiz() {
        return raiz;
    }

    public void configurar(List<Opcion> opciones, double anchoVentana, double altoVentana,
                           Consumer<Concierto> alElegir) {
        int cols = anchoVentana >= 1100 ? 3 : anchoVentana >= 700 ? 2 : 1;
        double ancho = cols == 1 ? Math.min(620, Math.max(320, anchoVentana - 100)) : cols == 2 ? 720 : 1000;
        double tile = (ancho - 20 - GAP * (cols - 1)) / cols;

        raiz.setPrefWidth(ancho);
        lista.setPrefColumns(cols);
        lista.setPrefTileWidth(tile);
        lista.setPrefTileHeight(ALTO_TARJETA);
        lista.getChildren().clear();
        for (Opcion o : opciones) {
            Fxml.Vista<ConciertoOpcionController> v = Fxml.cargar("kiosco/concierto-opcion");
            v.controlador().configurar(o.nombre(), o.detalle(), tile, () -> alElegir.accept(o.concierto()));
            lista.getChildren().add(v.raiz());
        }

        double filas = Math.ceil(opciones.size() / (double) cols);
        double visible = Math.min(filas * (ALTO_TARJETA + GAP), Math.max(ALTO_TARJETA + GAP, altoVentana - 300));
        scroll.setPrefViewportHeight(visible);
        scroll.setMinViewportHeight(visible);
    }

}
