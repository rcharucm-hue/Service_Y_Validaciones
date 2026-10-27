package com.svbgua.kiosco.datos;

import com.svbgua.kiosco.modelo.Localidad;
import com.svbgua.kiosco.modelo.Asiento;
import com.svbgua.kiosco.modelo.TipoLocalidad;
import com.svbgua.model.Modelos.Catalogo;
import com.svbgua.model.Modelos.Inventario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * Convierte el inventario del backend (una fila por localidad) en las zonas y mesas del mapa.
 * El backend maneja cantidades: en la localidad "Mesa" cada unidad del inventario es un ASIENTO
 * (mesas de 4, hasta 30 mesas). Cada asiento se vende por separado. Los asientos "ocupados" que se
 * muestran son tantos como falten para llegar a la disponibilidad real (se reparten con una
 * semilla fija para que no cambien de lugar entre recargas).
 */
public final class FabricaLocalidades {

    public static final int MAX_MESAS = 30;
    public static final int SILLAS_POR_MESA = 4;

    // Geometria apaisada (original) y vertical, en el espacio virtual de MapaRecintoView.
    private static final double[] VIP_H = {420, 152, 700, 152, 730, 232, 390, 232};
    private static final double[] PREF_H = {366, 252, 754, 252, 796, 342, 324, 342};
    private static final double[] GEN_H = {318, 360, 802, 360, 836, 560, 284, 560};
    private static final double[] VIP_V = {290, 150, 470, 150, 490, 270, 270, 270};
    private static final double[] PREF_V = {262, 288, 498, 288, 520, 420, 240, 420};
    private static final double[] GEN_V = {232, 438, 528, 438, 556, 700, 204, 700};

    private static final double[] FILAS_Y = {205, 285, 365, 445, 525};
    private static final double[] COLUMNAS_IZQUIERDA = {90, 160, 230};
    private static final double[] COLUMNAS_DERECHA = {910, 980, 1050};

    // Vertical: las mesas van a los lados de las zonas, igual que en horizontal.
    private static final double[] V_COLUMNAS_IZQUIERDA = {75, 145};
    private static final double[] V_COLUMNAS_DERECHA = {615, 685};
    private static final double V_FILA_INICIO = 215;
    private static final double V_FILA_PASO = 80;

    private FabricaLocalidades() {
    }

    public static List<Localidad> crear(List<Inventario> inventarios, Catalogo catalogo, boolean vertical) {
        List<Inventario> orden = new ArrayList<>(inventarios);
        orden.sort(Comparator.comparingInt(Inventario::id));

        List<Localidad> resultado = new ArrayList<>();
        EnumSet<TipoLocalidad> usados = EnumSet.noneOf(TipoLocalidad.class);
        List<Localidad> mesas = new ArrayList<>();

        for (Inventario inv : orden) {
            String nombre = catalogo.nombreLocalidad(inv.idLocalidad());
            TipoLocalidad tipo = TipoLocalidad.desdeNombre(nombre);
            if (!usados.add(tipo)) {
                continue; // el mapa tiene un solo lugar por tipo: se usa el primero
            }
            if (tipo.esMesa()) {
                mesas.addAll(crearAsientos(inv, vertical));
            } else {
                resultado.add(new Localidad(String.valueOf(inv.id()), nombre, tipo, inv.precio(), inv.total(),
                        inv.disponible(), puntos(tipo, vertical), inv.id()));
            }
        }
        resultado.addAll(mesas);
        return resultado;
    }

    private static double[] puntos(TipoLocalidad tipo, boolean vertical) {
        return switch (tipo) {
            case VIP -> vertical ? VIP_V : VIP_H;
            case PREFERENCIAL -> vertical ? PREF_V : PREF_H;
            default -> vertical ? GEN_V : GEN_H;
        };
    }

    private static List<Localidad> crearAsientos(Inventario inv, boolean vertical) {
        int total = Math.min(inv.total(), MAX_MESAS * SILLAS_POR_MESA);
        int libres = Math.min(inv.disponible(), total);
        int ocupados = total - libres;

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, new Random(inv.id() * 7919L));
        Set<Integer> agotados = new HashSet<>(indices.subList(0, ocupados));

        List<Localidad> asientos = new ArrayList<>();
        int mesas = (total + SILLAS_POR_MESA - 1) / SILLAS_POR_MESA;
        for (int m = 0; m < mesas; m++) {
            String id = String.format(Locale.ROOT, "M%02d", m + 1);
            int lado = m % 2;
            int k = m / 2;
            double x;
            double y;
            if (vertical) {
                double[] columnas = lado == 0 ? V_COLUMNAS_IZQUIERDA : V_COLUMNAS_DERECHA;
                x = columnas[k % columnas.length];
                y = V_FILA_INICIO + V_FILA_PASO * (k / columnas.length);
            } else {
                double[] columnas = lado == 0 ? COLUMNAS_IZQUIERDA : COLUMNAS_DERECHA;
                x = columnas[k % 3];
                y = FILAS_Y[Math.min(k / 3, FILAS_Y.length - 1)];
            }
            int sillas = Math.min(SILLAS_POR_MESA, total - m * SILLAS_POR_MESA);
            for (int a = 0; a < sillas; a++) {
                int indice = m * SILLAS_POR_MESA + a;
                asientos.add(new Asiento(id, a + 1, sillas, inv.precio(), !agotados.contains(indice), x, y, inv.id()));
            }
        }
        return asientos;
    }
}
