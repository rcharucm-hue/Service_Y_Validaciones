package com.svbgua.kiosco;
import com.svbgua.kiosco.datos.*;
import com.svbgua.kiosco.modelo.Asiento;
import com.svbgua.kiosco.modelo.LineaSeleccion;
import com.svbgua.kiosco.modelo.SeleccionActual;
import com.svbgua.model.Modelos.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

/** Pruebas de los dos errores del kiosco; no accede a Render ni al archivo real del usuario. */
public class AutoservicioPrueba {
 private static int pruebas;
 private static void comprobar(boolean ok,String mensaje){if(!ok)throw new AssertionError(mensaje);pruebas++;}
 private static void rechaza(Runnable accion,String mensaje){try{accion.run();}catch(IllegalArgumentException|IllegalStateException e){pruebas++;return;}throw new AssertionError(mensaje);}
 public static void main(String[] args)throws Exception {
  Path carpeta=Path.of(args.length==0?"target/pruebas-autoservicio":args[0]);Files.createDirectories(carpeta);
  Path archivo=carpeta.resolve("asientos-"+UUID.randomUUID()+".properties");
  var cat=new Catalogo(List.of(),Map.of(1,new Localidad(1,"General"),2,new Localidad(2,"Mesa")),Map.of());
  var general=new Inventario(1,10,1,new BigDecimal("50.00"),100,100);
  var mesaAjena=new Inventario(2,99,2,new BigDecimal("20.00"),8,8);
  var lista=InventarioConcierto.filtrar(List.of(general,mesaAjena),cat,10);
  comprobar(lista.equals(List.of(general)),"Inventario ajeno no se debe mostrar");
  for(boolean vertical:List.of(false,true)) {
   var mapa=FabricaLocalidades.crear(lista,cat,vertical);
   comprobar(mapa.stream().noneMatch(l->l instanceof Asiento),"Concierto sin mesas debe carecer de asientos");
   comprobar(mapa.stream().noneMatch(l->l.getPrecio().compareTo(new BigDecimal("20"))==0),"No generar mesa ficticia de Q20");
  }
  rechaza(()->InventarioConcierto.validarCompra(lista,Map.of(2,1)),"No vender localidad de otro concierto");
  var mesa=new Inventario(3,10,2,new BigDecimal("120.00"),12,12);
  var estado=new EstadoAsientos(archivo);
  var mapa=FabricaLocalidades.crear(List.of(mesa),cat,false,estado.ocupados("https://api.example",10,List.of(mesa)));
  List<LineaSeleccion> comprados=mapa.stream().filter(l->l instanceof Asiento a && a.getMesaId().equals("M02"))
   .map(l->new LineaSeleccion(l,1)).toList();
  comprobar(comprados.size()==4,"Mesa completa tiene cuatro asientos");
  comprobar(comprados.stream().allMatch(l->l.localidad().getPrecio().compareTo(new BigDecimal("120"))==0),"Usar precio real por asiento");
  comprobar(estado.ocupados("https://api.example",10,List.of(mesa)).get(3).isEmpty(),"Seleccionar sin confirmar no bloquea asientos");
  estado.confirmar("https://api.example",10,comprados);
  estado.confirmar("https://api.example",10,comprados);
  comprobar(estado.ocupados("https://api.example",10,List.of(mesa)).get(3).size()==4,"Confirmacion repetida no duplica ocupacion");
  var despues=new Inventario(3,10,2,new BigDecimal("120.00"),12,8);
  var reiniciado=new EstadoAsientos(archivo);
  for(boolean vertical:List.of(false,true)) {
   var nuevo=FabricaLocalidades.crear(List.of(despues),cat,vertical,reiniciado.ocupados("https://api.example",10,List.of(despues)));
   comprobar(nuevo.stream().filter(l->l instanceof Asiento a && a.getMesaId().equals("M02")).noneMatch(l->l.estaDisponible()),"M02 completa debe quedar ocupada tras recargar o reiniciar");
   comprobar(nuevo.stream().filter(l->l.estaDisponible()).count()==8,"Quedan ocho asientos disponibles");
   var seleccion=new SeleccionActual();
   comprobar(!seleccion.tocar(nuevo.stream().filter(l->l.getId().equals("M02-A1")).findFirst().orElseThrow()),"No volver a seleccionar un asiento vendido");
  }
  comprobar(reiniciado.ocupados("https://otro.example",10,List.of(mesa)).get(3).isEmpty(),"Separar servidores");
  comprobar(reiniciado.ocupados("https://api.example",20,List.of(new Inventario(3,20,2,new BigDecimal("120"),12,12))).get(3).isEmpty(),"Separar conciertos");
  var desactualizado=FabricaLocalidades.crear(List.of(mesa),cat,false,reiniciado.ocupados("https://api.example",10,List.of(mesa)));
  comprobar(desactualizado.stream().filter(l->l.getId().startsWith("M02-")).noneMatch(l->l.estaDisponible()),"Respuesta antigua no libera los asientos confirmados");
  var remoto=new Inventario(3,10,2,new BigDecimal("120"),12,6);
  var combinado=FabricaLocalidades.crear(List.of(remoto),cat,false,reiniciado.ocupados("https://api.example",10,List.of(remoto)));
  comprobar(combinado.stream().filter(l->l.estaDisponible()).count()==6,"Respetar descuentos adicionales del servidor");
  comprobar(combinado.stream().filter(l->l.getId().startsWith("M02-")).noneMatch(l->l.estaDisponible()),"Ocupacion externa no mueve asientos confirmados");
  rechaza(()->InventarioConcierto.validarCompra(List.of(remoto),Map.of(3,7)),"Rechazar sobreventa antes del POST");
  Files.deleteIfExists(archivo);
  System.out.println(pruebas+" comprobaciones de autoservicio correctas.");
 }
}
