package com.bienestar.sistema_bienestar_universitario.controller;
import com.bienestar.sistema_bienestar_universitario.dto.OpcionDto;
import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import com.bienestar.sistema_bienestar_universitario.model.Estudiante;
import com.bienestar.sistema_bienestar_universitario.repository.AreaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.CarreraRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EtniaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.FacultadRepository;
import com.bienestar.sistema_bienestar_universitario.repository.ServicioRepository;
import com.bienestar.sistema_bienestar_universitario.repository.SexoRepository;
import com.bienestar.sistema_bienestar_universitario.service.AsistenciaService;
import com.bienestar.sistema_bienestar_universitario.service.ConfiguracionService;
import com.bienestar.sistema_bienestar_universitario.service.EstudianteService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/estudiante")
@RequiredArgsConstructor
public class EstudianteController {

    public static final String SESION_ASISTENCIA_PENDIENTE = "asistenciaPendienteId";

    private final EstudianteService      estudianteService;
    private final AsistenciaService      asistenciaService;
    private final ConfiguracionService   configuracionService;
    private final FacultadRepository     facultadRepository;
    private final CarreraRepository      carreraRepository;
    private final AreaRepository         areaRepository;
    private final ServicioRepository     servicioRepository;
    private final SexoRepository         sexoRepository;
    private final EtniaRepository        etniaRepository;

