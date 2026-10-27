package com.svbgua.kiosco.modelo;

/** Tipos de localidad que maneja el recinto de demostración. */
public enum TipoLocalidad {
    VIP("VIP", "Zona exclusiva frente al escenario"),
    PREFERENCIAL("Preferencial", "Zona intermedia con excelente vista"),
    GENERAL("General", "Admisión general de pie"),
    MESA("Mesa", "Asiento en mesa, se vende por separado");

    private final String etiqueta;
    private final String descripcion;

    TipoLocalidad(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** Reconoce el tipo por el nombre que el administrador le puso a la localidad en el backend. */
    public static TipoLocalidad desdeNombre(String nombre) {
        String n = java.text.Normalizer.normalize(nombre == null ? "" : nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT);
        if (n.contains("vip")) return VIP;
        if (n.contains("pref")) return PREFERENCIAL;
        if (n.contains("mesa")) return MESA;
        return GENERAL;
    }

    /** Las mesas se venden completas (una sola unidad), no por boleto. */
    public boolean esMesa() {
        return this == MESA;
    }
}
