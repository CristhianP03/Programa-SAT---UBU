package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.LogAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogAccesoRepository extends JpaRepository<LogAcceso, Integer> {

    // Obtener historial de accesos de un usuario específico
    // Ordenado del más reciente al más antiguo
    List<LogAcceso> findByUsuarioIdOrderByFechaHoraDesc(Integer usuarioId);

    // Buscar notificaciones pendientes de enviar
    // El servicio de correo las procesa y marca como enviadas
    List<LogAcceso> findByNotificacionEnviadaFalse();
}
