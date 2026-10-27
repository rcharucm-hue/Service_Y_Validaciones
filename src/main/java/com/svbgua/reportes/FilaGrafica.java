package com.svbgua.reportes;
import java.math.BigDecimal;
/** Dato agregado para el dataset de graficas Jasper. */
public class FilaGrafica {
    private final String serie, categoria;
    private final BigDecimal valor;
    public FilaGrafica(String serie, String categoria, BigDecimal valor) {
        this.serie=serie; this.categoria=categoria; this.valor=valor;
    }
    public String getSerie() { return serie; }
    public String getCategoria() { return categoria; }
    public BigDecimal getValor() { return valor; }
}
