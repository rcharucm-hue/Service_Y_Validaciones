package com.svbgua.util;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Envía por correo (SMTP, pensado para Gmail) el comprobante de la compra con su código QR.
 *
 * Configuración (cualquiera de las dos):
 *  - Variables de entorno SVB_SMTP_USER y SVB_SMTP_PASS (opcional SVB_SMTP_HOST y SVB_SMTP_PORT).
 *  - Archivo ~/.svbgua/correo.properties con las claves usuario, clave, host y puerto.
 * Para Gmail, "clave" es una contraseña de aplicación (Cuenta de Google > Seguridad > Contraseñas de aplicaciones).
 */
public final class Correo {
    private Correo() {}

    private static final Pattern VALIDO = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    public static boolean valido(String correo) {
        return correo != null && VALIDO.matcher(correo.trim()).matches();
    }

    private record Config(String host, String puerto, String usuario, String clave) {}

    private static Config leerConfig() {
        String usuario = System.getenv("SVB_SMTP_USER");
        String clave = System.getenv("SVB_SMTP_PASS");
        String host = System.getenv("SVB_SMTP_HOST");
        String puerto = System.getenv("SVB_SMTP_PORT");

        Path archivo = Path.of(System.getProperty("user.home"), ".svbgua", "correo.properties");
        if ((usuario == null || clave == null) && Files.isRegularFile(archivo)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(archivo)) {
                p.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo leer " + archivo + ".", e);
            }
            usuario = p.getProperty("usuario", usuario);
            clave = p.getProperty("clave", clave);
            host = p.getProperty("host", host);
            puerto = p.getProperty("puerto", puerto);
        }
        if (usuario == null || usuario.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalStateException("El envío de correo no está configurado en este equipo "
                    + "(ver correo.properties.ejemplo).");
        }
        return new Config(host == null || host.isBlank() ? "smtp.gmail.com" : host.trim(),
                puerto == null || puerto.isBlank() ? "587" : puerto.trim(),
                usuario.trim(), clave.replace(" ", ""));
    }

    /** Guarda el motivo del fallo en ~/.svbgua/correo-error.log para que el personal pueda revisarlo. */
    public static void registrarError(Throwable t) {
        StringBuilder sb = new StringBuilder(java.time.LocalDateTime.now().toString()).append("  ");
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getClass().getSimpleName()).append(": ").append(c.getMessage()).append(" | ");
        }
        sb.append('\n');
        System.err.print(sb);
        try {
            Path dir = Path.of(System.getProperty("user.home"), ".svbgua");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("correo-error.log"), sb.toString(), java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignorar) {
            // si no se puede escribir el registro, ya se mostró en consola
        }
    }

    /** true si hay credenciales de correo configuradas. */
    public static boolean configurado() {
        try {
            leerConfig();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /** Envía el comprobante con el QR incrustado. Se llama fuera del hilo de la interfaz. */
    public static void enviarComprobante(String destino, int idVenta, String concierto, String fecha,
                                         List<String> lineas, BigDecimal total, byte[] qrPng) {
        Config cfg = leerConfig();
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", cfg.host());
        props.put("mail.smtp.port", cfg.puerto());
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout", "20000");
        props.put("mail.smtp.writetimeout", "20000");

        Session sesion = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.usuario(), cfg.clave());
            }
        });

        try {
            MimeMessage msg = new MimeMessage(sesion);
            msg.setFrom(new InternetAddress(cfg.usuario(), "TICKTES UMG", "UTF-8"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino.trim()));
            msg.setSubject("Tus boletos · " + concierto + " · Venta No. " + idVenta, "UTF-8");

            MimeMultipart cuerpo = new MimeMultipart("related");
            MimeBodyPart html = new MimeBodyPart();
            html.setContent(html(idVenta, concierto, fecha, lineas, total), "text/html; charset=UTF-8");
            cuerpo.addBodyPart(html);

            MimeBodyPart imagen = new MimeBodyPart();
            imagen.setDataHandler(new DataHandler(new ByteArrayDataSource(qrPng, "image/png")));
            imagen.setContentID("<qr>");
            imagen.setDisposition(MimeBodyPart.INLINE);
            imagen.setFileName("qr-venta-" + idVenta + ".png");
            cuerpo.addBodyPart(imagen);

            msg.setContent(cuerpo);
            Transport.send(msg);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo enviar el correo: " + e.getMessage(), e);
        }
    }

    private static String html(int idVenta, String concierto, String fecha, List<String> lineas, BigDecimal total) {
        StringBuilder items = new StringBuilder();
        for (String l : lineas) {
            items.append("<li style=\"margin:4px 0\">").append(esc(l)).append("</li>");
        }
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:auto;color:#222\">"
                + "<h2 style=\"color:#6D28D9;margin-bottom:0\">TICKETS UMG</h2>"
                + "<p style=\"margin-top:2px;color:#666\">Comprobante de compra</p>"
                + "<h3 style=\"margin-bottom:4px\">" + esc(concierto) + "</h3>"
                + (fecha == null || fecha.isBlank() ? "" : "<p style=\"margin:0;color:#555\">" + esc(fecha) + "</p>")
                + "<p><b>Venta No. " + idVenta + "</b></p>"
                + "<ul style=\"padding-left:18px\">" + items + "</ul>"
                + "<p style=\"font-size:18px\"><b>Total: " + esc(Fmt.money(total)) + "</b></p>"
                + "<p>Presenta este código QR en la entrada:</p>"
                + "<p style=\"text-align:center\"><img src=\"cid:qr\" width=\"240\" height=\"240\" alt=\"Código QR\"></p>"
                + "<p style=\"color:#888;font-size:12px\">Gracias por tu compra.</p></div>";
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
