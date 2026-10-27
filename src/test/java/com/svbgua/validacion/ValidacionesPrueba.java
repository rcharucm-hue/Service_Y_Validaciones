package com.svbgua.validacion;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pruebas sin dependencias externas (adaptadas de Service_Y_Validaciones).
 * Ejecutar:  javac -d out src/main/java/com/svbgua/validacion/*.java src/test/java/com/svbgua/validacion/*.java
 *            java -cp out com.svbgua.validacion.ValidacionesPrueba
 */
public class ValidacionesPrueba {
    private static int pruebas;

    private static void rechaza(Runnable accion) {
        try {
            accion.run();
        } catch (ValidacionException e) {
            pruebas++;
            return;
        }
        throw new AssertionError("Se aceptaron datos inválidos");
    }

    private static void acepta(Runnable accion) {
        accion.run();
        pruebas++;
    }

    public static void main(String[] args) {
        // artistas
        rechaza(() -> Validaciones.artista(" ", "Rock", null));
        acepta(() -> Validaciones.artista("Artista", "Rock", null));
        rechaza(() -> Validaciones.artista("x".repeat(121), "Rock", null));
        rechaza(() -> Validaciones.artista("A", "Rock", "p".repeat(51)));

        // dinero
        rechaza(() -> Validaciones.dinero(new BigDecimal("0"), "Precio"));
        rechaza(() -> Validaciones.dinero(new BigDecimal("0.001"), "Precio"));
        rechaza(() -> Validaciones.dinero(new BigDecimal("-5.00"), "Precio"));
        rechaza(() -> Validaciones.dinero(new BigDecimal("100000000.00"), "Precio"));
        acepta(() -> Validaciones.dinero(new BigDecimal("99999999.99"), "Precio"));
        acepta(() -> Validaciones.dinero(new BigDecimal("150"), "Precio"));

        // inventario
        rechaza(() -> Validaciones.inventario(1, 1, new BigDecimal("25.00"), 10, 11));
        rechaza(() -> Validaciones.inventario(1, 1, new BigDecimal("25.00"), 10, -1));
        rechaza(() -> Validaciones.inventario(0, 1, new BigDecimal("25.00"), 10, 5));
        acepta(() -> Validaciones.inventario(1, 1, new BigDecimal("25.00"), 10, 0));

        // conciertos
        rechaza(() -> Validaciones.concierto(1, "Concierto", "2026-02-30T20:00:00", "Recinto", "programado"));
        rechaza(() -> Validaciones.concierto(1, "Concierto", "2026-11-20T20:00:00-06:00", "Recinto", "CANCELADO"));
        rechaza(() -> Validaciones.concierto(1, "Concierto", "2026-11-20T20:00:00-06:00", " ", "programado"));
        rechaza(() -> Validaciones.concierto(0, "Concierto", "2026-11-20T20:00:00-06:00", "Recinto", "programado"));
        acepta(() -> Validaciones.concierto(1, "Concierto", "2026-11-20T20:00:00-06:00", "Recinto", "programado"));
        acepta(() -> Validaciones.concierto(1, "Concierto", "2026-11-20T20:00:00", "Recinto", null));

        // localidades
        rechaza(() -> Validaciones.localidad(""));
        acepta(() -> Validaciones.localidad("VIP de pie"));

        // usuarios y credenciales
        rechaza(() -> Validaciones.usuario("a:b", "Ana", "vendedor", "12345678", true));
        rechaza(() -> Validaciones.usuario("ana", "Ana", "superusuario", "12345678", true));
        rechaza(() -> Validaciones.usuario("ana", "Ana", "vendedor", "corta", true));
        rechaza(() -> Validaciones.usuario("ana", "Ana", "vendedor", "ñ".repeat(37), true)); // 74 bytes
        acepta(() -> Validaciones.usuario("ana", "Ana", "vendedor", "", false));            // editar sin cambiar clave
        acepta(() -> Validaciones.usuario("ana", "Ana", "administrador", "12345678", true));
        rechaza(() -> Validaciones.credenciales("ana:x", "clave"));
        rechaza(() -> Validaciones.credenciales("ana", ""));
        acepta(() -> Validaciones.credenciales("ana", "clave"));

        // URL del servidor
        acepta(() -> Validaciones.urlServidor("https://svb-gua-backend.onrender.com"));
        acepta(() -> Validaciones.urlServidor("https://svb-gua-backend.onrender.com/"));
        acepta(() -> Validaciones.urlServidor("http://localhost:3000"));
        rechaza(() -> Validaciones.urlServidor("http://svb-gua-backend.onrender.com"));
        rechaza(() -> Validaciones.urlServidor("https://usuario:clave@servidor.com"));
        rechaza(() -> Validaciones.urlServidor("https://servidor.com/api"));
        rechaza(() -> Validaciones.urlServidor("ftp://servidor.com"));
        rechaza(() -> Validaciones.urlServidor(null));

        // ventas con varios boletos
        Map<Integer, Integer> ok = new LinkedHashMap<>();
        ok.put(1, 2);
        ok.put(2, 4);
        acepta(() -> Validaciones.detallesVenta(ok));
        rechaza(() -> Validaciones.detallesVenta(Map.of()));
        rechaza(() -> Validaciones.detallesVenta(null));
        rechaza(() -> Validaciones.detallesVenta(Map.of(1, 0)));
        rechaza(() -> Validaciones.detallesVenta(Map.of(-3, 1)));
        Map<Integer, Integer> muchos = new LinkedHashMap<>();
        for (int i = 1; i <= 51; i++) muchos.put(i, 1);
        rechaza(() -> Validaciones.detallesVenta(muchos));

        // subtotal exacto (0.10 x 3 no debe dar 0.30000000000000004)
        acepta(() -> Validaciones.subtotal(new BigDecimal("0.10"), 3, new BigDecimal("0.30")));
        rechaza(() -> Validaciones.subtotal(new BigDecimal("0.10"), 3, new BigDecimal("0.31")));

        System.out.println(pruebas + " pruebas de validación correctas.");
    }
}
