package com.umg.validaciones.service;
import com.umg.validaciones.model.*;
import java.io.IOException;
import java.util.*;
public final class VentaService {
    private final ApiClient api;
    public VentaService(ApiClient api) { this.api=Objects.requireNonNull(api); }
    public Venta crear(List<DetalleSolicitud> detalles) throws IOException,InterruptedException {
        ValidacionesService.validarDetalles(detalles);
        
        return api.convertir(api.enviar("POST","/api/ventas/create",Map.of("detalles",detalles)),Venta.class);
    }
    public List<Venta> listar() throws IOException,InterruptedException {
        return api.lista(api.enviar("GET","/api/ventas",null),Venta.class);
    }
    public Venta buscar(int id) throws IOException,InterruptedException {
        ValidacionesService.id(id,"ID"); return api.convertir(api.enviar("GET","/api/ventas/"+id,null),Venta.class);
    }
}
