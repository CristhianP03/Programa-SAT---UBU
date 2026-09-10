package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {

    // Condiciones opcionales compartidas por el histórico y el resumen.
    // Cada parámetro nulo se ignora: se compara la columna contra sí misma
    // mediante COALESCE (evita "IS NULL" que PostgreSQL no puede tipar).
    String FILTROS_HISTORIAL =
            "a.servicio.area.id = COALESCE(:areaId, a.servicio.area.id) " +
            "AND a.servicio.id = COALESCE(:servicioId, a.servicio.id) " +
            "AND a.estado = COALESCE(:estado, a.estado) " +
            "AND a.estado <> 'CANCELADO' " +
            "AND a.fechaHoraRegistro >= COALESCE(:desde, a.fechaHoraRegistro) " +
            "AND a.fechaHoraRegistro < COALESCE(:hasta, a.fechaHoraRegistro) " +
            "AND (LOWER(a.estudiante.nombres) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(a.estudiante.apellidos) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(a.estudiante.cedula) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR " +
            "LOWER(a.estudiante.correoInstitucional) LIKE LOWER(CONCAT('%', :busqueda, '%')))";

    List<Asistencia> findByEstudianteIdOrderByFechaHoraRegistroDesc(Integer estudianteId);

    @Query("SELECT a FROM Asistencia a " +
           "WHERE a.fechaHoraRegistro >= :inicio AND a.fechaHoraRegistro < :fin " +
           "AND a.estado <> 'CANCELADO' " +
           "ORDER BY a.fechaHoraRegistro ASC")
    List<Asistencia> findByRangoFechas(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT COUNT(a) > 0 FROM Asistencia a " +
           "WHERE a.estudiante.id = :estudianteId " +
           "AND a.servicio.id = :servicioId " +
           "AND a.fechaHoraRegistro >= :inicio AND a.fechaHoraRegistro < :fin " +
           "AND a.estado IN ('PENDIENTE_VERIFICACION', 'ESPERANDO', 'EN_ATENCION')")
    boolean existeAsistenciaActivaHoy(
            @Param("estudianteId") Integer estudianteId,
            @Param("servicioId")   Integer servicioId,
            @Param("inicio")       LocalDateTime inicio,
            @Param("fin")          LocalDateTime fin);

    @Query("SELECT COUNT(a) > 0 FROM Asistencia a " +
           "WHERE a.usuarioAtendio.id = :usuarioId " +
           "AND a.estado = 'EN_ATENCION' " +
           "AND a.fechaHoraRegistro >= :inicio AND a.fechaHoraRegistro < :fin")
    boolean existeAtencionEnCurso(
            @Param("usuarioId") Integer usuarioId,
            @Param("inicio")    LocalDateTime inicio,
            @Param("fin")       LocalDateTime fin);

    @Query("SELECT COUNT(a) FROM Asistencia a " +
           "WHERE a.servicio.area.id = :areaId " +
           "AND a.estado = 'ESPERANDO' " +
           "AND a.numeroTurno < :numeroTurno " +
           "AND a.fechaHoraRegistro >= :inicio AND a.fechaHoraRegistro < :fin")
    int countEnColaAntes(
            @Param("areaId") Integer areaId,
            @Param("numeroTurno") Integer numeroTurno,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT a FROM Asistencia a " +
           "WHERE a.servicio.area.id = :areaId " +
           "AND a.estado IN ('ESPERANDO', 'EN_ATENCION', 'NO_SE_PRESENTO') " +
           "AND a.fechaHoraRegistro >= :inicio AND a.fechaHoraRegistro < :fin " +
           "ORDER BY a.numeroTurno ASC")
    List<Asistencia> findActivosByAreaIdOrderByNumeroTurnoAsc(
            @Param("areaId") Integer areaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("SELECT a.estado, a.servicio.nombre, COUNT(a) " +
           "FROM Asistencia a " +
           "WHERE a.fechaHoraRegistro >= :inicio AND a.fechaHoraRegistro < :fin " +
           "AND a.estado <> 'CANCELADO' " +
           "GROUP BY a.estado, a.servicio.nombre")
    List<Object[]> countByServicioYEstado(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // El orden se aplica desde el Pageable (columnas seleccionables en reportes)
    @Query("SELECT a FROM Asistencia a WHERE " + FILTROS_HISTORIAL)
    Page<Asistencia> buscarAsistencias(
            @Param("areaId") Integer areaId,
            @Param("servicioId") Integer servicioId,
            @Param("estado") Asistencia.Estado estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("busqueda") String busqueda,
            Pageable pageable);

    @Query("SELECT a FROM Asistencia a WHERE " + FILTROS_HISTORIAL)
    List<Asistencia> buscarAsistenciasSinPaginar(
            @Param("areaId") Integer areaId,
            @Param("servicioId") Integer servicioId,
            @Param("estado") Asistencia.Estado estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("busqueda") String busqueda,
            Sort sort);

    @Query("SELECT a.estado, COUNT(a) FROM Asistencia a WHERE " + FILTROS_HISTORIAL +
           " GROUP BY a.estado")
    List<Object[]> resumenEstados(
            @Param("areaId") Integer areaId,
            @Param("servicioId") Integer servicioId,
            @Param("estado") Asistencia.Estado estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("busqueda") String busqueda);

    @Query("SELECT a.estado, a.servicio.area.nombre, COUNT(a) " +
           "FROM Asistencia a " +
           "WHERE a.fechaHoraRegistro >= :inicio AND a.fechaHoraRegistro < :fin " +
           "AND a.estado <> 'CANCELADO' " +
           "GROUP BY a.estado, a.servicio.area.nombre")
    List<Object[]> countByAreaYEstado(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    // Cierre masivo de la cola: los registros activos pasan a ATENDIDO.
    // fecha_hora_atencion = fecha_hora_cierre para cumplir chk_estado_coherencia.
    @Modifying
    @Transactional
    @Query("UPDATE Asistencia a SET a.estado = 'ATENDIDO', " +
           "a.fechaHoraAtencion = :hora, a.fechaHoraCierre = :hora, " +
           "a.ultimoCambioEstado = :hora " +
           "WHERE a.estado IN ('ESPERANDO', 'EN_ATENCION') " +
           "AND a.fechaHoraRegistro < :fin")
    int cerrarActivosHasta(
            @Param("hora") LocalDateTime hora,
            @Param("fin") LocalDateTime fin);
}
