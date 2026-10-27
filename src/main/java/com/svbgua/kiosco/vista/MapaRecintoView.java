package com.svbgua.kiosco.vista;

import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.modelo.Asiento;
import com.svbgua.kiosco.modelo.SeleccionActual;
import com.svbgua.kiosco.util.Formato;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Mapa interactivo del recinto. Dibuja en un espacio virtual fijo (1120 x 700) y lo
 * escala para caber en el contenedor, de modo que la geometría y las áreas táctiles
 * siempre coinciden con lo que se ve.
 */
public class MapaRecintoView extends StackPane {

    /** Mapa apaisado (pantallas anchas). */
    public static final double ANCHO_H = 1120;
    public static final double ALTO_H = 700;
    /** Mapa vertical (pantallas de autoservicio en posicion vertical). */
    public static final double ANCHO_V = 760;
    public static final double ALTO_V = 870;

    private final boolean vertical;
    private final double ANCHO;
    private final double ALTO;

    private final Pane lienzo = new Pane();
    private final Map<String, ElementoMapa> elementos = new LinkedHashMap<>();

    public MapaRecintoView(List<Localidad> localidades, boolean vertical) {
        this.vertical = vertical;
        this.ANCHO = vertical ? ANCHO_V : ANCHO_H;
        this.ALTO = vertical ? ALTO_V : ALTO_H;
        setMinSize(0, 0); // permite encoger por debajo del tamaño virtual
        lienzo.setMinSize(ANCHO, ALTO);
        lienzo.setPrefSize(ANCHO, ALTO);
        lienzo.setMaxSize(ANCHO, ALTO);
        getChildren().add(lienzo);

        construirPiso();
        construirSectoresMesas(localidades);

        EscenarioView escenario = new EscenarioView();
        double dx = ANCHO / 2.0 - 560.0; // el escenario esta dibujado centrado en x = 560
        escenario.setTranslateX(dx);
        escenario.getHaces().setTranslateX(dx);
        lienzo.getChildren().addAll(escenario.getHaces(), escenario);

        // Mesas (decorativas) y luego sus asientos, que son lo que se toca y se vende.
        Map<String, double[]> mesas = new LinkedHashMap<>();
        Map<String, Boolean> hayLibres = new LinkedHashMap<>();
        for (Localidad l : localidades) {
            if (l instanceof Asiento a) {
                mesas.putIfAbsent(a.getMesaId(), new double[]{a.getCentroX(), a.getCentroY()});
                hayLibres.merge(a.getMesaId(), a.estaDisponible(), Boolean::logicalOr);
            }
        }
        for (Map.Entry<String, double[]> m : mesas.entrySet()) {
            lienzo.getChildren().add(new MesaNode(m.getKey(), m.getValue()[0], m.getValue()[1],
                    hayLibres.get(m.getKey())));
        }

        for (Localidad l : localidades) {
            ElementoMapa e = (l instanceof Asiento a) ? new SillaNode(a) : new ZonaNode(l);
            e.actualizar(false, 0);
            elementos.put(l.getId(), e);
            lienzo.getChildren().add(e);
        }

        widthProperty().addListener((o, a, b) -> ajustarEscala());
        heightProperty().addListener((o, a, b) -> ajustarEscala());
    }

    private void ajustarEscala() {
        double e = Math.min(getWidth() / ANCHO, getHeight() / ALTO);
        if (e > 0) {
            lienzo.setScaleX(e); // el pivote por defecto es el centro: queda centrado
            lienzo.setScaleY(e);
        }
    }

    public void setAlTocar(Consumer<Localidad> alTocar) {
        for (ElementoMapa e : elementos.values()) {
            e.setAlTocar(alTocar);
        }
    }

    public void refrescar(SeleccionActual seleccion) {
        for (ElementoMapa e : elementos.values()) {
            e.actualizar(seleccion.contiene(e.getLocalidad()), seleccion.cantidadDe(e.getLocalidad()));
        }
    }

    public void sacudir(Localidad l) {
        ElementoMapa e = elementos.get(l.getId());
        if (e != null) {
            e.sacudir();
        }
    }

    private void construirPiso() {
        Rectangle piso = new Rectangle(0, 0, ANCHO, ALTO);
        piso.setArcWidth(40);
        piso.setArcHeight(40);
        piso.setFill(new RadialGradient(0, 0, 0.5, 0.12, 0.95, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2A1B4D")), new Stop(0.55, Color.web("#15112A")),
                new Stop(1, Color.web("#0A0914"))));
        piso.setStroke(Color.web("#2E2850"));
        piso.setStrokeWidth(2);
        piso.setMouseTransparent(true);
        lienzo.getChildren().add(piso);
    }

    private void construirSectoresMesas(List<Localidad> localidades) {
        String precio = null;
        for (Localidad l : localidades) {
            if (l instanceof Asiento) {
                precio = Formato.monedaCorta(l.getPrecio()) + " por asiento";
                break;
            }
        }
        if (precio == null) {
            return; // este concierto no tiene mesas
        }
        if (vertical) {
            sector(26, 190, 170, 815, "MESAS", precio);
            sector(570, 734, 170, 815, "MESAS", precio);
        } else {
            sector(36, 270, 150, 588, "MESAS · SECTOR IZQUIERDO", precio + " · 4 por mesa");
            sector(850, 1084, 150, 588, "MESAS · SECTOR DERECHO", precio + " · 4 por mesa");
        }
    }

    private void sector(double x1, double x2, double y1, double y2, String titulo, String precio) {
        Polygon p = new Polygon(x1, y1, x2, y1, x2, y2, x1, y2);
        p.setFill(Color.web("#FF5C7A", 0.07));
        p.setStroke(Color.web("#FF5C7A", 0.40));
        p.setStrokeWidth(1.5);
        p.setStrokeLineJoin(StrokeLineJoin.ROUND);
        p.getStrokeDashArray().addAll(10.0, 8.0);
        p.setMouseTransparent(true);
        double cx = (x1 + x2) / 2.0;
        lienzo.getChildren().addAll(p,
                EfectosVisuales.textoCentrado(titulo, cx, y1 - 12, Font.font("System", FontWeight.BOLD, 13),
                        Color.web("#FF8FA3")),
                EfectosVisuales.textoCentrado(precio, cx, y2 + 18, Font.font("System", FontWeight.NORMAL, 12),
                        Color.web("#C9B8D6")));
    }
}
