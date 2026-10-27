package com.svbgua.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.svbgua.model.Modelos.*;
import com.svbgua.validacion.Validaciones;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Cliente HTTP del backend SVB-GUA (Basic Auth). Los metodos son bloqueantes: llamarlos fuera del hilo de JavaFX. */
public final class ApiClient {

    public static final ApiClient INSTANCE = new ApiClient();

    private static final ObjectMapper MAPPER = new ObjectMapper();
    // NEVER: una redirección no debe reenviar la cabecera Authorization (Basic) a otro destino.
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private String baseUrl = "https://svb-gua-backend.onrender.com";
    private String authHeader;
    private Usuario usuario;

    private ApiClient() {}

    public Usuario usuario() {
        return usuario;
    }

    public void cerrarSesion() {
        authHeader = null;
        usuario = null;
    }

    // ---------------------------------------------------------------- sesion

    public Usuario login(String url, String nombre, String contrasena) {
        Validaciones.urlServidor(url);
        Validaciones.credenciales(nombre, contrasena);
        baseUrl = url.trim().replaceAll("/+$", "");
        ObjectNode body = MAPPER.createObjectNode();
        body.put("nombre_usuario", nombre.trim());
        body.put("contrasena", contrasena);
        JsonNode r = request("POST", "/api/usuarios/login", body, false);
        JsonNode u = r.path("usuario");
        authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((nombre.trim() + ":" + contrasena).getBytes(StandardCharsets.UTF_8));
        usuario = new Usuario(u.path("id_usuario").asInt(), u.path("nombre_usuario").asText(),
                u.path("nombre_completo").asText(), u.path("rol").asText());
        return usuario;
    }

    /** Crea un usuario desde el login. El backend exige un administrador, por eso se envian sus credenciales. */
    public Usuario crearUsuario(String url, String adminUsuario, String adminContrasena,
                                String nombreUsuario, String nombreCompleto, String contrasena, String rol) {
        Validaciones.urlServidor(url);
        Validaciones.credenciales(adminUsuario, adminContrasena);
        Validaciones.usuario(nombreUsuario, nombreCompleto, rol, contrasena, true);
        String base = url.trim().replaceAll("/+$", "");
        String auth = "Basic " + Base64.getEncoder()
                .encodeToString((adminUsuario.trim() + ":" + adminContrasena).getBytes(StandardCharsets.UTF_8));
        ObjectNode body = MAPPER.createObjectNode();
        body.put("nombre_usuario", nombreUsuario.trim());
        body.put("nombre_completo", nombreCompleto.trim());
        body.put("contrasena", contrasena);
        body.put("rol", rol);
        return usuario(request(base, auth, "POST", "/api/usuarios/create", body));
    }

    // -------------------------------------------------------------- catalogo

    public Catalogo cargarCatalogo() {
        List<Concierto> conciertos = listarConciertos();
        Map<Integer, Localidad> localidades = new LinkedHashMap<>();
        for (Localidad l : listarLocalidades()) localidades.put(l.id(), l);
        Map<Integer, Artista> artistas = new LinkedHashMap<>();
        for (Artista a : listarArtistas()) artistas.put(a.id(), a);
        return new Catalogo(conciertos, localidades, artistas);
    }

    public List<Concierto> listarConciertos() {
        return lista(request("GET", "/api/conciertos", null, true), n -> new Concierto(
                n.path("id_concierto").asInt(), n.path("id_artista").asInt(), n.path("titulo_evento").asText(),
                n.path("fecha_concierto").asText(), n.path("recinto").asText(), n.path("estado").asText()));
    }

    public List<Localidad> listarLocalidades() {
        return lista(request("GET", "/api/localidades", null, true),
                n -> new Localidad(n.path("id_localidad").asInt(), n.path("nombre").asText()));
    }

    public Localidad crearLocalidad(String nombre) {
        Validaciones.localidad(nombre);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("nombre", nombre.trim());
        JsonNode n = request("POST", "/api/localidades/create", body, true);
        return new Localidad(n.path("id_localidad").asInt(), n.path("nombre").asText());
    }

