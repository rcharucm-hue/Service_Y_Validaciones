package com.svbgua.kiosco.datos;
import com.svbgua.model.Modelos.*;
import java.util.*;
/** La consulta HTTP no basta: cada registro debe pertenecer al concierto seleccionado. */
public final class InventarioConcierto {
    private InventarioConcierto() {}
    public static List<Inventario> filtrar(List<Inventario> datos,Catalogo catalogo,int idConcierto) {
        if(idConcierto<=0)throw new IllegalArgumentException("Concierto no valido.");
        if(datos==null)throw new IllegalArgumentException("La API no devolvio inventario.");
        List<Inventario> salida=new ArrayList<>();Set<Integer> ids=new HashSet<>();
        for(Inventario i:datos) {
            if(i==null || i.idConcierto()!=idConcierto)continue;
            if(i.id()<=0 || !catalogo.localidades().containsKey(i.idLocalidad()) || i.precio()==null
                    || i.precio().signum()<=0 || i.total()<=0 || i.disponible()<0 || i.disponible()>i.total())
                throw new IllegalStateException("La API devolvio inventario no valido para este concierto.");
            if(!ids.add(i.id()))throw new IllegalStateException("La API devolvio inventario duplicado.");
            salida.add(i);
        }
        return List.copyOf(salida);
    }
    public static Map<Integer,Integer> validarCompra(List<Inventario> inventario,Map<Integer,Integer> cantidades) {
        com.svbgua.validacion.Validaciones.detallesVenta(cantidades);
        Map<Integer,Inventario> porId=new HashMap<>();for(Inventario i:inventario)porId.put(i.id(),i);
        for(var entrada:cantidades.entrySet()) {
            Inventario i=porId.get(entrada.getKey());
            if(i==null)throw new IllegalStateException("La localidad seleccionada ya no existe en este concierto. Vuelve al mapa.");
            if(entrada.getValue()>i.disponible())throw new IllegalStateException("La localidad ya no tiene suficientes boletos. Vuelve al mapa.");
        }
        return Map.copyOf(cantidades);
    }
}