    @GetMapping("/registro")
    public String registro(@RequestParam(required = false) boolean editar,
                           Model model, HttpSession session) {
        model.addAttribute("facultades", facultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("areas",      areaRepository.findByActivoTrueOrderByNombreAsc());
        model.addAttribute("sexos",      sexoRepository.findAllByOrderByNombreAsc());
        model.addAttribute("etnias",     etniaRepository.findAllByOrderByNombreAsc());
        model.addAttribute("editar",     false);
        model.addAttribute("registroCerrado", configuracionService.estaCerrado());

        if (editar) {
            Integer asistenciaId = (Integer) session.getAttribute(SESION_ASISTENCIA_PENDIENTE);
            if (asistenciaId != null) {
                Asistencia asistencia = asistenciaService.buscarPorId(asistenciaId);
                if (asistencia != null && asistenciaService.puedeEditarDatos(asistencia)) {
                    Estudiante estudiante = asistencia.getEstudiante();
                    model.addAttribute("editar",       true);
                    model.addAttribute("asistenciaId", asistencia.getId());
                    if (!model.containsAttribute("cedula")) {
                        model.addAttribute("cedula",     estudiante.getCedula());
                        model.addAttribute("nombres",    estudiante.getNombres());
                        model.addAttribute("apellidos",  estudiante.getApellidos());
                        model.addAttribute("correo",     estudiante.getCorreoInstitucional());
                        model.addAttribute("sexoId",     estudiante.getSexo().getId());
                        model.addAttribute("etniaId",    estudiante.getEtnia().getId());
                        model.addAttribute("carreraId",  estudiante.getCarrera().getId());
                        model.addAttribute("facultadId", estudiante.getCarrera().getFacultad().getId());
                        model.addAttribute("servicioId", asistencia.getServicio().getId());
                        model.addAttribute("areaId",     asistencia.getServicio().getArea().getId());
                    }
                } else {
                    session.removeAttribute(SESION_ASISTENCIA_PENDIENTE);
                }
            }
        }

        return "estudiante/registro";
    }

    @GetMapping("/carreras")
    @ResponseBody
    public Object carrerasPorFacultad(@RequestParam Integer facultadId) {
        try {
            return carreraRepository.findByFacultadIdOrderByNombreAsc(facultadId)
                    .stream()
                    .map(c -> new OpcionDto(c.getId(), c.getNombre()))
                    .toList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @GetMapping("/servicios-por-area")
    @ResponseBody
    public Object serviciosPorArea(@RequestParam Integer areaId) {
        try {
            return servicioRepository.findByAreaIdAndActivoTrueOrderByNombreAsc(areaId)
                    .stream()
                    .map(s -> new OpcionDto(s.getId(), s.getNombre()))
                    .toList();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    @PostMapping("/registrar")
    public String registrar(
            @RequestParam String  cedula,
            @RequestParam String  nombres,
            @RequestParam String  apellidos,
            @RequestParam String  correoInstitucional,
            @RequestParam Integer sexoId,
            @RequestParam Integer etniaId,
            @RequestParam Integer carreraId,
            @RequestParam Integer servicioId,
            @RequestParam(required = false) Integer facultadId,
            @RequestParam(required = false) Integer areaId,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        try {
            Estudiante estudiante = estudianteService.buscarOCrear(
                    cedula, nombres, apellidos, correoInstitucional, sexoId, etniaId, carreraId);

            Asistencia asistencia = asistenciaService.registrar(
                    estudiante.getId(), servicioId);

            session.setAttribute(SESION_ASISTENCIA_PENDIENTE, asistencia.getId());

            int posicion = asistenciaService.contarEnColaAntes(
                    asistencia.getServicio().getArea().getId(),
                    asistencia.getNumeroTurno());

            redirectAttrs.addFlashAttribute("turno",        asistencia.getTurnoTexto());
            redirectAttrs.addFlashAttribute("servicio",     asistencia.getServicio().getNombre());
            redirectAttrs.addFlashAttribute("area",         asistencia.getServicio().getArea().getNombre());
            redirectAttrs.addFlashAttribute("posicion",     posicion);
            redirectAttrs.addFlashAttribute("asistenciaId", asistencia.getId());
            redirectAttrs.addFlashAttribute("editableHasta",
                    asistencia.getFechaHoraRegistro().plusMinutes(2).toString());
            redirectAttrs.addFlashAttribute("mensaje",
                    "Registro creado. Ya estás en la cola de atención.");
            return "redirect:/estudiante/turno";

        } catch (DataIntegrityViolationException e) {
            redirectAttrs.addFlashAttribute("error",
                    "Error de integridad de datos. Verifica que la cédula o correo no estén duplicados.");
            repoblarFormulario(redirectAttrs, cedula, nombres, apellidos, correoInstitucional,
                    sexoId, etniaId, facultadId, carreraId, areaId, servicioId);
            return "redirect:/estudiante/registro";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            repoblarFormulario(redirectAttrs, cedula, nombres, apellidos, correoInstitucional,
                    sexoId, etniaId, facultadId, carreraId, areaId, servicioId);
            return "redirect:/estudiante/registro";
        }
    }

    @PostMapping("/editar-datos")
    public String editarDatos(
            @RequestParam String  cedula,
            @RequestParam String  nombres,
            @RequestParam String  apellidos,
            @RequestParam String  correoInstitucional,
            @RequestParam Integer sexoId,
            @RequestParam Integer etniaId,
            @RequestParam Integer carreraId,
            @RequestParam Integer servicioId,
            @RequestParam(required = false) Integer facultadId,
            @RequestParam(required = false) Integer areaId,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Integer asistenciaId = (Integer) session.getAttribute(SESION_ASISTENCIA_PENDIENTE);
        if (asistenciaId == null) {
            redirectAttrs.addFlashAttribute("error",
                    "No hay un registro pendiente para editar.");
            return "redirect:/estudiante/registro";
        }

        try {
            Asistencia asistencia = asistenciaService.buscarPendienteEditable(asistenciaId);
            Estudiante estudiante = asistencia.getEstudiante();

            estudianteService.actualizarDatos(
                    estudiante, cedula, nombres, apellidos, correoInstitucional,
                    sexoId, etniaId, carreraId);

            if (!servicioId.equals(asistencia.getServicio().getId())) {
                asistencia = asistenciaService.cambiarServicioPendiente(asistenciaId, servicioId);
            }

            int posicion = asistenciaService.contarEnColaAntes(
                    asistencia.getServicio().getArea().getId(),
                    asistencia.getNumeroTurno());

            redirectAttrs.addFlashAttribute("turno",        asistencia.getTurnoTexto());
            redirectAttrs.addFlashAttribute("servicio",     asistencia.getServicio().getNombre());
            redirectAttrs.addFlashAttribute("area",         asistencia.getServicio().getArea().getNombre());
            redirectAttrs.addFlashAttribute("posicion",     posicion);
            redirectAttrs.addFlashAttribute("asistenciaId", asistencia.getId());
            redirectAttrs.addFlashAttribute("editableHasta",
                    asistencia.getFechaHoraRegistro().plusMinutes(2).toString());
            redirectAttrs.addFlashAttribute("mensaje",
                    "Tus datos fueron actualizados. Tu turno se mantiene en la cola.");
            return "redirect:/estudiante/turno";

        } catch (DataIntegrityViolationException e) {
            redirectAttrs.addFlashAttribute("error",
                    "Error de integridad de datos. Verifica que la cédula o el correo no pertenezcan a otro estudiante.");
            repoblarFormulario(redirectAttrs, cedula, nombres, apellidos, correoInstitucional,
                    sexoId, etniaId, facultadId, carreraId, areaId, servicioId);
            return "redirect:/estudiante/registro?editar=true";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            repoblarFormulario(redirectAttrs, cedula, nombres, apellidos, correoInstitucional,
                    sexoId, etniaId, facultadId, carreraId, areaId, servicioId);
            return "redirect:/estudiante/registro?editar=true";
        }
    }

    @GetMapping("/buscar-por-cedula")
    @ResponseBody
    public Object buscarPorCedula(@RequestParam String cedula) {
        try {
            if (cedula == null || cedula.length() != 10 || !cedula.matches("\\d{10}")) {
                return java.util.Collections.emptyMap();
            }
            return estudianteService.buscarPorCedula(cedula.trim())
                    .map(e -> {
                        java.util.Map<String, Object> m = new java.util.HashMap<>();
                        m.put("cedula", e.getCedula());
                        m.put("nombres", e.getNombres());
                        m.put("apellidos", e.getApellidos());
                        m.put("correo", e.getCorreoInstitucional());
                        m.put("sexoId", e.getSexo() != null ? e.getSexo().getId() : null);
                        m.put("sexoNombre", e.getSexo() != null ? e.getSexo().getNombre() : null);
                        m.put("etniaId", e.getEtnia() != null ? e.getEtnia().getId() : null);
                        m.put("etniaNombre", e.getEtnia() != null ? e.getEtnia().getNombre() : null);
                        m.put("carreraId", e.getCarrera() != null ? e.getCarrera().getId() : null);
                        m.put("carreraNombre", e.getCarrera() != null ? e.getCarrera().getNombre() : null);
                        m.put("facultadId", e.getCarrera() != null && e.getCarrera().getFacultad() != null ? e.getCarrera().getFacultad().getId() : null);
                        m.put("facultadNombre", e.getCarrera() != null && e.getCarrera().getFacultad() != null ? e.getCarrera().getFacultad().getNombre() : null);
                        return m;
                    })
                    .orElse(java.util.Collections.emptyMap());
        } catch (Exception ex) {
            return java.util.Collections.emptyMap();
        }
    }

    @GetMapping("/turno")
    public String turno() {
        return "estudiante/turno";
    }

    // -------------------------------------------------------
    // Vuelve a mostrar el formulario con los datos que el
    // estudiante ya ingresó cuando ocurrió un error, para que
    // corrija sin perder el resto de la información.
    // -------------------------------------------------------
    private void repoblarFormulario(RedirectAttributes redirectAttrs,
                                    String cedula, String nombres, String apellidos,
                                    String correo,
                                    Integer sexoId, Integer etniaId,
                                    Integer facultadId, Integer carreraId,
                                    Integer areaId, Integer servicioId) {
        redirectAttrs.addFlashAttribute("cedula",     cedula);
        redirectAttrs.addFlashAttribute("nombres",    nombres);
        redirectAttrs.addFlashAttribute("apellidos",  apellidos);
        redirectAttrs.addFlashAttribute("correo",     correo);
        redirectAttrs.addFlashAttribute("sexoId",     sexoId);
        redirectAttrs.addFlashAttribute("etniaId",    etniaId);
        redirectAttrs.addFlashAttribute("facultadId", facultadId);
        redirectAttrs.addFlashAttribute("carreraId",  carreraId);
        redirectAttrs.addFlashAttribute("areaId",     areaId);
        redirectAttrs.addFlashAttribute("servicioId", servicioId);
    }
}