    public Localidad actualizarLocalidad(int id, String nombre) {
        Validaciones.id(id, "Localidad");
        Validaciones.localidad(nombre);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("nombre", nombre.trim());
        request("PUT", "/api/localidades/update/" + id, body, true);
        return new Localidad(id, nombre.trim());
    }

    public void eliminarLocalidad(int id) {
        Validaciones.id(id, "Localidad");
        request("DELETE", "/api/localidades/delete/" + id, null, true);
    }

    // ------------------------------------------------------ artistas / conciertos

    public List<Artista> listarArtistas() {
        return lista(request("GET", "/api/artistas", null, true), n -> new Artista(
                n.path("id_artista").asInt(), n.path("nombre_artistico").asText(),
                n.path("genero_musical").asText(), n.path("pais_origen").asText("")));
    }

    public void crearArtista(String nombre, String genero, String pais) {
        request("POST", "/api/artistas/create", cuerpoArtista(nombre, genero, pais), true);
    }

    public void actualizarArtista(int id, String nombre, String genero, String pais) {
        Validaciones.id(id, "Artista");
        request("PUT", "/api/artistas/update/" + id, cuerpoArtista(nombre, genero, pais), true);
    }

    public void eliminarArtista(int id) {
        Validaciones.id(id, "Artista");
        request("DELETE", "/api/artistas/delete/" + id, null, true);
    }

    private static ObjectNode cuerpoArtista(String nombre, String genero, String pais) {
        Validaciones.artista(nombre, genero, pais);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("nombre_artistico", nombre.trim());
        body.put("genero_musical", genero.trim());
        if (pais == null || pais.isBlank()) body.putNull("pais_origen"); else body.put("pais_origen", pais.trim());
        return body;
    }

    public void crearConcierto(int idArtista, String titulo, String fechaIso, String recinto, String estado) {
        request("POST", "/api/conciertos/create", cuerpoConcierto(idArtista, titulo, fechaIso, recinto, estado), true);
    }

    public void actualizarConcierto(int id, int idArtista, String titulo, String fechaIso, String recinto, String estado) {
        Validaciones.id(id, "Concierto");
        request("PUT", "/api/conciertos/update/" + id, cuerpoConcierto(idArtista, titulo, fechaIso, recinto, estado), true);
    }

    public void eliminarConcierto(int id) {
        Validaciones.id(id, "Concierto");
        request("DELETE", "/api/conciertos/delete/" + id, null, true);
    }

    private static ObjectNode cuerpoConcierto(int idArtista, String titulo, String fechaIso, String recinto, String estado) {
        Validaciones.concierto(idArtista, titulo, fechaIso, recinto, estado);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("id_artista", idArtista);
        body.put("titulo_evento", titulo.trim());
        body.put("fecha_concierto", fechaIso);
        body.put("recinto", recinto.trim());
        body.put("estado", estado);
        return body;
    }

    // -------------------------------------------------------------- usuarios

    public List<Usuario> listarUsuarios() {
        return lista(request("GET", "/api/usuarios", null, true), ApiClient::usuario);
    }

    public void crearUsuarioSesion(String nombreUsuario, String nombreCompleto, String contrasena, String rol) {
        Validaciones.usuario(nombreUsuario, nombreCompleto, rol, contrasena, true);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("nombre_usuario", nombreUsuario.trim());
        body.put("nombre_completo", nombreCompleto.trim());
        body.put("contrasena", contrasena);
        body.put("rol", rol);
        request("POST", "/api/usuarios/create", body, true);
    }

