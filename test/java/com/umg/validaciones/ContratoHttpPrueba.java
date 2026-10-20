package com.umg.validaciones;
import com.umg.validaciones.model.*;
import com.umg.validaciones.service.*;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.*;

public class ContratoHttpPrueba {
    private static int pruebas;
    private static void comprobar(boolean cumple,String mensaje) {
        if(!cumple) throw new AssertionError(mensaje); pruebas++;
    }
    public static void main(String[] args) throws Exception {
        ObjectMapper json=new ObjectMapper();
        AtomicReference<String> respuesta=new AtomicReference<>("{}");
        AtomicInteger estado=new AtomicInteger(200), peticiones=new AtomicInteger();
        AtomicReference<String> metodo=new AtomicReference<>(),ruta=new AtomicReference<>(),auth=new AtomicReference<>();
        AtomicReference<JsonNode> cuerpo=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",intercambio->{
            peticiones.incrementAndGet(); metodo.set(intercambio.getRequestMethod());
            ruta.set(intercambio.getRequestURI().getPath()); auth.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            String recibido=new String(intercambio.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            cuerpo.set(recibido.isBlank()?json.nullNode():json.readTree(recibido));
            byte[] bytes=respuesta.get().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type","application/json");
            intercambio.sendResponseHeaders(estado.get(),bytes.length);
            intercambio.getResponseBody().write(bytes); intercambio.close();
        });
        server.start();
        try {
            ApiClient api=new ApiClient("http://127.0.0.1:"+server.getAddress().getPort(),"admin","clave-prueba");
            Localidad m=new Localidad(); m.setId_localidad(77); m.setNombre("VIP");
            LocalidadService localidades=new LocalidadService(api);
            estado.set(201); respuesta.set("{\"id_localidad\":1,\"nombre\":\"VIP\"}");
            comprobar(localidades.crear(m).getId_localidad()==1,"Respuesta crear localidad");
            comprobar(metodo.get().equals("POST") && ruta.get().equals("/api/localidades/create"),"Ruta crear localidad");
            comprobar(cuerpo.get().size()==1 && cuerpo.get().path("nombre").asText().equals("VIP"),"No enviar ID al crear");
            comprobar(auth.get().equals("Basic "+Base64.getEncoder().encodeToString("admin:clave-prueba".getBytes(StandardCharsets.UTF_8))),"Basic Auth");
            estado.set(200); localidades.actualizar(1,m);
            comprobar(metodo.get().equals("PUT") && ruta.get().equals("/api/localidades/update/1"),"Actualizar localidad");
            respuesta.set("[{\"id_localidad\":1,\"nombre\":\"VIP\"}]");
            comprobar(localidades.listar().size()==1 && ruta.get().equals("/api/localidades"),"Lista localidad");
            respuesta.set("{\"id_localidad\":1,\"nombre\":\"VIP\"}"); localidades.buscar(1);
            comprobar(ruta.get().equals("/api/localidades/1"),"Buscar localidad");
            respuesta.set("{\"message\":\"Eliminada\"}"); localidades.eliminar(1);
            comprobar(metodo.get().equals("DELETE") && ruta.get().equals("/api/localidades/delete/1"),"Eliminar localidad");

            Concierto c=new Concierto(); c.setId_concierto(80); c.setId_artista(1); c.setTitulo_evento("Show");
            c.setFecha_concierto("2026-12-20T20:00:00-06:00"); c.setRecinto("Teatro");
            respuesta.set("{\"id_concierto\":1,\"estado\":\"programado\"}"); new ConciertoService(api).crear(c);
            comprobar(ruta.get().equals("/api/conciertos/create"),"Ruta concierto");
            comprobar(!cuerpo.get().has("estado") && !cuerpo.get().has("id_concierto") && cuerpo.get().size()==4,"Omitir estado nulo y campos de lectura");
            Artista a=new Artista(); a.setNombre_artistico("Artista"); a.setGenero_musical("Rock");
            new ArtistaService(api).crear(a);
            comprobar(ruta.get().equals("/api/artistas/create") && cuerpo.get().size()==2,"Cuerpo artista");
            InventarioBoleto inventario=new InventarioBoleto(); inventario.setId_inventario(90); inventario.setId_concierto(1);
            inventario.setId_localidad(1); inventario.setPrecio(new BigDecimal("25.50"));
            inventario.setCantidad_total(100); inventario.setCantidad_disponible(100);
            new InventarioBoletoService(api).crear(inventario);
            comprobar(ruta.get().equals("/api/inventario-boletos/create"),"Ruta inventario");
            comprobar(cuerpo.get().size()==5 && cuerpo.get().path("precio").isNumber(),"Precio debe ser numero JSON");

            UsuarioService usuarios=new UsuarioService(api);
            respuesta.set("{\"message\":\"Login correcto.\",\"usuario\":{\"id_usuario\":1,\"nombre_usuario\":\"admin\",\"rol\":\"administrador\"}}");
            comprobar(usuarios.login().getId_usuario()==1,"Desenvolver usuario de login");
            comprobar(ruta.get().equals("/api/usuarios/login") && cuerpo.get().size()==2 && cuerpo.get().has("contrasena"),"Cuerpo login");
            respuesta.set("{\"id_usuario\":1,\"nombre_usuario\":\"admin\",\"rol\":\"administrador\"}"); usuarios.actual();
            comprobar(ruta.get().equals("/api/usuarios/me"),"Identidad");
            usuarios.crear(new UsuarioSolicitud("otro","Nombre Apellido","contrasena8","vendedor"));
            comprobar(cuerpo.get().size()==4 && cuerpo.get().has("contrasena") && !cuerpo.get().has("contrasena_hash"),"No enviar hash");
            usuarios.actualizar(2,new UsuarioSolicitud(null,"Otro Nombre",null,null));
            comprobar(cuerpo.get().size()==1 && cuerpo.get().has("nombre_completo"),"Actualizacion parcial sin nulls");

            VentaService ventas=new VentaService(api);
            estado.set(201); respuesta.set("{\"id_venta\":1,\"total_venta\":\"51.00\",\"detalles\":[{\"id_inventario\":3,\"cantidad\":2,\"precio_unitario\":\"25.50\",\"subtotal\":\"51.00\",\"inventario\":{\"id_inventario\":3,\"id_concierto\":1,\"id_localidad\":2}}]}");
            Venta venta=ventas.crear(List.of(new DetalleSolicitud(3,2)));
            comprobar(ruta.get().equals("/api/ventas/create") && cuerpo.get().size()==1 && cuerpo.get().has("detalles"),"Solo enviar detalles");
            comprobar(cuerpo.get().path("detalles").get(0).size()==2,"No enviar vendedor ni precios");
            comprobar(venta.getTotal_venta().compareTo(new BigDecimal("51.00"))==0 && venta.getDetalles().get(0).getInventario().getId_concierto()==1,"Leer decimales y asociaciones");
            int antes=peticiones.get();
            try { ventas.crear(List.of(new DetalleSolicitud(3,1),new DetalleSolicitud(3,2))); throw new AssertionError("Duplicado aceptado"); }
            catch(ValidacionException e) { comprobar(peticiones.get()==antes,"Duplicado rechazado antes de HTTP"); }
            try { usuarios.crear(new UsuarioSolicitud("otro","Nombre","a".repeat(73),"vendedor")); throw new AssertionError("Contrasena aceptada"); }
            catch(ValidacionException e) { comprobar(peticiones.get()==antes,"Limite bytes validado antes de HTTP"); }
            for(int codigo:List.of(400,401,403,404,409,500)) {
                estado.set(codigo); respuesta.set("{\"message\":\"Mensaje del servidor\"}");
                try { localidades.listar(); throw new AssertionError("Error HTTP ocultado"); }
                catch(ApiException e) { comprobar(e.getEstado()==codigo && e.getMessage().equals("Mensaje del servidor"),"Conservar estado y mensaje "+codigo); }
            }
            estado.set(503); respuesta.set("<html>Unavailable</html>");
            try { localidades.listar(); throw new AssertionError("HTML aceptado"); }
            catch(ApiException e) { comprobar(e.getEstado()==503,"Manejar error HTML de Render"); }
            estado.set(302); respuesta.set("{}"); antes=peticiones.get();
            try { localidades.listar(); throw new AssertionError("Redireccion aceptada"); }
            catch(ApiException e) { comprobar(e.getEstado()==302 && peticiones.get()==antes+1,"No seguir redirecciones con credenciales"); }
            try { new ApiClient("http://example.com","admin","clave"); throw new AssertionError("HTTP remoto aceptado"); }
            catch(IllegalArgumentException e) { pruebas++; }
            System.out.println(pruebas+" comprobaciones de contrato HTTP correctas, usando servidor local.");
        } finally { server.stop(0); }
    }
}
