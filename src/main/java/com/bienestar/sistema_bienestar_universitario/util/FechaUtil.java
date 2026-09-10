package com.bienestar.sistema_bienestar_universitario.util;

import java.time.LocalDate;

public final class FechaUtil {

    private FechaUtil() {
    }

    /**
     * Parsea una fecha ISO (yyyy-MM-dd). Devuelve null si el valor está vacío
     * o malformado, en lugar de lanzar una excepción.
     */
    public static LocalDate parsearFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
