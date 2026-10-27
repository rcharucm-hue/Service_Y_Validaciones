package com.svbgua.kiosco.modelo;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Localidad del recinto. No contiene lógica de dibujo: solo datos y la geometría
 * (puntos x,y en el espacio virtual del mapa) para que la vista la represente.
 */
public class Localidad {

    /** Máximo de boletos por localidad en una compra (regla de demostración). */
    public static final int LIMITE_POR_COMPRA = 10;

    private final String id;
    private final String nombre;
    private final TipoLocalidad tipo;
    private final BigDecimal precio;
    private final int capacidad;
    private final int disponibles;
    private final double[] puntos;
    private final int idInventario;

    public Localidad(String id, String nombre, TipoLocalidad tipo, BigDecimal precio,
                     int capacidad, int disponibles, double[] puntos, int idInventario) {
        this.id = Objects.requireNonNull(id);
        this.nombre = Objects.requireNonNull(nombre);
        this.tipo = Objects.requireNonNull(tipo);
        this.precio = Objects.requireNonNull(precio);
        this.capacidad = capacidad;
        this.disponibles = Math.max(0, Math.min(disponibles, capacidad));
        this.puntos = puntos.clone();
        this.idInventario = idInventario;
    }

    /** Id del registro de inventario en el backend (una mesa y su zona comparten el de su tipo). */
    public int getIdInventario() {
        return idInventario;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoLocalidad getTipo() {
        return tipo;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public int getDisponibles() {
        return disponibles;
    }

    public double[] getPuntos() {
        return puntos.clone();
    }

    public EstadoLocalidad getEstado() {
        return disponibles > 0 ? EstadoLocalidad.DISPONIBLE : EstadoLocalidad.NO_DISPONIBLE;
    }

    public boolean estaDisponible() {
        return getEstado() == EstadoLocalidad.DISPONIBLE;
    }

    /** Cantidad máxima que se puede seleccionar de esta localidad. */
    public int getLimiteSeleccion() {
        return tipo.esMesa() ? 1 : Math.min(disponibles, LIMITE_POR_COMPRA);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Localidad otra && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return nombre + " (" + id + ")";
    }
}
