package com.bienestar.sistema_bienestar_universitario.dto;

import com.bienestar.sistema_bienestar_universitario.model.Asistencia;

import java.time.LocalDate;

/**
 * Filtros seleccionables para el histórico de asistencias.
 * Los valores nulos se ignoran (sin filtro).
 */
public record FiltroReporte(
        Integer areaId,
        Integer servicioId,
        Asistencia.Estado estado,
        LocalDate desde,
        LocalDate hasta,
        String busqueda) {
}
