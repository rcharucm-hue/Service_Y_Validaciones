package com.umg.validaciones.service;
import com.umg.validaciones.model.Localidad;
import java.util.*;
public final class LocalidadService extends CatalogoService<Localidad> {
    public LocalidadService(ApiClient api) { super(api,"/api/localidades",Localidad.class); }
    @Override protected Map<String,Object> cuerpo(Localidad m) {
        ValidacionesService.validar(m);
        Map<String,Object> datos=new LinkedHashMap<>();
        if(m.getNombre()!=null) datos.put("nombre",m.getNombre());
        return datos;
    }
}
