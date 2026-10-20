package com.umg.validaciones.service;
import com.umg.validaciones.model.InventarioBoleto;
import java.util.*;
public final class InventarioBoletoService extends CatalogoService<InventarioBoleto> {
    public InventarioBoletoService(ApiClient api) { super(api,"/api/inventario-boletos",InventarioBoleto.class); }
    @Override protected Map<String,Object> cuerpo(InventarioBoleto m) {
        ValidacionesService.validar(m);
        Map<String,Object> datos=new LinkedHashMap<>();
        if(m.getId_concierto()!=null) datos.put("id_concierto",m.getId_concierto());
        if(m.getId_localidad()!=null) datos.put("id_localidad",m.getId_localidad());
        if(m.getPrecio()!=null) datos.put("precio",m.getPrecio());
        if(m.getCantidad_total()!=null) datos.put("cantidad_total",m.getCantidad_total());
        if(m.getCantidad_disponible()!=null) datos.put("cantidad_disponible",m.getCantidad_disponible());
        return datos;
    }
}
