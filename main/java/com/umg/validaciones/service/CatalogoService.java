package com.umg.validaciones.service;
import java.io.IOException;
import java.util.*;
public abstract class CatalogoService<T> {
    protected final ApiClient api;
    protected final String ruta;
    private final Class<T> tipo;
    protected CatalogoService(ApiClient api,String ruta,Class<T> tipo) {
        this.api=Objects.requireNonNull(api); this.ruta=ruta; this.tipo=tipo;
    }
    protected abstract Map<String,Object> cuerpo(T datos);
    public List<T> listar() throws IOException,InterruptedException { return api.lista(api.enviar("GET",ruta,null),tipo); }
    public T buscar(int id) throws IOException,InterruptedException {
        ValidacionesService.id(id,"ID"); return api.convertir(api.enviar("GET",ruta+"/"+id,null),tipo);
    }
    public T crear(T datos) throws IOException,InterruptedException {
        return api.convertir(api.enviar("POST",ruta+"/create",cuerpo(datos)),tipo);
    }
    public T actualizar(int id,T datos) throws IOException,InterruptedException {
        ValidacionesService.id(id,"ID"); return api.convertir(api.enviar("PUT",ruta+"/update/"+id,cuerpo(datos)),tipo);
    }
    public void eliminar(int id) throws IOException,InterruptedException {
        ValidacionesService.id(id,"ID"); api.enviar("DELETE",ruta+"/delete/"+id,null);
    }
}
