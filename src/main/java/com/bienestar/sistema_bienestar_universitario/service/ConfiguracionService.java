package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.model.ConfiguracionSistema;
import com.bienestar.sistema_bienestar_universitario.repository.ConfiguracionSistemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ConfiguracionService {

    private final ConfiguracionSistemaRepository configuracionSistemaRepository;

    // Hora por defecto si el administrador no configura una: 6:00 pm
    public static final LocalTime HORA_CIERRE_DEFECTO = LocalTime.of(18, 0);

    @Transactional(readOnly = true)
    public ConfiguracionSistema obtenerConfiguracion() {
        return configuracionSistemaRepository
                .findTopByOrderByIdAsc()
                .orElseGet(ConfiguracionSistema::new);
    }

    // Hora efectiva de cierre: la configurada, o 18:00 si está vacía
    @Transactional(readOnly = true)
    public LocalTime horaEfectiva() {
        LocalTime hora = obtenerConfiguracion().getHoraCierre();
        return hora != null ? hora : HORA_CIERRE_DEFECTO;
    }

    // True si ya pasó la hora de cierre (o es exactamente esa hora)
    @Transactional(readOnly = true)
    public boolean estaCerrado() {
        return !LocalTime.now().isBefore(horaEfectiva());
    }

    @Transactional(readOnly = true)
    public LocalDate fechaUltimoCierre() {
        return obtenerConfiguracion().getFechaUltimoCierre();
    }

    @Transactional
    public void registrarCierreDiario(LocalDate fecha) {
        ConfiguracionSistema config = obtenerConfiguracion();
        config.setFechaUltimoCierre(fecha);
        configuracionSistemaRepository.save(config);
    }

    // null = sin hora configurada (usa la hora por defecto 18:00)
    @Transactional
    public void guardarHoraCierre(LocalTime hora) {
        try {
            ConfiguracionSistema config = obtenerConfiguracion();
            if (config.getId() == null) {
                config.setId(1);
            }
            config.setHoraCierre(hora);
            configuracionSistemaRepository.saveAndFlush(config);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            // Reintento una vez con entidad fresca (concurrencia con @Scheduled)
            ConfiguracionSistema fresco = configuracionSistemaRepository.findTopByOrderByIdAsc()
                    .orElseGet(ConfiguracionSistema::new);
            if (fresco.getId() == null) fresco.setId(1);
            fresco.setHoraCierre(hora);
            configuracionSistemaRepository.saveAndFlush(fresco);
        }
    }
}
