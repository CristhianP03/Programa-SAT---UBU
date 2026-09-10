package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.TurnoDiario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TurnoDiarioRepository extends JpaRepository<TurnoDiario, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TurnoDiario t WHERE t.area.id = :areaId AND t.fecha = :fecha")
    Optional<TurnoDiario> findByAreaIdAndFechaForUpdate(
            @Param("areaId") Integer areaId,
            @Param("fecha") LocalDate fecha);

    @Modifying
    @Transactional
    @Query("UPDATE TurnoDiario t SET t.ultimoTurno = t.ultimoTurno + 1 " +
           "WHERE t.area.id = :areaId AND t.fecha = :fecha")
    void incrementarTurno(
            @Param("areaId") Integer areaId,
            @Param("fecha") LocalDate fecha);
}
