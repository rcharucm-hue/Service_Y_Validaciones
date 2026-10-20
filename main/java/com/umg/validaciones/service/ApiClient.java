package com.umg.validaciones.service;

import com.fasterxml.jackson.databind.*;
import com.umg.validaciones.model.Usuario;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public final class ApiClient {
    public static final String URL_RENDER="https://svb-gua-backend.onrender.com";
    private final URI base;
    private final String usuario, contrasena;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final ObjectMapper mapper=new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
    public ApiClient(String nombre, String clave) { this(URL_RENDER,nombre,clave); }
    public ApiClient(String url, String nombre, String clave) {
        base=URI.create(url.replaceAll("/+$", ""));
        boolean local=Set.of("localhost","127.0.0.1","[::1]").contains(String.valueOf(base.getHost()));
        if(base.getHost()==null || base.getUserInfo()!=null || base.getQuery()!=null || base.getFragment()!=null
                || (base.getPath()!=null && !base.getPath().isEmpty())
                || !("https".equals(base.getScheme()) || (local && "http".equals(base.getScheme()))))
            throw new IllegalArgumentException("La URL debe ser un origen HTTPS; HTTP solo se permite en localhost.");
        if(nombre==null || nombre.isBlank() || nombre.trim().length()>50 || nombre.contains(":")
                || clave==null || clave.isEmpty() || clave.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ValidacionException("Usuario o contrasena no validos para Basic Auth.");
        usuario=nombre.trim(); contrasena=clave;
    }
    public static ApiClient desdeEntorno() {
        String url=System.getenv("SVB_API_URL");
        return new ApiClient(url==null || url.isBlank()?URL_RENDER:url,System.getenv("SVB_USUARIO"),System.getenv("SVB_CONTRASENA"));
    }
    
    public Usuario login() throws IOException, InterruptedException {
        JsonNode respuesta=enviar("POST","/api/usuarios/login",Map.of("nombre_usuario",usuario,"contrasena",contrasena));
        if(!respuesta.hasNonNull("usuario")) throw new IOException("La API no devolvio el usuario del login.");
        return convertir(respuesta.get("usuario"),Usuario.class);
    }
    JsonNode enviar(String metodo,String ruta,Object cuerpo) throws IOException, InterruptedException {
        if(ruta==null || !ruta.startsWith("/api/") || ruta.contains("#")) throw new IllegalArgumentException("Ruta API no valida.");
        URI destino=URI.create(base.toString()+ruta);
        String basic=Base64.getEncoder().encodeToString((usuario+":"+contrasena).getBytes(StandardCharsets.UTF_8));
        HttpRequest.Builder req=HttpRequest.newBuilder(destino).timeout(Duration.ofSeconds(90))
                .header("Accept","application/json").header("Authorization","Basic "+basic);
        if(cuerpo==null) req.method(metodo,HttpRequest.BodyPublishers.noBody());
        else req.header("Content-Type","application/json; charset=UTF-8")
                .method(metodo,HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(cuerpo),StandardCharsets.UTF_8));
        HttpResponse<String> respuesta=http.send(req.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int estado=respuesta.statusCode();
        if(estado<200 || estado>=300) {
            String mensaje="La API respondio HTTP "+estado;
            try { JsonNode error=mapper.readTree(respuesta.body());
                if(error!=null && error.path("message").isTextual()) mensaje=error.get("message").asText();
            } catch(IOException ignorado) { /* Render puede devolver HTML al fallar. */ }
            throw new ApiException(estado,mensaje);
        }
        if(respuesta.body().isBlank()) return mapper.nullNode();
        return mapper.readTree(respuesta.body());
    }
    <T> T convertir(JsonNode nodo,Class<T> tipo) throws IOException { return mapper.treeToValue(nodo,tipo); }
    <T> List<T> lista(JsonNode nodo,Class<T> tipo) throws IOException {
        if(!nodo.isArray()) throw new IOException("Se esperaba una lista JSON de la API.");
        List<T> datos=new ArrayList<>(); for(JsonNode item:nodo) datos.add(convertir(item,tipo)); return datos;
    }
}
