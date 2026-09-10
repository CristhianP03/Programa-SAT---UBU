package com.bienestar.sistema_bienestar_universitario.controller;

import com.bienestar.sistema_bienestar_universitario.dto.FiltroReporte;
import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import com.bienestar.sistema_bienestar_universitario.model.Servicio;
import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.bienestar.sistema_bienestar_universitario.model.Estudiante;
import com.bienestar.sistema_bienestar_universitario.repository.AreaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.CarreraRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EstudianteRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EtniaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.FacultadRepository;
import com.bienestar.sistema_bienestar_universitario.repository.ServicioRepository;
import com.bienestar.sistema_bienestar_universitario.repository.SexoRepository;
import com.bienestar.sistema_bienestar_universitario.repository.UsuarioRepository;
import com.bienestar.sistema_bienestar_universitario.service.ConfiguracionService;
import com.bienestar.sistema_bienestar_universitario.service.CorreoService;
import com.bienestar.sistema_bienestar_universitario.service.EstudianteService;
import com.bienestar.sistema_bienestar_universitario.service.ReporteExportador;
import com.bienestar.sistema_bienestar_universitario.service.ReporteService;
import com.bienestar.sistema_bienestar_universitario.service.UsuarioService;
import com.bienestar.sistema_bienestar_universitario.util.FechaUtil;
import com.bienestar.sistema_bienestar_universitario.util.FiltrosUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private static final int TAMANO_PAGINA = 10;
    private static final String ABECEDARIO_SEGURO =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private final ReporteService          reporteService;
    private final ReporteExportador       reporteExportador;
    private final UsuarioRepository       usuarioRepository;
    private final ServicioRepository      servicioRepository;
    private final AreaRepository          areaRepository;
    private final PasswordEncoder         passwordEncoder;
    private final CorreoService           correoService;
    private final ConfiguracionService    configuracionService;
    private final UsuarioService          usuarioService;
    private final EstudianteRepository    estudianteRepository;
    private final EstudianteService       estudianteService;
    private final FacultadRepository      facultadRepository;
    private final CarreraRepository       carreraRepository;
    private final SexoRepository          sexoRepository;
    private final EtniaRepository         etniaRepository;

    // -------------------------------------------------------
    // GET /admin/dashboard
    // Vista principal del administrador.
    // -------------------------------------------------------
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("resumenDetallado", reporteService.resumenPorServicioYEstado(LocalDate.now()));
        model.addAttribute("resumenPorArea",   reporteService.resumenPorAreaYEstado(LocalDate.now()));
        model.addAttribute("asistencias",      reporteService.reportePorFecha(LocalDate.now()));
        model.addAttribute("fecha",            LocalDate.now());
        model.addAttribute("configuracion",    configuracionService.obtenerConfiguracion());
        model.addAttribute("horaEfectiva",     configuracionService.horaEfectiva());
        model.addAttribute("cerrado",          configuracionService.estaCerrado());
        return "admin/dashboard";
    }

    // -------------------------------------------------------
    // POST /admin/hora-cierre
    // Configura la hora de cierre del registro. Vacío = hora
    // por defecto (18:00). No se permite una hora anterior a
    // la hora actual.
    // -------------------------------------------------------
    @PostMapping("/hora-cierre")
    public String guardarHoraCierre(
            @RequestParam(required = false) String hora,
            RedirectAttributes redirectAttrs) {

        try {
            if (hora == null || hora.isBlank()) {
                configuracionService.guardarHoraCierre(null);
                redirectAttrs.addFlashAttribute("mensaje",
                        "Hora de cierre sin configurar. Se usará la hora por defecto (18:00).");
            } else {
                LocalTime horaCierre = LocalTime.parse(hora);
                if (!horaCierre.isAfter(LocalTime.now())) {
                    redirectAttrs.addFlashAttribute("error",
                            "La hora de cierre no puede ser anterior o igual a la hora actual.");
                    return "redirect:/admin/dashboard";
                }
                configuracionService.guardarHoraCierre(horaCierre);
                redirectAttrs.addFlashAttribute("mensaje",
                        "Hora de cierre actualizada a las " + horaCierre + ".");
            }
        } catch (DateTimeParseException e) {
            redirectAttrs.addFlashAttribute("error", "La hora ingresada no es válida.");
        }
        return "redirect:/admin/dashboard";
    }

    // -------------------------------------------------------
    // GET /admin/perfil
    // Mi perfil del administrador.
    // -------------------------------------------------------
    @GetMapping("/perfil")
    public String perfil(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        Usuario admin = obtenerAdmin(userDetails);
        model.addAttribute("personal", admin);
        model.addAttribute("esAdmin", true);
        return "personal/perfil";
    }

    // -------------------------------------------------------
    // POST /admin/perfil
    // Actualiza los datos del perfil y, opcionalmente, la
    // contraseña. Si cambia el usuario o el correo se cierra
    // la sesión para que vuelva a iniciar con los nuevos datos.
    // -------------------------------------------------------
    @PostMapping("/perfil")
    public String guardarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String nombres,
            @RequestParam String apellidos,
            @RequestParam String correo,
            @RequestParam String usuario,
            @RequestParam(required = false) String contrasenaActual,
            @RequestParam(required = false) String nuevaContrasena,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Usuario admin = obtenerAdmin(userDetails);
        String usuarioAnterior = admin.getUsuario();
        String correoAnterior = admin.getCorreo();

        try {
            admin = usuarioService.actualizarPerfil(
                    admin, nombres, apellidos, correo, usuario,
                    contrasenaActual, nuevaContrasena);
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/perfil";
        }

        boolean credencialesCambiadas = !admin.getUsuario().equals(usuarioAnterior)
                || !admin.getCorreo().equals(correoAnterior);

        if (credencialesCambiadas) {
            SecurityContextHolder.clearContext();
            if (session != null) {
                session.invalidate();
            }
            return "redirect:/auth/login?perfil=1";
        }

        redirectAttrs.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/admin/perfil";
    }

    // -------------------------------------------------------
    // GET /admin/reportes
    // Histórico completo de asistencias con filtros (área, servicio,
    // estado, rango de fechas, búsqueda por texto), ordenamiento por
    // columnas y paginación.
    // -------------------------------------------------------
    @GetMapping("/reportes")
    public String reportes(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) Integer servicioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            @RequestParam(required = false) String page,
            Model model) {

        FiltroReporte filtro = new FiltroReporte(
                areaId, servicioId, FiltrosUtil.parsearEstado(estado),
                FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);

        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm   = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";

        Page<Asistencia> pagina = reporteService.historial(
                filtro, FiltrosUtil.parsearPagina(page), TAMANO_PAGINA, ordenNorm, dirNorm);

        List<Servicio> servicios = (areaId != null)
                ? servicioRepository.findByAreaIdOrderByNombreAsc(areaId)
                : servicioRepository.findAllByOrderByNombreAsc();

        model.addAttribute("desde",          desde);
        model.addAttribute("hasta",          hasta);
        model.addAttribute("areaId",         areaId);
        model.addAttribute("servicioId",     servicioId);
        model.addAttribute("estado",         estado);
        model.addAttribute("busqueda",       busqueda);
        model.addAttribute("orden",          ordenNorm);
        model.addAttribute("dir",            dirNorm);
        model.addAttribute("areas",          areaRepository.findAllByOrderByNombreAsc());
        model.addAttribute("servicios",      servicios);
        model.addAttribute("estados",        Asistencia.Estado.values());
        model.addAttribute("pagina",         pagina);
        model.addAttribute("asistencias",    pagina.getContent());
        model.addAttribute("paginas",        FiltrosUtil.rangoPaginas(pagina));
        model.addAttribute("resumenEstados", reporteService.resumenEstados(filtro));
        model.addAttribute("total",          pagina.getTotalElements());

        return "admin/reportes";
    }

    // -------------------------------------------------------
    // GET /admin/reportes/excel | /admin/reportes/pdf
    // Exportación del histórico filtrado (respeta los mismos filtros).
    // Las columnas se eligen con los checkboxes de la vista.
    // -------------------------------------------------------
    @GetMapping("/reportes/excel")
    public void exportarExcel(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) Integer servicioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) List<String> columnas,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            HttpServletResponse response) throws IOException {

        FiltroReporte filtro = new FiltroReporte(
                areaId, servicioId, FiltrosUtil.parsearEstado(estado),
                FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);

        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm   = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";

        reporteExportador.exportarExcel(response, "Reporte de asistencias",
                reporteService.historialCompleto(filtro, ordenNorm, dirNorm),
                true, columnas);
    }

    @GetMapping("/reportes/pdf")
    public void exportarPdf(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) Integer servicioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) List<String> columnas,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            HttpServletResponse response) throws IOException {

        FiltroReporte filtro = new FiltroReporte(
                areaId, servicioId, FiltrosUtil.parsearEstado(estado),
                FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);

        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm   = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";

        Usuario admin = obtenerAdmin(userDetails);

        reporteExportador.exportarPdf(response, "Reporte de asistencias",
                admin.getNombres() + " " + admin.getApellidos(),
                reporteService.historialCompleto(filtro, ordenNorm, dirNorm),
                true, columnas);
    }

    // -------------------------------------------------------
    // GET /admin/usuarios
    // Lista todos los usuarios (activos e inactivos).
    // -------------------------------------------------------
    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAllByOrderByApellidosAsc());
        model.addAttribute("areas",    areaRepository.findByActivoTrueOrderByNombreAsc());
        return "admin/usuarios";
    }

    // -------------------------------------------------------
    // POST /admin/usuarios/crear
    // Crea un nuevo usuario SIN contraseña: se genera una
    // contraseña temporal que se envía por correo. El usuario
    // puede cambiarla después desde su perfil.
    // -------------------------------------------------------
    @PostMapping("/usuarios/crear")
    @Transactional
    public String crearUsuario(
            @RequestParam String  nombres,
            @RequestParam String  apellidos,
            @RequestParam String  correo,
            @RequestParam String  usuario,
            @RequestParam Usuario.Rol rol,
            @RequestParam(required = false) Integer areaId,
            RedirectAttributes redirectAttrs) {

        if (!StringUtils.hasText(nombres) || !StringUtils.hasText(apellidos)
                || !StringUtils.hasText(correo) || !StringUtils.hasText(usuario)) {
            redirectAttrs.addFlashAttribute("error", "Todos los campos obligatorios deben estar llenos.");
            return "redirect:/admin/usuarios";
        }

        String usuarioNormalizado = UsuarioService.normalizar(usuario);
        String correoNormalizado  = UsuarioService.normalizar(correo);

        if (usuarioRepository.existsByUsuario(usuarioNormalizado)) {
            redirectAttrs.addFlashAttribute("error",
                    "Ya existe un usuario con ese nombre de usuario.");
            return "redirect:/admin/usuarios";
        }

        if (usuarioRepository.existsByCorreo(correoNormalizado)) {
            redirectAttrs.addFlashAttribute("error",
                    "Ya existe un usuario con ese correo.");
            return "redirect:/admin/usuarios";
        }

        if (rol == Usuario.Rol.ENCARGADO && areaId == null) {
            redirectAttrs.addFlashAttribute("error",
                    "Debe seleccionar un área para el encargado.");
            return "redirect:/admin/usuarios";
        }

        String contrasenaTemporal = generarContrasenaTemporal();

        Usuario nuevo = new Usuario();
        nuevo.setNombres(nombres.trim());
        nuevo.setApellidos(apellidos.trim());
        nuevo.setCorreo(correoNormalizado);
        nuevo.setUsuario(usuarioNormalizado);
        nuevo.setContrasena(passwordEncoder.encode(contrasenaTemporal));
        nuevo.setRol(rol);

        if (areaId != null) {
            areaRepository.findById(areaId)
                    .ifPresent(nuevo::setArea);
        }

        usuarioRepository.save(nuevo);

        correoService.enviarContrasenaTemporalAsync(
                correoNormalizado,
                nuevo.getNombres(),
                usuarioNormalizado,
                contrasenaTemporal);
        redirectAttrs.addFlashAttribute("mensaje",
                "Usuario creado. Se envió la contraseña temporal a su correo.");

        return "redirect:/admin/usuarios";
    }

    // -------------------------------------------------------
    // POST /admin/usuarios/desactivar/{id}
    // Desactiva un usuario sin borrarlo.
    // Su historial de atenciones queda intacto.
    // -------------------------------------------------------
    @PostMapping("/usuarios/desactivar/{id}")
    public String desactivarUsuario(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails currentUser,
            RedirectAttributes redirectAttrs) {

        if (currentUser.getUsername()
                .equals(usuarioRepository.findById(id)
                        .map(Usuario::getUsuario).orElse(null))) {
            redirectAttrs.addFlashAttribute("error", "No puedes desactivar tu propia cuenta.");
            return "redirect:/admin/usuarios";
        }

        usuarioRepository.findById(id).ifPresentOrElse(u -> {
            u.setActivo(false);
            usuarioRepository.save(u);
            redirectAttrs.addFlashAttribute("mensaje", "Usuario desactivado correctamente.");
        }, () -> {
            redirectAttrs.addFlashAttribute("error", "Usuario no encontrado.");
        });

        return "redirect:/admin/usuarios";
    }

    // -------------------------------------------------------
    // POST /admin/usuarios/activar/{id}
    // Reactiva un usuario que había sido desactivado.
    // -------------------------------------------------------
    @PostMapping("/usuarios/activar/{id}")
    public String activarUsuario(
            @PathVariable Integer id,
            RedirectAttributes redirectAttrs) {

        usuarioRepository.findById(id).ifPresentOrElse(u -> {
            u.setActivo(true);
            usuarioRepository.save(u);
            redirectAttrs.addFlashAttribute("mensaje", "Usuario activado correctamente.");
        }, () -> {
            redirectAttrs.addFlashAttribute("error", "Usuario no encontrado.");
        });

        return "redirect:/admin/usuarios";
    }

    // -------------------------------------------------------
    // POST /admin/areas/estado/{id}
    // Activa o desactiva un área sin borrar su historial.
    // -------------------------------------------------------
    @PostMapping("/areas/estado/{id}")
    @Transactional
    public String cambiarEstadoArea(
            @PathVariable Integer id,
            RedirectAttributes redirectAttrs) {

        areaRepository.findById(id).ifPresentOrElse(a -> {
            a.setActivo(!a.getActivo());
            areaRepository.save(a);
            redirectAttrs.addFlashAttribute("mensaje",
                    "Área " + (a.getActivo() ? "activada" : "desactivada") + " correctamente.");
        }, () -> {
            redirectAttrs.addFlashAttribute("error", "Área no encontrada.");
        });

        return "redirect:/admin/servicios";
    }

    // -------------------------------------------------------
    // GET /admin/servicios
    // Lista todos los servicios (activos e inactivos).
    // -------------------------------------------------------
    @GetMapping("/servicios")
    public String servicios(Model model) {
        model.addAttribute("servicios", servicioRepository.findAllByOrderByNombreAsc());
        model.addAttribute("areas",     areaRepository.findAllByOrderByNombreAsc());
        return "admin/servicios";
    }

    // -------------------------------------------------------
    // POST /admin/servicios/estado/{id}
    // Activa o desactiva un servicio sin borrar su historial.
    // -------------------------------------------------------
    @PostMapping("/servicios/estado/{id}")
    @Transactional
    public String cambiarEstadoServicio(
            @PathVariable Integer id,
            RedirectAttributes redirectAttrs) {

        servicioRepository.findById(id).ifPresentOrElse(s -> {
            s.setActivo(!s.getActivo());
            servicioRepository.save(s);
            redirectAttrs.addFlashAttribute("mensaje",
                    "Servicio " + (s.getActivo() ? "activado" : "desactivado") + " correctamente.");
        }, () -> {
            redirectAttrs.addFlashAttribute("error", "Servicio no encontrado.");
        });

        return "redirect:/admin/servicios";
    }

    // -------------------------------------------------------
    // GET /admin/estudiantes - gestión de estudiantes (corrección admin, cédula bloqueada)
    // -------------------------------------------------------
    @GetMapping("/estudiantes")
    public String estudiantes(Model model) {
        model.addAttribute("estudiantes", estudianteRepository.findAllByOrderByApellidosAsc());
        model.addAttribute("facultades", facultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("sexos", sexoRepository.findAllByOrderByNombreAsc());
        model.addAttribute("etnias", etniaRepository.findAllByOrderByNombreAsc());
        return "admin/estudiantes";
    }

    @PostMapping("/estudiantes/editar/{id}")
    public String editarEstudiante(@PathVariable Integer id,
                                   @RequestParam String nombres,
                                   @RequestParam String apellidos,
                                   @RequestParam String correo,
                                   @RequestParam Integer sexoId,
                                   @RequestParam Integer etniaId,
                                   @RequestParam Integer carreraId,
                                   RedirectAttributes redirectAttrs) {
        Estudiante e = estudianteRepository.findById(id).orElse(null);
        if (e == null) {
            redirectAttrs.addFlashAttribute("error", "Estudiante no encontrado.");
            return "redirect:/admin/estudiantes";
        }
        try {
            estudianteService.actualizarDatosAdmin(e, nombres, apellidos, correo, sexoId, etniaId, carreraId);
            redirectAttrs.addFlashAttribute("mensaje", "Estudiante actualizado correctamente.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/estudiantes";
    }

    @GetMapping("/reportes/datos")
    @ResponseBody
    public Object datosReportes(@RequestParam(required = false) String desde,
                                @RequestParam(required = false) String hasta,
                                @RequestParam(required = false) Integer areaId,
                                @RequestParam(required = false) Integer servicioId,
                                @RequestParam(required = false) String estado,
                                @RequestParam(required = false) String busqueda,
                                @RequestParam(required = false) String categoria) {
        FiltroReporte filtro = new FiltroReporte(areaId, servicioId, FiltrosUtil.parsearEstado(estado), FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);
        String cat = categoria != null ? categoria : "estado";
        return reporteService.distribucion(filtro, cat);
    }

    @PostMapping("/reportes/pdf-con-graficas")
    public void exportarPdfConGraficas(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) Integer servicioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) List<String> columnas,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            @RequestParam(required = false) List<String> graficas,
            @RequestParam(required = false) List<String> graficasCategorias,
            HttpServletResponse response) throws IOException {
        FiltroReporte filtro = new FiltroReporte(areaId, servicioId, FiltrosUtil.parsearEstado(estado), FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);
        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";
        Usuario admin = obtenerAdmin(userDetails);
        reporteExportador.exportarPdf(response, "Reporte de asistencias", admin.getNombres() + " " + admin.getApellidos(), reporteService.historialCompleto(filtro, ordenNorm, dirNorm), true, columnas, graficas, graficasCategorias);
    }

    private Usuario obtenerAdmin(UserDetails userDetails) {
        return usuarioRepository.findByUsuario(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Administrador no encontrado en base de datos."));
    }

    private String generarContrasenaTemporal() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(ABECEDARIO_SEGURO.charAt(
                    random.nextInt(ABECEDARIO_SEGURO.length())));
        }
        return sb.toString();
    }
}