    /** contrasena en blanco = no se cambia. */
    public void actualizarUsuario(int id, String nombreUsuario, String nombreCompleto, String rol, String contrasena) {
        Validaciones.id(id, "Usuario");
        Validaciones.usuario(nombreUsuario, nombreCompleto, rol, contrasena, false);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("nombre_usuario", nombreUsuario.trim());
        body.put("nombre_completo", nombreCompleto.trim());
        body.put("rol", rol);
        if (contrasena != null && !contrasena.isEmpty()) body.put("contrasena", contrasena);
        request("PUT", "/api/usuarios/update/" + id, body, true);
    }

    public void eliminarUsuario(int id) {
        Validaciones.id(id, "Usuario");
        request("DELETE", "/api/usuarios/delete/" + id, null, true);
    }

    private static Usuario usuario(JsonNode u) {
        return new Usuario(u.path("id_usuario").asInt(), u.path("nombre_usuario").asText(),
                u.path("nombre_completo").asText(), u.path("rol").asText());
    }

    // ------------------------------------------------------------ inventario

    public List<Inventario> listarInventario(Integer idConcierto) {
        String path = "/api/inventario-boletos" + (idConcierto == null ? "" : "?id_concierto=" + idConcierto);
        return lista(request("GET", path, null, true), ApiClient::inventario);
    }

    public Inventario crearInventario(int idConcierto, int idLocalidad, BigDecimal precio, int total, int disponible) {
        return inventario(request("POST", "/api/inventario-boletos/create",
                cuerpoInventario(idConcierto, idLocalidad, precio, total, disponible), true));
    }

    public Inventario actualizarInventario(int id, int idConcierto, int idLocalidad, BigDecimal precio, int total, int disponible) {
        Validaciones.id(id, "Inventario");
        return inventario(request("PUT", "/api/inventario-boletos/update/" + id,
                cuerpoInventario(idConcierto, idLocalidad, precio, total, disponible), true));
    }

    public void eliminarInventario(int id) {
        Validaciones.id(id, "Inventario");
        request("DELETE", "/api/inventario-boletos/delete/" + id, null, true);
    }

    private static ObjectNode cuerpoInventario(int idConcierto, int idLocalidad, BigDecimal precio, int total, int disponible) {
        Validaciones.inventario(idConcierto, idLocalidad, precio, total, disponible);
        ObjectNode body = MAPPER.createObjectNode();
        body.put("id_concierto", idConcierto);
        body.put("id_localidad", idLocalidad);
        body.put("precio", precio);
        body.put("cantidad_total", total);
        body.put("cantidad_disponible", disponible);
        return body;
    }

    private static Inventario inventario(JsonNode n) {
        return new Inventario(n.path("id_inventario").asInt(), n.path("id_concierto").asInt(),
                n.path("id_localidad").asInt(), new BigDecimal(n.path("precio").asText("0")),
                n.path("cantidad_total").asInt(), n.path("cantidad_disponible").asInt());
    }

    // ---------------------------------------------------------------- ventas

    public Venta crearVenta(List<ItemCarrito> items) {
        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (ItemCarrito item : items) cantidades.merge(item.inventario().id(), item.cantidad(), Integer::sum);
        return crearVentaPorIds(cantidades);
    }

    /** idInventario -> cantidad. */
    public Venta crearVentaPorIds(Map<Integer, Integer> cantidades) {
        Validaciones.detallesVenta(cantidades);
        ObjectNode body = MAPPER.createObjectNode();
        ArrayNode detalles = body.putArray("detalles");
        for (Map.Entry<Integer, Integer> e : cantidades.entrySet()) {
            ObjectNode d = detalles.addObject();
            d.put("id_inventario", e.getKey());
            d.put("cantidad", e.getValue());
        }
        return venta(request("POST", "/api/ventas/create", body, true));
    }

    public Venta obtenerVenta(int id) {
        Validaciones.id(id, "Venta");
        return venta(request("GET", "/api/ventas/" + id, null, true));
    }

    public List<Venta> listarVentas() {
        return lista(request("GET", "/api/ventas", null, true), ApiClient::venta);
    }

