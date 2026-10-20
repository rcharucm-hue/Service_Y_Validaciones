package com.umg.validaciones.service;
import com.umg.validaciones.model.Artista;
import java.util.*;
public final class ArtistaService extends CatalogoService<Artista> {
    public ArtistaService(ApiClient api) { super(api,"/api/artistas",Artista.class); }
    @Override protected Map<String,Object> cuerpo(Artista m) {
        ValidacionesService.validar(m);
        Map<String,Object> datos=new LinkedHashMap<>();
        if(m.getNombre_artistico()!=null) datos.put("nombre_artistico",m.getNombre_artistico());
        if(m.getGenero_musical()!=null) datos.put("genero_musical",m.getGenero_musical());
        if(m.getPais_origen()!=null) datos.put("pais_origen",m.getPais_origen());
        return datos;
    }
}
