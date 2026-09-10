package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.dto.FiltroReporte;
import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import com.bienestar.sistema_bienestar_universitario.repository.AsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final AsistenciaRepository asistenciaRepository;

    @Transactional(readOnly = true)
    public List<Asistencia> reportePorFecha(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
        return asistenciaRepository.findByRangoFechas(inicio, fin);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> resumenPorServicioYEstado(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
        List<Object[]> resultados = asistenciaRepository
                .countByServicioYEstado(inicio, fin);
        Map<String, Map<String, Long>> resumen = new LinkedHashMap<>();
        for (Object[] fila : resultados) {
            Asistencia.Estado estado = (Asistencia.Estado) fila[0];
            String servicio = fila[1] != null ? fila[1].toString() : "Sin servicio";
            Long cantidad = (Long) fila[2];
            resumen.computeIfAbsent(servicio, k -> new LinkedHashMap<>())
                    .put(estado.name(), cantidad);
        }
        return resumen;
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> resumenPorAreaYEstado(LocalDate fecha) {
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
        List<Object[]> resultados = asistenciaRepository.countByAreaYEstado(inicio, fin);
        Map<String, Map<String, Long>> resumen = new LinkedHashMap<>();
        for (Object[] fila : resultados) {
            Asistencia.Estado estado = (Asistencia.Estado) fila[0];
            String area = fila[1] != null ? fila[1].toString() : "Sin área";
            Long cantidad = (Long) fila[2];
            resumen.computeIfAbsent(area, k -> new LinkedHashMap<>())
                    .put(estado.name(), cantidad);
        }
        return resumen;
    }

    // -------------------------------------------------------
    // Histórico completo con filtros + paginación + ordenamiento
    // -------------------------------------------------------

    // Campos ordenables permitidos (lista blanca).
    // La clave es el valor que se recibe desde la vista.
    private static final Map<String, String> ORDENES = Map.ofEntries(
            Map.entry("fechaHoraRegistro", "fechaHoraRegistro"),
            Map.entry("fechaHoraAtencion", "fechaHoraAtencion"),
            Map.entry("fechaHoraCierre", "fechaHoraCierre"),
            Map.entry("numeroTurno", "numeroTurno"),
            Map.entry("estado", "estado"),
            Map.entry("estudiante.nombres", "estudiante.nombres"),
            Map.entry("estudiante.cedula", "estudiante.cedula"),
            Map.entry("servicio.nombre", "servicio.nombre"),
            Map.entry("area", "servicio.area.nombre"),
            Map.entry("estudiante.carrera.nombre", "estudiante.carrera.nombre"),
            Map.entry("estudiante.carrera.facultad.nombre", "estudiante.carrera.facultad.nombre"),
            Map.entry("estudiante.sexo.nombre", "estudiante.sexo.nombre"));

    @Transactional(readOnly = true)
    public Page<Asistencia> historial(FiltroReporte f, int page, int size,
                                      String orden, String dir) {
        return asistenciaRepository.buscarAsistencias(
                f.areaId(), f.servicioId(), f.estado(),
                aInicio(f.desde()), aFin(f.hasta()), normalizarBusqueda(f.busqueda()),
                PageRequest.of(Math.max(page, 0), size, buildSort(orden, dir)));
    }

    // Historial sin paginar — para la exportación a Excel/PDF.
    @Transactional(readOnly = true)
    public List<Asistencia> historialCompleto(FiltroReporte f, String orden, String dir) {
        return asistenciaRepository.buscarAsistenciasSinPaginar(
                f.areaId(), f.servicioId(), f.estado(),
                aInicio(f.desde()), aFin(f.hasta()), normalizarBusqueda(f.busqueda()),
                buildSort(orden, dir));
    }

    // Resumen por estado respetando los filtros (el estado del filtro se ignora).
    @Transactional(readOnly = true)
    public Map<String, Long> resumenEstados(FiltroReporte f) {
        List<Object[]> filas = asistenciaRepository.resumenEstados(
                f.areaId(), f.servicioId(), null,
                aInicio(f.desde()), aFin(f.hasta()), normalizarBusqueda(f.busqueda()));
        Map<String, Long> resumen = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            Asistencia.Estado estado = (Asistencia.Estado) fila[0];
            resumen.put(estado.name(), (Long) fila[1]);
        }
        return resumen;
    }

    private Sort buildSort(String orden, String dir) {
        String propiedad = ORDENES.getOrDefault(orden, "fechaHoraRegistro");
        Sort.Direction direccion = "asc".equalsIgnoreCase(dir)
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direccion, propiedad);
    }

    private LocalDateTime aInicio(LocalDate fecha) {
        return fecha != null ? fecha.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
    }

    private LocalDateTime aFin(LocalDate fecha) {
        return fecha != null ? fecha.plusDays(1).atStartOfDay() : LocalDateTime.of(9999, 12, 31, 23, 59, 59);
    }

    private String normalizarBusqueda(String busqueda) {
        if (busqueda == null || busqueda.isBlank()) {
            return "";
        }
        return busqueda.trim();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> distribucion(FiltroReporte f, String categoria) {
        List<Asistencia> lista = historialCompleto(f, "fechaHoraRegistro", "desc");
        Map<String, Long> m = new LinkedHashMap<>();
        for (Asistencia a : lista) {
            String key = switch (categoria) {
                case "sexo" -> a.getEstudiante()!=null && a.getEstudiante().getSexo()!=null ? a.getEstudiante().getSexo().getNombre() : "Sin sexo";
                case "etnia" -> a.getEstudiante()!=null && a.getEstudiante().getEtnia()!=null ? a.getEstudiante().getEtnia().getNombre() : "Sin etnia";
                case "facultad" -> a.getEstudiante()!=null && a.getEstudiante().getCarrera()!=null && a.getEstudiante().getCarrera().getFacultad()!=null ? a.getEstudiante().getCarrera().getFacultad().getNombre() : "Sin facultad";
                case "carrera" -> a.getEstudiante()!=null && a.getEstudiante().getCarrera()!=null ? a.getEstudiante().getCarrera().getNombre() : "Sin carrera";
                case "area" -> a.getServicio()!=null && a.getServicio().getArea()!=null ? a.getServicio().getArea().getNombre() : "Sin área";
                case "servicio" -> a.getServicio()!=null ? a.getServicio().getNombre() : "Sin servicio";
                case "estado" -> a.getEstado()!=null ? a.getEstado().name() : "Sin estado";
                default -> "Otros";
            };
            m.merge(key, 1L, Long::sum);
        }
        return m;
    }
}
