package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import com.bienestar.sistema_bienestar_universitario.model.Asistencia.Estado;
import com.bienestar.sistema_bienestar_universitario.model.Estudiante;
import com.bienestar.sistema_bienestar_universitario.model.Servicio;
import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.bienestar.sistema_bienestar_universitario.repository.AsistenciaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EstudianteRepository;
import com.bienestar.sistema_bienestar_universitario.repository.ServicioRepository;
import com.bienestar.sistema_bienestar_universitario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository      asistenciaRepository;
    private final EstudianteRepository      estudianteRepository;
    private final ServicioRepository        servicioRepository;
    private final UsuarioRepository         usuarioRepository;
    private final TurnoService              turnoService;
    private final CorreoService             correoService;
    private final ConfiguracionService      configuracionService;

    // Ventana de 2 minutos para que el estudiante corrija sus datos
    private static final int EDICION_LIMITE_SEGUNDOS = 120;

    @Transactional
    public Asistencia registrar(Integer estudianteId, Integer servicioId) {

        if (configuracionService.estaCerrado()) {
            throw new IllegalStateException(
                    "Los registros están cerrados. El horario de registro ya terminó hoy.");
        }

        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new IllegalArgumentException("Estudiante no encontrado."));

        Servicio servicio = servicioRepository.findById(servicioId)
                .orElseThrow(() -> new IllegalArgumentException("Servicio no encontrado."));

        if (!servicio.getActivo()) {
            throw new IllegalStateException(
                    "El servicio seleccionado no está disponible.");
        }

        boolean yaRegistrado = asistenciaRepository.existeAsistenciaActivaHoy(
                estudianteId, servicioId,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());

        if (yaRegistrado) {
            throw new IllegalStateException(
                    "Ya tienes un registro activo para este servicio hoy.");
        }

        boolean esPrimeraVez = asistenciaRepository
                .findByEstudianteIdOrderByFechaHoraRegistroDesc(estudianteId)
                .stream()
                .noneMatch(a -> a.getServicio().getId().equals(servicioId)
                             && a.getEstado() == Estado.ATENDIDO);

        Integer turno = turnoService.siguienteTurno(servicio.getArea().getId());

        Asistencia asistencia = new Asistencia();
        asistencia.setEstudiante(estudiante);
        asistencia.setServicio(servicio);
        asistencia.setNumeroTurno(turno);
        // Sin verificación previa: el estudiante entra directo a la cola
        asistencia.setEstado(Estado.ESPERANDO);
        asistencia.setEsPrimeraVez(esPrimeraVez);

        Asistencia guardada;
        try {
            guardada = asistenciaRepository.save(asistencia);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "Ya tienes un registro activo para este servicio hoy.");
        }

        correoService.enviarCorreoRegistroInformativoAsync(
                estudiante.getCorreoInstitucional(),
                estudiante.getNombres(),
                servicio.getArea().getNombre(),
                servicio.getNombre(),
                guardada.getTurnoTexto());

        notificarEncargadoNuevoTurnoAsync(guardada);

        return guardada;
    }

    @Transactional(readOnly = true)
    public Asistencia buscarPorId(Integer id) {
        return asistenciaRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean puedeEditarDatos(Asistencia asistencia) {
        return asistencia != null
                && asistencia.getEstado() == Estado.ESPERANDO
                && segundosDesdeRegistro(asistencia) <= EDICION_LIMITE_SEGUNDOS;
    }

    @Transactional(readOnly = true)
    public Asistencia buscarPendienteEditable(Integer asistenciaId) {
        Asistencia asistencia = obtenerOLanzar(asistenciaId);
        if (asistencia.getEstado() != Estado.ESPERANDO) {
            throw new IllegalStateException(
                    "Este registro ya está en atención o terminó; ya no puedes modificar tus datos.");
        }
        if (segundosDesdeRegistro(asistencia) > EDICION_LIMITE_SEGUNDOS) {
            throw new IllegalStateException(
                    "El tiempo para corregir tus datos (2 minutos) ha expirado.");
        }
        return asistencia;
    }

    @Transactional
    public Asistencia cambiarServicioPendiente(Integer asistenciaId, Integer nuevoServicioId) {
        Asistencia asistencia = buscarPendienteEditable(asistenciaId);

        Servicio nuevo = servicioRepository.findById(nuevoServicioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El servicio seleccionado no existe."));

        if (!nuevo.getActivo()) {
            throw new IllegalStateException(
                    "El servicio seleccionado no está disponible.");
        }

        boolean yaRegistrado = asistenciaRepository.existeAsistenciaActivaHoy(
                asistencia.getEstudiante().getId(),
                nuevoServicioId,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());

        if (yaRegistrado) {
            throw new IllegalStateException(
                    "Ya tienes un registro activo para ese servicio hoy.");
        }

        Integer turno = turnoService.siguienteTurno(nuevo.getArea().getId());
        asistencia.setServicio(nuevo);
        asistencia.setNumeroTurno(turno);
        return asistenciaRepository.save(asistencia);
    }

    @Transactional
    public Asistencia iniciarAtencion(Integer asistenciaId, Integer usuarioId) {

        Asistencia asistencia = obtenerOLanzar(asistenciaId);

        if (asistencia.getEstado() != Estado.ESPERANDO) {
            throw new IllegalStateException(
                    "Solo se puede iniciar atención a registros en estado ESPERANDO.");
        }

        Usuario encargado = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Encargado no encontrado."));

        if (encargado.getArea() == null ||
                !encargado.getArea().getId().equals(asistencia.getServicio().getArea().getId())) {
            throw new IllegalStateException(
                    "No puedes atender asistencias de otro área.");
        }

        LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
        LocalDateTime finDia    = LocalDate.now().plusDays(1).atStartOfDay();

        if (asistenciaRepository.existeAtencionEnCurso(
                encargado.getId(), inicioDia, finDia)) {
            throw new IllegalStateException(
                    "Ya tienes una atención en curso. Finalízala antes de atender a otro estudiante.");
        }

        int turnosAnteriores = asistenciaRepository.countEnColaAntes(
                encargado.getArea().getId(),
                asistencia.getNumeroTurno(),
                inicioDia, finDia);

        if (turnosAnteriores > 0) {
            throw new IllegalStateException(
                    "Debes atender en orden: hay turnos anteriores esperando en esta área.");
        }

        asistencia.setEstado(Estado.EN_ATENCION);
        asistencia.setUsuarioAtendio(encargado);
        asistencia.setFechaHoraAtencion(LocalDateTime.now());
        return asistenciaRepository.save(asistencia);
    }

    @Transactional
    public Asistencia finalizarAtencion(Integer asistenciaId, String observaciones) {

        Asistencia asistencia = obtenerOLanzar(asistenciaId);

        if (asistencia.getEstado() != Estado.EN_ATENCION) {
            throw new IllegalStateException(
                    "Solo se puede finalizar una atención en estado EN_ATENCION.");
        }

        asistencia.setEstado(Estado.ATENDIDO);
        asistencia.setObservaciones(observaciones);
        asistencia.setFechaHoraCierre(LocalDateTime.now());
        return asistenciaRepository.save(asistencia);
    }

    @Transactional
    public Asistencia marcarNoSePresento(Integer asistenciaId) {

        Asistencia asistencia = obtenerOLanzar(asistenciaId);

        if (asistencia.getEstado() != Estado.ESPERANDO) {
            throw new IllegalStateException(
                    "Solo se puede marcar como no presentado desde estado ESPERANDO.");
        }

        asistencia.setEstado(Estado.NO_SE_PRESENTO);
        asistencia.setFechaHoraCierre(LocalDateTime.now());
        return asistenciaRepository.save(asistencia);
    }

    @Transactional
    public Asistencia revertirNoSePresento(Integer asistenciaId) {

        Asistencia asistencia = obtenerOLanzar(asistenciaId);

        if (asistencia.getEstado() != Estado.NO_SE_PRESENTO) {
            throw new IllegalStateException(
                    "Solo se puede revertir un registro en estado NO_SE_PRESENTO.");
        }

        long minutosTranscurridos = Duration
                .between(asistencia.getUltimoCambioEstado(), LocalDateTime.now())
                .toMinutes();

        if (minutosTranscurridos > 2) {
            throw new IllegalStateException(
                    "El tiempo límite de 2 minutos para revertir ha expirado.");
        }

        boolean yaTieneOtroRegistro = asistenciaRepository.existeAsistenciaActivaHoy(
                asistencia.getEstudiante().getId(),
                asistencia.getServicio().getId(),
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());

        if (yaTieneOtroRegistro) {
            throw new IllegalStateException(
                    "El estudiante ya tiene un registro activo. No se puede revertir.");
        }

        asistencia.setEstado(Estado.ESPERANDO);
        asistencia.setFechaHoraCierre(null);
        return asistenciaRepository.save(asistencia);
    }

    @Transactional(readOnly = true)
    public List<Asistencia> obtenerActivosDelDiaPorArea(Integer areaId) {
        return asistenciaRepository
                .findActivosByAreaIdOrderByNumeroTurnoAsc(
                        areaId,
                        LocalDate.now().atStartOfDay(),
                        LocalDate.now().plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public int contarEnColaAntes(Integer areaId, Integer numeroTurno) {
        return asistenciaRepository.countEnColaAntes(
                areaId, numeroTurno,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public boolean tieneAtencionEnCurso(Integer usuarioId) {
        return asistenciaRepository.existeAtencionEnCurso(
                usuarioId,
                LocalDate.now().atStartOfDay(),
                LocalDate.now().plusDays(1).atStartOfDay());
    }

    @Transactional(readOnly = true)
    public boolean esSiguienteEnCola(Integer areaId, Integer numeroTurno) {
        return contarEnColaAntes(areaId, numeroTurno) == 0;
    }

    // -------------------------------------------------------
    // Cierre automático de la cola al llegar la hora de cierre.
    // ESPERANDO y EN_ATENCION pasan a ATENDIDO con la hora de
    // cierre configurada (fecha_hora_atencion = fecha_hora_cierre
    // para cumplir la restricción chk_estado_coherencia).
    // -------------------------------------------------------
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cerrarColaAutomaticamente() {
        LocalDate hoy = LocalDate.now();
        try {
            if (!configuracionService.estaCerrado()) {
                return;
            }
            if (hoy.equals(configuracionService.fechaUltimoCierre())) {
                return;
            }
            LocalTime horaCierre = configuracionService.horaEfectiva();
            LocalDateTime cierre = hoy.atTime(horaCierre);
            int actualizados = asistenciaRepository.cerrarActivosHasta(
                    cierre, hoy.plusDays(1).atStartOfDay());
            configuracionService.registrarCierreDiario(hoy);
            if (actualizados > 0) {
                log.info("Cola cerrada: {} registro(s) pasados a ATENDIDO a las {}.",
                        actualizados, horaCierre);
            }
        } catch (Exception e) {
            log.error("Error en el cierre automático de la cola: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void limpiarColaDeDiaAnterior() {
        try {
            LocalDate ayer = LocalDate.now().minusDays(1);
            LocalDateTime cierre = ayer.atTime(configuracionService.horaEfectiva());
            int actualizados = asistenciaRepository.cerrarActivosHasta(
                    cierre, LocalDate.now().atStartOfDay());
            if (actualizados > 0) {
                log.info("Cola reiniciada: {} registro(s) de días anteriores cerrados.",
                        actualizados);
            }
        } catch (Exception e) {
            log.error("Error al limpiar la cola de días anteriores: {}", e.getMessage());
        }
    }

    private Asistencia obtenerOLanzar(Integer id) {
        return asistenciaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asistencia no encontrada."));
    }

    private long segundosDesdeRegistro(Asistencia asistencia) {
        return Duration
                .between(asistencia.getFechaHoraRegistro(), LocalDateTime.now())
                .getSeconds();
    }

    private void notificarEncargadoNuevoTurnoAsync(Asistencia asistencia) {
        Integer areaId = asistencia.getServicio().getArea().getId();
        String nombreServicio = asistencia.getServicio().getNombre();
        String nombreEstudiante = asistencia.getEstudiante().getNombres();
        String turnoTexto = asistencia.getTurnoTexto();

        for (Usuario encargado : usuarioRepository
                .findByAreaIdAndRolAndActivoTrue(areaId, Usuario.Rol.ENCARGADO)) {
            correoService.enviarNotificacionNuevoTurnoAsync(
                    encargado.getCorreo(),
                    encargado.getNombres(),
                    nombreEstudiante,
                    nombreServicio,
                    turnoTexto);
        }
    }
}
