package com.svbgua.validacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Set;

/**
 * Reglas de validación del cliente (adaptadas del proyecto Service_Y_Validaciones). Espejan las restricciones de la
 * base de datos (longitudes, DECIMAL(10,2), estados y roles permitidos) para dar el error claro antes de viajar al
 * servidor. No depende de JavaFX ni de Jackson, así que se prueba con un simple {@code java}.
 */
public final class Validaciones {
    public static final Set<String> ROLES = Set.of("administrador", "vendedor");
    public static final Set<String> ESTADOS_CONCIERTO = Set.of("programado", "activo", "finalizado");
    public static final int MAX_TIPOS_POR_VENTA = 50;
    public static final BigDecimal MAX_DINERO = new BigDecimal("99999999.99");

    private Validaciones() {}

    private static void exigir(boolean cumple, String mensaje) {
        if (!cumple) throw new ValidacionException(mensaje);
    }

    // ------------------------------------------------------------ primitivas

    public static void id(int valor, String campo) {
        exigir(valor > 0, campo + " debe ser un entero positivo.");
    }

    public static void texto(String valor, String campo, int maximo, boolean obligatorio) {
        exigir(!obligatorio || (valor != null && !valor.isBlank()), campo + " es obligatorio.");
        exigir(valor == null || valor.trim().length() <= maximo, campo + " supera " + maximo + " caracteres.");
    }

    public static void opcion(String valor, String campo, Set<String> opciones) {
        exigir(valor != null && opciones.contains(valor), campo + " debe ser uno de: " + opciones);
    }

    /** Mayor que cero, máximo dos decimales y cabe en DECIMAL(10,2). */
    public static void dinero(BigDecimal valor, String campo) {
        exigir(valor != null && valor.signum() > 0, campo + " debe ser mayor que cero.");
        try {
            BigDecimal exacto = valor.setScale(2, RoundingMode.UNNECESSARY);
            exigir(exacto.compareTo(MAX_DINERO) <= 0, campo + " supera DECIMAL(10,2).");
        } catch (ArithmeticException e) {
            throw new ValidacionException(campo + " admite como máximo dos decimales.");
        }
    }

    /** ISO 8601 con o sin zona horaria. */
    public static void fecha(String valor, String campo, boolean obligatorio) {
        if (valor == null || valor.isBlank()) {
            exigir(!obligatorio, campo + " es obligatorio.");
            return;
        }
        try {
            OffsetDateTime.parse(valor);
        } catch (DateTimeParseException e) {
            try {
                LocalDateTime.parse(valor);
            } catch (DateTimeParseException error) {
                throw new ValidacionException(campo + " debe usar fecha y hora ISO 8601.");
            }
        }
    }

    // --------------------------------------------------------------- entidades

    public static void artista(String nombre, String genero, String pais) {
        texto(nombre, "Nombre artístico", 120, true);
        texto(genero, "Género musical", 50, true);
        texto(pais, "País de origen", 50, false);
    }

    public static void concierto(int idArtista, String titulo, String fechaIso, String recinto, String estado) {
        id(idArtista, "Artista");
        texto(titulo, "Título", 150, true);
        fecha(fechaIso, "Fecha del concierto", true);
        texto(recinto, "Recinto", 120, true);
        if (estado != null) opcion(estado, "Estado", ESTADOS_CONCIERTO);
    }

    public static void localidad(String nombre) {
        texto(nombre, "Localidad", 50, true);
    }

    public static void inventario(int idConcierto, int idLocalidad, BigDecimal precio, int total, int disponible) {
        id(idConcierto, "Concierto");
        id(idLocalidad, "Localidad");
        dinero(precio, "Precio");
        id(total, "Cantidad total");
        exigir(disponible >= 0, "Cantidad disponible debe ser un entero no negativo.");
        exigir(disponible <= total, "Cantidad disponible no puede superar la cantidad total.");
    }

    /**
     * @param contrasena obligatoria al crear; al editar puede venir vacía (= no se cambia)
     */
    public static void usuario(String nombreUsuario, String nombreCompleto, String rol, String contrasena, boolean contrasenaObligatoria) {
        texto(nombreUsuario, "Nombre de usuario", 50, true);
        exigir(!nombreUsuario.contains(":"), "El nombre de usuario no puede contener dos puntos.");
        texto(nombreCompleto, "Nombre completo", 100, true);
        opcion(rol, "Rol", ROLES);
        if (contrasenaObligatoria || (contrasena != null && !contrasena.isEmpty())) contrasena(contrasena);
    }

    /** Al menos 8 caracteres y máximo 72 bytes UTF-8 (límite de bcrypt). */
    public static void contrasena(String contrasena) {
        exigir(contrasena != null && contrasena.length() >= 8
                        && contrasena.getBytes(StandardCharsets.UTF_8).length <= 72,
                "La contraseña debe tener al menos 8 caracteres y como máximo 72 bytes UTF-8.");
    }

    /** Credenciales para Basic Auth: el usuario no puede llevar ':' (separa usuario y clave). */
    public static void credenciales(String usuario, String contrasena) {
        exigir(usuario != null && !usuario.isBlank() && usuario.trim().length() <= 50 && !usuario.contains(":")
                        && contrasena != null && !contrasena.isEmpty()
                        && contrasena.getBytes(StandardCharsets.UTF_8).length <= 72,
                "Usuario o contraseña no válidos.");
    }

    /** HTTPS obligatorio, salvo HTTP en localhost. Sin credenciales, query ni fragmento embebidos en la URL. */
    public static void urlServidor(String url) {
        URI base;
        try {
            base = URI.create(url == null ? "" : url.trim().replaceAll("/+$", ""));
        } catch (IllegalArgumentException e) {
            throw new ValidacionException("La dirección del servidor no es válida.");
        }
        boolean local = base.getHost() != null && Set.of("localhost", "127.0.0.1", "[::1]").contains(base.getHost());
        boolean origenLimpio = base.getHost() != null && base.getUserInfo() == null && base.getQuery() == null
                && base.getFragment() == null && (base.getPath() == null || base.getPath().isEmpty());
        boolean esquemaSeguro = "https".equals(base.getScheme()) || (local && "http".equals(base.getScheme()));
        exigir(origenLimpio && esquemaSeguro, "La URL debe ser un origen HTTPS; HTTP solo se permite en localhost.");
    }

    // ------------------------------------------------------------------ ventas

    /** idInventario -> cantidad. Entre 1 y 50 tipos de boleto, todos con cantidad positiva. */
    public static void detallesVenta(Map<Integer, Integer> cantidades) {
        exigir(cantidades != null && !cantidades.isEmpty() && cantidades.size() <= MAX_TIPOS_POR_VENTA,
                "La venta debe incluir entre 1 y " + MAX_TIPOS_POR_VENTA + " tipos de boleto.");
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            id(e.getKey() == null ? 0 : e.getKey(), "Inventario");
            id(e.getValue() == null ? 0 : e.getValue(), "Cantidad");
        }
    }

    /** El subtotal debe ser exactamente precio unitario por cantidad. */
    public static void subtotal(BigDecimal precioUnitario, int cantidad, BigDecimal subtotal) {
        dinero(precioUnitario, "Precio unitario");
        id(cantidad, "Cantidad");
        dinero(subtotal, "Subtotal");
        exigir(precioUnitario.multiply(BigDecimal.valueOf(cantidad)).compareTo(subtotal) == 0,
                "Subtotal debe ser precio unitario por cantidad.");
    }
}
