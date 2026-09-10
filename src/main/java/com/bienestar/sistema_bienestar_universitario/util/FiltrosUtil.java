package com.bienestar.sistema_bienestar_universitario.util;

import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

public final class FiltrosUtil {

    private FiltrosUtil() {
    }

    /**
     * Convierte el estado recibido del formulario a enum.
     * Devuelve null si está vacío o es inválido (nunca lanza).
     */
    public static Asistencia.Estado parsearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return Asistencia.Estado.valueOf(estado);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Convierte el número de página recibido del formulario.
     * Devuelve 0 si está vacío o es inválido (nunca lanza).
     */
    public static int parsearPagina(String pagina) {
        if (pagina == null || pagina.isBlank()) {
            return 0;
        }
        try {
            int p = Integer.parseInt(pagina);
            return Math.max(p, 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Ventana de números de página alrededor de la página actual
     * (para la barra de paginación de los reportes).
     */
    public static List<Integer> rangoPaginas(Page<?> pagina) {
        List<Integer> paginas = new ArrayList<>();
        int total = pagina.getTotalPages();
        if (total <= 1) {
            return paginas;
        }
        int actual = pagina.getNumber();
        int inicio = Math.max(0, actual - 2);
        int fin = Math.min(total - 1, actual + 2);
        for (int i = inicio; i <= fin; i++) {
            paginas.add(i);
        }
        return paginas;
    }
}
