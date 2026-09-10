package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.model.Area;
import com.bienestar.sistema_bienestar_universitario.model.TurnoDiario;
import com.bienestar.sistema_bienestar_universitario.repository.AreaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.TurnoDiarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private final TurnoDiarioRepository turnoDiarioRepository;
    private final AreaRepository         areaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // Turnos consecutivos por área y por día
    @Transactional
    public Integer siguienteTurno(Integer areaId) {

        LocalDate hoy = LocalDate.now();

        TurnoDiario turnoDiario;
        try {
            turnoDiario = turnoDiarioRepository
                    .findByAreaIdAndFechaForUpdate(areaId, hoy)
                    .orElseGet(() -> crearTurnoDiarioSiNoExiste(areaId, hoy));
        } catch (DataIntegrityViolationException e) {
            turnoDiario = turnoDiarioRepository
                    .findByAreaIdAndFechaForUpdate(areaId, hoy)
                    .orElseThrow(() -> new IllegalStateException(
                            "Error al obtener el turno diario."));
        }

        turnoDiarioRepository.incrementarTurno(areaId, hoy);
        entityManager.refresh(turnoDiario);

        return turnoDiario.getUltimoTurno();
    }

    private TurnoDiario crearTurnoDiarioSiNoExiste(Integer areaId, LocalDate hoy) {
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El área no existe."));

        TurnoDiario nuevo = new TurnoDiario();
        nuevo.setArea(area);
        nuevo.setFecha(hoy);
        nuevo.setUltimoTurno(0);

        try {
            return turnoDiarioRepository.saveAndFlush(nuevo);
        } catch (DataIntegrityViolationException e) {
            return turnoDiarioRepository
                    .findByAreaIdAndFechaForUpdate(areaId, hoy)
                    .orElseThrow(() -> new IllegalStateException(
                            "Error al crear el turno diario."));
        }
    }
}
