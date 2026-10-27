package com.svbgua.kiosco.modelo;

import java.math.BigDecimal;

/** Una línea del resumen: localidad, cantidad y subtotal. */
public record LineaSeleccion(Localidad localidad, int cantidad) {

    public BigDecimal subtotal() {
        return localidad.getPrecio().multiply(BigDecimal.valueOf(cantidad));
    }
}
