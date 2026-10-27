package com.svbgua.kiosco;

import com.svbgua.kiosco.modelo.InfoConcierto;
import com.svbgua.kiosco.modelo.LineaSeleccion;
import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.modelo.SeleccionActual;
import com.svbgua.model.Modelos.Venta;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.List;

/** Estado compartido entre los pasos del autoservicio (selección, datos de factura y de tarjeta, última venta). */
public class SesionKiosco {

    public final SeleccionActual seleccion = new SeleccionActual();

    // Datos escritos con los teclados en pantalla
    public final StringProperty nit = new SimpleStringProperty("");
    public final StringProperty tarjeta = new SimpleStringProperty("");
    public final StringProperty titular = new SimpleStringProperty("");
    public final StringProperty cvv = new SimpleStringProperty("");
    public final StringProperty correo = new SimpleStringProperty("");
    public boolean usaNit;

    public InfoConcierto info;
    public List<Localidad> localidades = List.of();

    // Comprobante
    public Venta ultimaVenta;
    public List<LineaSeleccion> ultimasLineas = List.of();
    public String ultimaTarjeta = "";
    public String ultimoTitular = "";
    public String ultimaFactura = "";
    public String ultimoQrTexto = "";

    public String facturaTexto() {
        return usaNit ? "NIT " + formatoNit(nit.get()) : "CF · Consumidor Final";
    }

    public static String formatoNit(String raw) {
        return raw.length() > 1 ? raw.substring(0, raw.length() - 1) + "-" + raw.substring(raw.length() - 1) : raw;
    }

    public static String agrupar(String digitos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digitos.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append(' ');
            }
            sb.append(digitos.charAt(i));
        }
        return sb.toString();
    }
}
