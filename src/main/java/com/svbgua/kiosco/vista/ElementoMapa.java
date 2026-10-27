package com.svbgua.kiosco.vista;

import com.svbgua.kiosco.modelo.Localidad;
import java.util.function.Consumer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Elemento interactivo del mapa (zona o mesa). Responde a ratón y tacto: JavaFX
 * convierte los toques en eventos de ratón sintetizados, por lo que un único
 * manejador de clic cubre ambos. Solo cuenta como toque si el dedo no se arrastró
 * ({@code isStillSincePress}), lo que evita selecciones accidentales al desplazarse.
 *
 * La animación de selección siempre vuelve a translateY=0 y escala=1 al deseleccionar,
 * así que el área de interacción nunca queda desalineada.
 */
public abstract class ElementoMapa extends Group {

    protected static final Color SOMBRA_BASE = Color.rgb(0, 0, 0, 0.5);

    protected final Localidad localidad;
    protected final DropShadow efecto = new DropShadow(BlurType.GAUSSIAN, SOMBRA_BASE, 14, 0.0, 0, 6);
    protected boolean seleccionado;
    protected boolean hover;

    private Timeline animacion;
    private Consumer<Localidad> alTocar;

    protected ElementoMapa(Localidad localidad) {
        this.localidad = localidad;
        setCursor(Cursor.HAND);
        setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || !e.isStillSincePress()) {
                return;
            }
            e.consume();
            if (alTocar != null) {
                alTocar.accept(this.localidad);
            }
        });
        setOnMouseEntered(e -> {
            hover = true;
            refrescarColores();
        });
        setOnMouseExited(e -> {
            hover = false;
            refrescarColores();
        });
    }

    public Localidad getLocalidad() {
        return localidad;
    }

    public void setAlTocar(Consumer<Localidad> alTocar) {
        this.alTocar = alTocar;
    }

    public void sacudir() {
        EfectosVisuales.sacudir(this);
    }

    /** Sincroniza el aspecto con la selección. Solo anima cuando el estado cambia. */
    public final void actualizar(boolean estaSeleccionado, int cantidad) {
        boolean cambio = estaSeleccionado != seleccionado;
        seleccionado = estaSeleccionado;
        aplicarCantidad(cantidad);
        refrescarColores();
        if (cambio) {
            animarSeleccion(estaSeleccionado);
            alCambiarSeleccion(estaSeleccionado);
        }
    }

    protected abstract void aplicarCantidad(int cantidad);

    protected abstract void refrescarColores();

    protected abstract double elevacion();

    protected abstract double escala();

    /** Gancho opcional para animaciones propias del subtipo. */
    protected void alCambiarSeleccion(boolean estaSeleccionado) {
    }

    private void animarSeleccion(boolean sel) {
        if (animacion != null) {
            animacion.stop();
        }
        double esc = escala();
        Color brillo = Paleta.de(localidad.getTipo());

        KeyFrame fin = new KeyFrame(Duration.millis(260),
                new KeyValue(translateYProperty(), sel ? -elevacion() : 0.0, Interpolator.EASE_BOTH),
                new KeyValue(scaleXProperty(), sel ? esc : 1.0, Interpolator.EASE_BOTH),
                new KeyValue(scaleYProperty(), sel ? esc : 1.0, Interpolator.EASE_BOTH),
                new KeyValue(efecto.colorProperty(), sel ? brillo : SOMBRA_BASE, Interpolator.EASE_BOTH),
                new KeyValue(efecto.radiusProperty(), sel ? 38.0 : 14.0, Interpolator.EASE_BOTH),
                new KeyValue(efecto.spreadProperty(), sel ? 0.30 : 0.0, Interpolator.EASE_BOTH),
                new KeyValue(efecto.offsetYProperty(), sel ? 0.0 : 6.0, Interpolator.EASE_BOTH));

        if (sel) {
            toFront(); // que el resplandor no quede tapado por zonas vecinas
            // pequeño "rebote" de escala antes de asentarse
            KeyFrame rebote = new KeyFrame(Duration.millis(120),
                    new KeyValue(scaleXProperty(), esc + 0.035, Interpolator.EASE_OUT),
                    new KeyValue(scaleYProperty(), esc + 0.035, Interpolator.EASE_OUT));
            animacion = new Timeline(rebote, fin);
        } else {
            animacion = new Timeline(fin);
        }
        animacion.play();
    }
}
