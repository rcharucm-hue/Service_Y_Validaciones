package com.svbgua.kiosco;

import com.svbgua.kiosco.modelo.Asiento;
import com.svbgua.kiosco.modelo.LineaSeleccion;
import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.modelo.SeleccionActual;
import com.svbgua.kiosco.util.Formato;
import com.svbgua.kiosco.vista.Paleta;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;
import java.util.function.Consumer;

/**
 * Panel lateral (panel-seleccion.fxml): detalle de la localidad activa (precio, disponibilidad, cantidad,
 * subtotal) y resumen de toda la selección. No contiene lógica de negocio: delega
 * en {@link SeleccionActual} y se redibuja cuando se llama a {@link #refrescar()}.
 */
public class PanelSeleccionController {

    @FXML private VBox raiz;
    @FXML private HBox cuerpo;
    @FXML private VBox colIzq;
    @FXML private VBox colDer;
    @FXML private Label titulo;
    @FXML private Label tituloResumen;
    @FXML private VBox tarjetaActiva;
    @FXML private ScrollPane scroll;
    @FXML private VBox listaResumen;

    private SeleccionActual seleccion;
    private Consumer<String> alAviso = s -> { };

    public void iniciar(SeleccionActual seleccion, Consumer<String> alAviso) {
        this.seleccion = seleccion;
        this.alAviso = alAviso;
    }

    /** En pantallas verticales la tarjeta activa y el resumen van lado a lado. */
    public void setHorizontal(boolean horizontal) {
        raiz.getChildren().clear();
        colIzq.getChildren().clear();
        colDer.getChildren().clear();
        cuerpo.getChildren().clear();
        if (!horizontal) {
            raiz.getChildren().addAll(titulo, tarjetaActiva, tituloResumen, scroll);
            return;
        }
        colIzq.getChildren().addAll(titulo, tarjetaActiva);
        colDer.getChildren().addAll(tituloResumen, scroll);
        cuerpo.getChildren().addAll(colIzq, colDer);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        raiz.getChildren().add(cuerpo);
    }

    public void refrescar() {
        if (seleccion == null) {
            return;
        }
        construirTarjeta();
        construirResumen();
    }

    // ------------------------------------------------------------------ tarjeta activa

    private void construirTarjeta() {
        tarjetaActiva.getChildren().clear();
        Localidad l = seleccion.getActiva();
        if (l == null) {
            Label vacio = new Label("Toca una zona o una mesa del mapa para comenzar.");
            vacio.getStyleClass().add("panel-vacio");
            vacio.setWrapText(true);
            tarjetaActiva.getChildren().add(vacio);
            return;
        }

        Color color = Paleta.de(l.getTipo());
        Label nombre = new Label(l.getNombre());
        nombre.getStyleClass().add("activa-nombre");
        HBox cabecera = new HBox(10, new Circle(9, color), nombre);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        Label tipo = new Label(l.getTipo().getDescripcion());
        tipo.getStyleClass().add("activa-tipo");
        tipo.setWrapText(true);

        tarjetaActiva.getChildren().addAll(cabecera, tipo,
                fila("Precio unitario", Formato.moneda(l.getPrecio())));

        int cantidad = seleccion.cantidadDe(l);

        if (l instanceof Asiento a) {
            tarjetaActiva.getChildren().addAll(
                    fila("Disponibilidad", "Disponible"),
                    fila("Ubicación", "Mesa " + a.getMesaId() + " · Asiento " + a.getNumero()));
        } else {
            tarjetaActiva.getChildren().addAll(
                    fila("Disponibles", l.getDisponibles() + " de " + l.getCapacidad()),
                    crearSelectorCantidad(l, cantidad));
        }

        Label etiquetaSub = new Label("SUBTOTAL");
        etiquetaSub.getStyleClass().add("subtotal-etiqueta");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label valorSub = new Label(Formato.moneda(new LineaSeleccion(l, cantidad).subtotal()));
        valorSub.getStyleClass().add("subtotal-valor");
        HBox subtotal = new HBox(etiquetaSub, espacio, valorSub);
        subtotal.setAlignment(Pos.CENTER_LEFT);

        Button quitar = new Button(l instanceof Asiento ? "Quitar asiento" : "Quitar de la selección");
        quitar.getStyleClass().add("boton-secundario");
        quitar.setMaxWidth(Double.MAX_VALUE);
        quitar.setOnAction(e -> seleccion.quitar(l));

        tarjetaActiva.getChildren().addAll(subtotal, quitar);
    }

    private HBox crearSelectorCantidad(Localidad l, int cantidad) {
        Button menos = new Button("−");
        menos.getStyleClass().add("boton-cantidad");
        menos.setOnAction(e -> seleccion.decrementar(l));

        Label valor = new Label(String.valueOf(cantidad));
        valor.getStyleClass().add("cantidad-valor");

        Button mas = new Button("+");
        mas.getStyleClass().add("boton-cantidad");
        mas.setDisable(cantidad >= l.getLimiteSeleccion());
        mas.setOnAction(e -> {
            if (!seleccion.incrementar(l)) {
                alAviso.accept("Máximo " + l.getLimiteSeleccion() + " boletos por compra en " + l.getNombre());
            }
        });

        HBox caja = new HBox(18, menos, valor, mas);
        caja.setAlignment(Pos.CENTER);
        return caja;
    }

    private HBox fila(String etiqueta, String valor) {
        Label a = new Label(etiqueta);
        a.getStyleClass().add("dato-etiqueta");
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        Label b = new Label(valor);
        b.getStyleClass().add("dato-valor");
        HBox h = new HBox(a, espacio, b);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ------------------------------------------------------------------ resumen

    private void construirResumen() {
        listaResumen.getChildren().clear();
        List<LineaSeleccion> lineas = seleccion.lineas();
        if (lineas.isEmpty()) {
            Label vacio = new Label("Aún no has elegido localidades.");
            vacio.getStyleClass().add("panel-vacio-resumen");
            listaResumen.getChildren().add(vacio);
            return;
        }
        for (LineaSeleccion ln : lineas) {
            listaResumen.getChildren().add(crearLinea(ln));
        }
    }

    private HBox crearLinea(LineaSeleccion ln) {
        Localidad l = ln.localidad();
        Label nombre = new Label(l.getNombre());
        nombre.getStyleClass().add("linea-nombre");
        String textoDetalle = l instanceof Asiento
                ? "1 asiento"
                : ln.cantidad() + " × " + Formato.monedaCorta(l.getPrecio());
        Label detalle = new Label(textoDetalle);
        detalle.getStyleClass().add("linea-detalle");
        VBox textos = new VBox(2, nombre, detalle);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);

        Label sub = new Label(Formato.moneda(ln.subtotal()));
        sub.getStyleClass().add("linea-subtotal");

        Button quitar = new Button("×");
        quitar.getStyleClass().add("boton-quitar-linea");
        quitar.setOnAction(e -> seleccion.quitar(l));

        HBox fila = new HBox(12, new Circle(7, Paleta.de(l.getTipo())), textos, espacio, sub, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().add("linea-resumen");
        if (seleccion.getActiva() != null && seleccion.getActiva().equals(l)) {
            fila.getStyleClass().add("linea-resumen-activa");
        }
        fila.setOnMouseClicked(e -> {
            if (e.isStillSincePress()) {
                seleccion.activar(l);
            }
        });
        return fila;
    }
}
