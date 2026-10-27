package com.svbgua.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Genera el código QR de una venta (para mostrarlo en pantalla y enviarlo por correo). */
public final class Qr {
    private Qr() {}

    /** Texto que lleva el QR: identifica la venta y sus boletos. */
    public static String contenido(int idVenta, String concierto, String fecha, BigDecimal total, List<String> lineas) {
        StringBuilder sb = new StringBuilder();
        sb.append("TICKETS UMG | VENTA No. ").append(idVenta).append('\n');
        sb.append(concierto).append('\n');
        if (fecha != null && !fecha.isBlank()) {
            sb.append(fecha).append('\n');
        }
        for (String l : lineas) {
            sb.append(l).append('\n');
        }
        sb.append("TOTAL: ").append(Fmt.money(total));
        return sb.toString();
    }

    private static BitMatrix matriz(String texto, int tam) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        try {
            return new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tam, tam, hints);
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el código QR.", e);
        }
    }

    /** Imagen JavaFX (negro sobre blanco) para mostrar en la interfaz. */
    public static Image imagen(String texto, int tam) {
        BitMatrix m = matriz(texto, tam);
        WritableImage img = new WritableImage(m.getWidth(), m.getHeight());
        PixelWriter pw = img.getPixelWriter();
        for (int y = 0; y < m.getHeight(); y++) {
            for (int x = 0; x < m.getWidth(); x++) {
                pw.setArgb(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return img;
    }

    /** PNG en bytes para adjuntar al correo. */
    public static byte[] png(String texto, int tam) {
        BitMatrix m = matriz(texto, tam);
        BufferedImage img = new BufferedImage(m.getWidth(), m.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < m.getHeight(); y++) {
            for (int x = 0; x < m.getWidth(); x++) {
                img.setRGB(x, y, m.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la imagen del QR.", e);
        }
    }
}
