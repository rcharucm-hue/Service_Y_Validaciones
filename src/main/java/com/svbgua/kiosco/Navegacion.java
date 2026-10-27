package com.svbgua.kiosco;

/** Lo que los pasos del autoservicio le piden al controlador principal. */
public interface Navegacion {
    void irPaso1();

    void irPaso2();

    void irPaso3();

    void pagar();

    void nuevaCompra();

    void aviso(String texto);
}
