package com.umg.validaciones;
import com.umg.validaciones.service.*;
public class EjemploConexion {
    public static void main(String[] args) throws Exception {
        ApiClient api=ApiClient.desdeEntorno();
        var usuario=new UsuarioService(api).actual();
        System.out.println("Conexion correcta. Rol: "+usuario.getRol());
        System.out.println("Conciertos disponibles en catalogo: "+new ConciertoService(api).listar().size());
    }
}
