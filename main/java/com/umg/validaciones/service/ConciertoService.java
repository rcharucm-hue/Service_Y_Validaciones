package com.umg.validaciones.service;
import com.umg.validaciones.model.Concierto;
import java.util.*;
public final class ConciertoService extends CatalogoService<Concierto> {
    public ConciertoService(ApiClient api) { super(api,"/api/conciertos",Concierto.class); }
    @Override protected Map<String,Object> cuerpo(Concierto m) {
        ValidacionesService.validar(m);
        Map<String,Object> datos=new LinkedHashMap<>();
        if(m.getId_artista()!=null) datos.put("id_artista",m.getId_artista());
        if(m.getTitulo_evento()!=null) datos.put("titulo_evento",m.getTitulo_evento());
        if(m.getFecha_concierto()!=null) datos.put("fecha_concierto",m.getFecha_concierto());
        if(m.getRecinto()!=null) datos.put("recinto",m.getRecinto());
        if(m.getEstado()!=null) datos.put("estado",m.getEstado());
        return datos;
    }
}