    private static Venta venta(JsonNode n) {
        List<DetalleVenta> detalles = lista(n.path("detalles"), d -> new DetalleVenta(
                d.path("id_inventario").asInt(), d.path("cantidad").asInt(),
                new BigDecimal(d.path("precio_unitario").asText("0")), new BigDecimal(d.path("subtotal").asText("0"))));
        // El vendedor puede venir plano (id_usuario, nombre_usuario...) o anidado en "usuario": se aceptan ambos.
        Integer idUsuario = primerEntero(n, "id_usuario", "id_vendedor", "usuario_id");
        String vendedor = primerTexto(n, "nombre_usuario", "nombre_vendedor", "vendedor", "usuario");
        JsonNode u = n.path("usuario");
        if (u.isObject()) {
            if (idUsuario == null) idUsuario = primerEntero(u, "id_usuario", "id");
            String nombre = primerTexto(u, "nombre_completo", "nombre_usuario");
            if (nombre != null) vendedor = nombre;
        }
        return new Venta(n.path("id_venta").asInt(), n.path("fecha_venta").asText(),
                new BigDecimal(n.path("total_venta").asText("0")), detalles, idUsuario, vendedor);
    }

    private static Integer primerEntero(JsonNode n, String... campos) {
        for (String c : campos) {
            JsonNode v = n.path(c);
            if (v.isNumber() || (v.isTextual() && v.asText().matches("\\d+"))) {
                return v.asInt();
            }
        }
        return null;
    }

    private static String primerTexto(JsonNode n, String... campos) {
        for (String c : campos) {
            JsonNode v = n.path(c);
            if (v.isValueNode() && !v.isNull() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ http

    private static <T> List<T> lista(JsonNode arreglo, Function<JsonNode, T> mapeo) {
        List<T> salida = new ArrayList<>();
        if (arreglo != null && arreglo.isArray()) {
            for (JsonNode n : arreglo) salida.add(mapeo.apply(n));
        }
        return salida;
    }

    private JsonNode request(String metodo, String path, JsonNode body, boolean conAuth) {
        if (conAuth && authHeader == null) throw new ApiException(401, "Inicia sesión para continuar.");
        return request(baseUrl, conAuth ? authHeader : null, metodo, path, body);
    }

    private JsonNode request(String baseUrl, String authHeader, String metodo, String path, JsonNode body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(60))
                    .header("Accept", "application/json");
            if (authHeader != null) b.header("Authorization", authHeader);
            if (body != null) {
                b.header("Content-Type", "application/json");
                b.method(metodo, HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body), StandardCharsets.UTF_8));
            } else {
                b.method(metodo, HttpRequest.BodyPublishers.noBody());
            }
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = leer(resp.body());
            if (resp.statusCode() >= 400) {
                String msg = json.path("message").asText("");
                if (msg.isBlank()) {
                    msg = resp.statusCode() == 502 || resp.statusCode() == 503 || resp.statusCode() == 504
                            ? "El servidor no está disponible o está despertando (error " + resp.statusCode() + "). Espera unos segundos e intenta de nuevo."
                            : "El servidor respondió con error " + resp.statusCode() + ".";
                }
                throw new ApiException(resp.statusCode(), msg);
            }
            return json;
        } catch (IllegalArgumentException e) {
            throw new ApiException(0, "La dirección del servidor no es válida.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "La operación fue interrumpida.");
        } catch (IOException e) {
            throw new ApiException(0, "No se pudo conectar con el servidor (" + baseUrl + "). Verifica que el backend esté encendido.");
        }
    }

    private static JsonNode leer(String texto) {
        if (texto == null || texto.isBlank()) return MAPPER.nullNode();
        // Render devuelve una página HTML (502/503) mientras el servicio despierta: no se muestra tal cual al usuario.
        if (texto.stripLeading().startsWith("<")) return MAPPER.createObjectNode();
        try {
            return MAPPER.readTree(texto);
        } catch (IOException e) {
            return MAPPER.createObjectNode().put("message", texto.length() > 200 ? texto.substring(0, 200) : texto);
        }
    }
}
