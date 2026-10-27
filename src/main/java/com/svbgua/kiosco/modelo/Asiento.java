package com.svbgua.kiosco.modelo;

import java.math.BigDecimal;

/** Asiento individual de una mesa: se vende por separado (1 unidad del inventario). */
public class Asiento extends Localidad {

    private final String mesaId;
    private final int numero;
    private final int sillasMesa;
    private final double centroX;
    private final double centroY;

    public Asiento(String mesaId, int numero, int sillasMesa, BigDecimal precio, boolean disponible,
                   double centroX, double centroY, int idInventario) {
        super(mesaId + "-A" + numero, "Mesa " + mesaId + " · Asiento " + numero, TipoLocalidad.MESA, precio, 1,
                disponible ? 1 : 0, new double[0], idInventario);
        this.mesaId = mesaId;
        this.numero = numero;
        this.sillasMesa = sillasMesa;
        this.centroX = centroX;
        this.centroY = centroY;
    }

    public String getMesaId() {
        return mesaId;
    }

    public int getNumero() {
        return numero;
    }

    public int getSillasMesa() {
        return sillasMesa;
    }

    public double getCentroX() {
        return centroX;
    }

    public double getCentroY() {
        return centroY;
    }
}
