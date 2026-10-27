package com.svbgua.kiosco.modelo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Estado de la selección del usuario. Es lógica pura (sin JavaFX): la vista solo
 * se suscribe con {@link #addListener(Runnable)} y se redibuja cuando algo cambia.
 *
 * Reglas:
 *  - Cada asiento de una mesa se selecciona por separado y cuenta una vez.
 *  - VIP/Preferencial/General admiten varios boletos (sin asientos individuales).
 *  - Una localidad no disponible nunca entra en la selección.
 */
public class SeleccionActual {

    private final Map<String, Localidad> localidades = new LinkedHashMap<>();
    private final Map<String, Integer> cantidades = new LinkedHashMap<>();
    private final List<Runnable> oyentes = new ArrayList<>();
    private Localidad activa;

    public void addListener(Runnable oyente) {
        oyentes.add(oyente);
    }

    private void notificar() {
        for (Runnable r : new ArrayList<>(oyentes)) {
            r.run();
        }
    }

    public boolean contiene(Localidad l) {
        return cantidades.containsKey(l.getId());
    }

    public int cantidadDe(Localidad l) {
        return cantidades.getOrDefault(l.getId(), 0);
    }

    /** Localidad que se muestra en el panel de detalle (puede ser null). */
    public Localidad getActiva() {
        return activa;
    }

    public boolean estaVacia() {
        return cantidades.isEmpty();
    }

    /**
     * Toque sobre el mapa.
     *  - No disponible: se rechaza (devuelve false).
     *  - Asiento: alterna seleccionado / no seleccionado.
     *  - Zona: si no estaba, se agrega con 1 boleto; si ya estaba, pasa a ser la activa.
     */
    public boolean tocar(Localidad l) {
        if (!l.estaDisponible()) {
            return false;
        }
        if (!contiene(l)) {
            localidades.put(l.getId(), l);
            cantidades.put(l.getId(), 1);
            activa = l;
        } else if (l.getTipo().esMesa()) {
            quitarInterno(l);
        } else {
            activa = l;
        }
        notificar();
        return true;
    }

    public void activar(Localidad l) {
        if (contiene(l)) {
            activa = l;
            notificar();
        }
    }

    /** @return false si no se pudo (no es zona, no está seleccionada o llegó al límite). */
    public boolean incrementar(Localidad l) {
        if (!contiene(l) || l.getTipo().esMesa()) {
            return false;
        }
        int n = cantidadDe(l);
        if (n >= l.getLimiteSeleccion()) {
            return false;
        }
        cantidades.put(l.getId(), n + 1);
        activa = l;
        notificar();
        return true;
    }

    /** Reduce una unidad; al llegar a cero la localidad sale de la selección. */
    public void decrementar(Localidad l) {
        if (!contiene(l) || l.getTipo().esMesa()) {
            return;
        }
        int n = cantidadDe(l) - 1;
        if (n <= 0) {
            quitarInterno(l);
        } else {
            cantidades.put(l.getId(), n);
            activa = l;
        }
        notificar();
    }

    public void quitar(Localidad l) {
        if (contiene(l)) {
            quitarInterno(l);
            notificar();
        }
    }

    public void limpiar() {
        localidades.clear();
        cantidades.clear();
        activa = null;
        notificar();
    }

    private void quitarInterno(Localidad l) {
        cantidades.remove(l.getId());
        localidades.remove(l.getId());
        if (activa != null && activa.getId().equals(l.getId())) {
            activa = null;
            for (Localidad otra : localidades.values()) {
                activa = otra; // queda la última seleccionada
            }
        }
    }

    public List<LineaSeleccion> lineas() {
        List<LineaSeleccion> r = new ArrayList<>();
        for (Map.Entry<String, Localidad> e : localidades.entrySet()) {
            r.add(new LineaSeleccion(e.getValue(), cantidades.get(e.getKey())));
        }
        return r;
    }

    /** Boletos de zonas (VIP/Preferencial/General). Las mesas se cuentan aparte. */
    public int totalBoletos() {
        int t = 0;
        for (LineaSeleccion l : lineas()) {
            if (!l.localidad().getTipo().esMesa()) {
                t += l.cantidad();
            }
        }
        return t;
    }

    public int totalAsientos() {
        int t = 0;
        for (LineaSeleccion l : lineas()) {
            if (l.localidad().getTipo().esMesa()) {
                t++;
            }
        }
        return t;
    }

    public BigDecimal total() {
        BigDecimal t = BigDecimal.ZERO;
        for (LineaSeleccion l : lineas()) {
            t = t.add(l.subtotal());
        }
        return t;
    }
}
