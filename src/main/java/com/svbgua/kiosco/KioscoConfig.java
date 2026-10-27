package com.svbgua.kiosco;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Cuenta de servicio del autoservicio. Los clientes no inician sesión: la terminal se conecta sola con esta
 * cuenta (usar una de rol "vendedor", nunca de administrador) y el PIN sirve para que el personal salga.
 *
 * Se lee de ~/.svbgua/kiosco.properties (usuario, clave, pin) o de las variables de entorno
 * SVB_KIOSCO_USER, SVB_KIOSCO_PASS y SVB_KIOSCO_PIN.
 */
public record KioscoConfig(String usuario, String clave, String pin) {

    /** Guarda la configuración en ~/.svbgua/kiosco.properties para que no haya que repetirla. */
    public void guardar() {
        Path dir = Path.of(System.getProperty("user.home"), ".svbgua");
        Properties p = new Properties();
        p.setProperty("usuario", usuario);
        p.setProperty("clave", clave);
        p.setProperty("pin", pin);
        try {
            Files.createDirectories(dir);
            try (java.io.Writer w = Files.newBufferedWriter(dir.resolve("kiosco.properties"), StandardCharsets.UTF_8)) {
                p.store(w, "Cuenta de servicio y PIN del autoservicio (generado por SVB-GUA)");
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la configuración del autoservicio.", e);
        }
    }

    public static KioscoConfig leer() {
        String usuario = System.getenv("SVB_KIOSCO_USER");
        String clave = System.getenv("SVB_KIOSCO_PASS");
        String pin = System.getenv("SVB_KIOSCO_PIN");

        Path archivo = Path.of(System.getProperty("user.home"), ".svbgua", "kiosco.properties");
        if ((usuario == null || clave == null || pin == null) && Files.isRegularFile(archivo)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(archivo)) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + archivo + ".", e);
            }
            usuario = p.getProperty("usuario", usuario);
            clave = p.getProperty("clave", clave);
            pin = p.getProperty("pin", pin);
        }
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalStateException("El autoservicio aún no está configurado en este equipo.");
        }
        if (pin == null || !pin.trim().matches("\\d{4,8}")) {
            throw new IllegalStateException("Falta el PIN del personal (4 a 8 números) en kiosco.properties.");
        }
        return new KioscoConfig(usuario.trim(), clave, pin.trim());
    }
}
