package com.umg.validaciones.service;
import com.umg.validaciones.model.*;
import java.io.IOException;
import java.util.*;
public final class UsuarioService {
    private final ApiClient api;
    private static final String RUTA="/api/usuarios";
    public UsuarioService(ApiClient api) { this.api=Objects.requireNonNull(api); }
    public Usuario login() throws IOException,InterruptedException { return api.login(); }
    public Usuario actual() throws IOException,InterruptedException { return api.convertir(api.enviar("GET",RUTA+"/me",null),Usuario.class); }
    public List<Usuario> listar() throws IOException,InterruptedException { return api.lista(api.enviar("GET",RUTA,null),Usuario.class); }
    public Usuario buscar(int id) throws IOException,InterruptedException {
        ValidacionesService.id(id,"ID"); return api.convertir(api.enviar("GET",RUTA+"/"+id,null),Usuario.class);
    }
    public Usuario crear(UsuarioSolicitud datos) throws IOException,InterruptedException {
        return api.convertir(api.enviar("POST",RUTA+"/create",cuerpo(datos,false)),Usuario.class);
    }
    public Usuario actualizar(int id,UsuarioSolicitud datos) throws IOException,InterruptedException {
        ValidacionesService.id(id,"ID"); return api.convertir(api.enviar("PUT",RUTA+"/update/"+id,cuerpo(datos,true)),Usuario.class);
    }
    public void eliminar(int id) throws IOException,InterruptedException {
        ValidacionesService.id(id,"ID"); api.enviar("DELETE",RUTA+"/delete/"+id,null);
    }
    private Map<String,Object> cuerpo(UsuarioSolicitud m,boolean parcial) {
        ValidacionesService.validar(m,parcial);
        Map<String,Object> datos=new LinkedHashMap<>();
        if(m.nombre_usuario()!=null) datos.put("nombre_usuario",m.nombre_usuario());
        if(m.nombre_completo()!=null) datos.put("nombre_completo",m.nombre_completo());
        if(m.contrasena()!=null) datos.put("contrasena",m.contrasena());
        if(m.rol()!=null) datos.put("rol",m.rol());
        return datos;
    }
}
