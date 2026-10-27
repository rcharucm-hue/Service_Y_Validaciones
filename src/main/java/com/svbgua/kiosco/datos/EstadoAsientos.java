package com.svbgua.kiosco.datos;

import com.svbgua.kiosco.modelo.Asiento;
import com.svbgua.kiosco.modelo.LineaSeleccion;
import com.svbgua.model.Modelos.Inventario;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Ocupacion confirmada en esta terminal, separada por servidor, concierto e inventario. */
public final class EstadoAsientos {
    private final Path archivo;
    private final Properties datos=new Properties();
    private boolean cargado;
    public EstadoAsientos(Path archivo) { this.archivo=archivo.toAbsolutePath(); }
    public static EstadoAsientos local() {
        return new EstadoAsientos(Path.of(System.getProperty("user.home"),".svbgua","asientos-vendidos.properties"));
    }
    private void cargar() throws IOException {
        if(cargado)return;
        if(Files.exists(archivo))try(InputStream in=Files.newInputStream(archivo)){datos.load(in);}
        cargado=true;
    }
    private static String clave(String servidor,int concierto,int inventario) {
        try {
            String entorno=servidor.trim().replaceAll("/+$", "");
            byte[] hash=MessageDigest.getInstance("SHA-256").digest(entorno.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash)+"."+concierto+"."+inventario;
        } catch(NoSuchAlgorithmException imposible){throw new IllegalStateException(imposible);}
    }
    private Set<String> leer(String clave) throws IOException {
        cargar();Set<String> asientos=new HashSet<>();
        for(String id:datos.getProperty(clave,"").split(","))if(id.matches("M[0-9]+-A[1-4]"))asientos.add(id);
        return asientos;
    }
    public synchronized Map<Integer,Set<String>> ocupados(String servidor,int concierto,List<Inventario> inventarios) throws IOException {
        Map<Integer,Set<String>> mapa=new HashMap<>();
        for(Inventario inv:inventarios) {
            if(inv.idConcierto()!=concierto)continue;
            mapa.put(inv.id(),Set.copyOf(leer(clave(servidor,concierto,inv.id()))));
        }
        return Map.copyOf(mapa);
    }
    /** Llamar SOLO despues de recibir la venta confirmada. Si el disco falla se conserva el estado en memoria. */
    public synchronized void confirmar(String servidor,int concierto,List<LineaSeleccion> lineas) throws IOException {
        cargar();boolean cambio=false;
        for(LineaSeleccion linea:lineas)if(linea.localidad() instanceof Asiento asiento) {
            String clave=clave(servidor,concierto,asiento.getIdInventario());
            Set<String> vendidos=leer(clave);
            vendidos.add(asiento.getId());datos.setProperty(clave,String.join(",",new TreeSet<>(vendidos)));cambio=true;
        }
        if(!cambio)return;
        Files.createDirectories(archivo.getParent());
        Path temporal=Files.createTempFile(archivo.getParent(),"asientos-",".tmp");
        try {
            try(OutputStream out=Files.newOutputStream(temporal)){datos.store(out,"Asientos vendidos confirmados; no contiene datos del cliente");}
            try{Files.move(temporal,archivo,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporal,archivo,StandardCopyOption.REPLACE_EXISTING);}
        } finally {Files.deleteIfExists(temporal);}
    }
}
