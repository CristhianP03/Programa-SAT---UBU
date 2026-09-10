package com.bienestar.sistema_bienestar_universitario.controller;

import com.bienestar.sistema_bienestar_universitario.dto.FiltroReporte;
import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import com.bienestar.sistema_bienestar_universitario.model.Asistencia.Estado;
import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.bienestar.sistema_bienestar_universitario.model.Estudiante;
import com.bienestar.sistema_bienestar_universitario.repository.CarreraRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EstudianteRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EtniaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.FacultadRepository;
import com.bienestar.sistema_bienestar_universitario.repository.ServicioRepository;
import com.bienestar.sistema_bienestar_universitario.repository.SexoRepository;
import com.bienestar.sistema_bienestar_universitario.repository.UsuarioRepository;
import com.bienestar.sistema_bienestar_universitario.service.AsistenciaService;
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
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/encargado")
@RequiredArgsConstructor
public class EncargadoController {

    private static final int TAMANO_PAGINA = 10;

    private final AsistenciaService     asistenciaService;
    private final ReporteService        reporteService;
    private final ReporteExportador     reporteExportador;
    private final ServicioRepository    servicioRepository;
    private final UsuarioRepository     usuarioRepository;
    private final UsuarioService        usuarioService;
    private final EstudianteRepository  estudianteRepository;
    private final EstudianteService     estudianteService;
    private final FacultadRepository    facultadRepository;
    private final CarreraRepository     carreraRepository;
    private final SexoRepository        sexoRepository;
    private final EtniaRepository       etniaRepository;

    // -------------------------------------------------------
    // GET /encargado/panel
    // Panel principal del encargado.
    // Muestra la cola actual de su área y quién puede atenderse.
    // -------------------------------------------------------
    @GetMapping("/panel")
    public String panel(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Usuario encargado = obtenerEncargado(userDetails);
        List<Asistencia> cola = asistenciaService
                .obtenerActivosDelDiaPorArea(encargado.getArea().getId());

        boolean tieneAtencionEnCurso = asistenciaService
                .tieneAtencionEnCurso(encargado.getId());

        Set<Integer> siguientes = new HashSet<>();
        int enCola = 0, enAtencion = 0, noPresentados = 0;
        for (Asistencia a : cola) {
            switch (a.getEstado()) {
                case ESPERANDO -> {
                    enCola++;
                    if (asistenciaService.esSiguienteEnCola(
                            encargado.getArea().getId(), a.getNumeroTurno())) {
                        siguientes.add(a.getId());
                    }
                }
                case EN_ATENCION   -> enAtencion++;
                case NO_SE_PRESENTO -> noPresentados++;
                default -> { }
            }
        }

        model.addAttribute("encargado",            encargado);
        model.addAttribute("cola",                 cola);
        model.addAttribute("tieneAtencionEnCurso", tieneAtencionEnCurso);
        model.addAttribute("siguientes",           siguientes);
        model.addAttribute("enCola",               enCola);
        model.addAttribute("enAtencion",           enAtencion);
        model.addAttribute("noPresentados",        noPresentados);

        return "encargado/panel";
    }

    // -------------------------------------------------------
    // POST /encargado/atender/{id}
    // El encargado llama al siguiente estudiante.
    // -------------------------------------------------------
    @PostMapping("/atender/{id}")
    public String iniciarAtencion(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttrs) {

        try {
            Usuario encargado = obtenerEncargado(userDetails);
            asistenciaService.iniciarAtencion(id, encargado.getId());
            redirectAttrs.addFlashAttribute("mensaje", "Atención iniciada.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/encargado/panel";
    }

    // -------------------------------------------------------
    // POST /encargado/finalizar/{id}
    // El encargado cierra la atención como ATENDIDO.
    // -------------------------------------------------------
    @PostMapping("/finalizar/{id}")
    public String finalizarAtencion(
            @PathVariable Integer id,
            @RequestParam(required = false) String observaciones,
            RedirectAttributes redirectAttrs) {

        if (observaciones != null && observaciones.length() > 500) {
            observaciones = observaciones.substring(0, 500);
        }

        try {
            asistenciaService.finalizarAtencion(id, observaciones);
            redirectAttrs.addFlashAttribute("mensaje", "Atención finalizada correctamente.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/encargado/panel";
    }

    // -------------------------------------------------------
    // POST /encargado/no-presento/{id}
    // El encargado marca que el estudiante no se presentó.
    // -------------------------------------------------------
    @PostMapping("/no-presento/{id}")
    public String marcarNoPresento(
            @PathVariable Integer id,
            RedirectAttributes redirectAttrs) {

        try {
            asistenciaService.marcarNoSePresento(id);
            redirectAttrs.addFlashAttribute("mensaje",
                    "Marcado como no presentado. Tienes 2 minutos para revertir.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/encargado/panel";
    }

    // -------------------------------------------------------
    // POST /encargado/revertir/{id}
    // El encargado revierte NO_SE_PRESENTO dentro de 2 minutos.
    // -------------------------------------------------------
    @PostMapping("/revertir/{id}")
    public String revertirNoPresento(
            @PathVariable Integer id,
            RedirectAttributes redirectAttrs) {

        try {
            asistenciaService.revertirNoSePresento(id);
            redirectAttrs.addFlashAttribute("mensaje",
                    "Revertido correctamente. El estudiante vuelve a la cola.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/encargado/panel";
    }

    // -------------------------------------------------------
    // GET /encargado/reporte
    // Histórico de asistencias del área del encargado con filtros
    // (servicio, estado, rango de fechas, búsqueda por texto),
    // ordenamiento por columnas y paginación.
    // -------------------------------------------------------
    @GetMapping("/reporte")
    public String reporte(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer servicioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            @RequestParam(required = false) String page,
            Model model) {

        Usuario encargado = obtenerEncargado(userDetails);
        Integer areaId = encargado.getArea().getId();

        FiltroReporte filtro = new FiltroReporte(
                areaId, servicioId, FiltrosUtil.parsearEstado(estado),
                FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);

        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm   = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";

        Page<Asistencia> pagina = reporteService.historial(
                filtro, FiltrosUtil.parsearPagina(page), TAMANO_PAGINA, ordenNorm, dirNorm);

        model.addAttribute("encargado",      encargado);
        model.addAttribute("desde",          desde);
        model.addAttribute("hasta",          hasta);
        model.addAttribute("servicioId",     servicioId);
        model.addAttribute("estado",         estado);
        model.addAttribute("busqueda",       busqueda);
        model.addAttribute("orden",          ordenNorm);
        model.addAttribute("dir",            dirNorm);
        model.addAttribute("servicios",      servicioRepository.findByAreaIdOrderByNombreAsc(areaId));
        model.addAttribute("estados",        Asistencia.Estado.values());
        model.addAttribute("pagina",         pagina);
        model.addAttribute("asistencias",    pagina.getContent());
        model.addAttribute("paginas",        FiltrosUtil.rangoPaginas(pagina));
        model.addAttribute("resumenEstados", reporteService.resumenEstados(filtro));
        model.addAttribute("total",          pagina.getTotalElements());

        return "encargado/reporte";
    }

    // -------------------------------------------------------
    // GET /encargado/reporte/excel | /encargado/reporte/pdf
    // Exportación del histórico filtrado del área del encargado.
    // Las columnas se eligen con los checkboxes de la vista.
    // -------------------------------------------------------
    @GetMapping("/reporte/excel")
    public void exportarExcel(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer servicioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) List<String> columnas,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            HttpServletResponse response) throws IOException {

        Usuario encargado = obtenerEncargado(userDetails);
        FiltroReporte filtro = new FiltroReporte(
                encargado.getArea().getId(), servicioId, FiltrosUtil.parsearEstado(estado),
                FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);

        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm   = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";

        reporteExportador.exportarExcel(response, "Reporte del área",
                reporteService.historialCompleto(filtro, ordenNorm, dirNorm),
                false, columnas);
    }

    @GetMapping("/reporte/pdf")
    public void exportarPdf(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false) Integer servicioId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) List<String> columnas,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String dir,
            HttpServletResponse response) throws IOException {

        Usuario encargado = obtenerEncargado(userDetails);
        FiltroReporte filtro = new FiltroReporte(
                encargado.getArea().getId(), servicioId, FiltrosUtil.parsearEstado(estado),
                FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);

        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm   = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";

        reporteExportador.exportarPdf(response, "Reporte del área",
                encargado.getNombres() + " " + encargado.getApellidos(),
                reporteService.historialCompleto(filtro, ordenNorm, dirNorm),
                false, columnas);
    }

    // -------------------------------------------------------
    // GET /encargado/perfil
    // El encargado ve y edita sus datos personales.
    // -------------------------------------------------------
    @GetMapping("/perfil")
    public String perfil(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        Usuario encargado = obtenerEncargado(userDetails);
        model.addAttribute("personal", encargado);
        model.addAttribute("esAdmin", false);
        return "personal/perfil";
    }

    // -------------------------------------------------------
    // POST /encargado/perfil
    // Guarda usuario, nombres, apellidos y correo; opcionalmente
    // cambia la contraseña verificando la actual.
    // Si cambia el usuario o el correo, fuerza volver a iniciar sesión.
    // -------------------------------------------------------
    @PostMapping("/perfil")
    @Transactional
    public String guardarPerfil(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String  nombres,
            @RequestParam String  apellidos,
            @RequestParam String  correo,
            @RequestParam String  usuario,
            @RequestParam(required = false) String contrasenaActual,
            @RequestParam(required = false) String nuevaContrasena,
            HttpSession session,
            RedirectAttributes redirectAttrs) {

        Usuario encargado = obtenerEncargado(userDetails);
        String usuarioAnterior = encargado.getUsuario();
        String correoAnterior = encargado.getCorreo();

        try {
            encargado = usuarioService.actualizarPerfil(
                    encargado, nombres, apellidos, correo, usuario,
                    contrasenaActual, nuevaContrasena);
        } catch (IllegalArgumentException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/encargado/perfil";
        }

        boolean credencialesCambiadas = !encargado.getUsuario().equals(usuarioAnterior)
                || !encargado.getCorreo().equals(correoAnterior);

        if (credencialesCambiadas) {
            SecurityContextHolder.clearContext();
            if (session != null) {
                session.invalidate();
            }
            return "redirect:/auth/login?perfil=1";
        }

        redirectAttrs.addFlashAttribute("mensaje", "Perfil actualizado correctamente.");
        return "redirect:/encargado/perfil";
    }

    @GetMapping("/estudiantes")
    public String estudiantes(Model model) {
        model.addAttribute("estudiantes", estudianteRepository.findAllByOrderByApellidosAsc());
        model.addAttribute("facultades", facultadRepository.findAllByOrderByNombreAsc());
        model.addAttribute("sexos", sexoRepository.findAllByOrderByNombreAsc());
        model.addAttribute("etnias", etniaRepository.findAllByOrderByNombreAsc());
        return "encargado/estudiantes";
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
            return "redirect:/encargado/estudiantes";
        }
        try {
            estudianteService.actualizarDatosAdmin(e, nombres, apellidos, correo, sexoId, etniaId, carreraId);
            redirectAttrs.addFlashAttribute("mensaje", "Estudiante actualizado correctamente.");
        } catch (Exception ex) {
            redirectAttrs.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/encargado/estudiantes";
    }

    @GetMapping("/reporte/datos")
    @ResponseBody
    public Object datosReporteEncargado(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestParam(required = false) String desde,
                                        @RequestParam(required = false) String hasta,
                                        @RequestParam(required = false) Integer servicioId,
                                        @RequestParam(required = false) String estado,
                                        @RequestParam(required = false) String busqueda,
                                        @RequestParam(required = false) String categoria) {
        Usuario encargado = obtenerEncargado(userDetails);
        FiltroReporte filtro = new FiltroReporte(encargado.getArea().getId(), servicioId, FiltrosUtil.parsearEstado(estado), FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);
        String cat = categoria != null ? categoria : "estado";
        return reporteService.distribucion(filtro, cat);
    }

    @PostMapping("/reporte/pdf-con-graficas")
    public void exportarPdfConGraficasEncargado(@AuthenticationPrincipal UserDetails userDetails,
                                                @RequestParam(required = false) String desde,
                                                @RequestParam(required = false) String hasta,
                                                @RequestParam(required = false) Integer servicioId,
                                                @RequestParam(required = false) String estado,
                                                @RequestParam(required = false) String busqueda,
                                                @RequestParam(required = false) List<String> columnas,
                                                @RequestParam(required = false) String orden,
                                                @RequestParam(required = false) String dir,
                                                @RequestParam(required = false) List<String> graficas,
                                                @RequestParam(required = false) List<String> graficasCategorias,
                                                HttpServletResponse response) throws IOException {
        Usuario encargado = obtenerEncargado(userDetails);
        FiltroReporte filtro = new FiltroReporte(encargado.getArea().getId(), servicioId, FiltrosUtil.parsearEstado(estado), FechaUtil.parsearFecha(desde), FechaUtil.parsearFecha(hasta), busqueda);
        String ordenNorm = StringUtils.hasText(orden) ? orden : "fechaHoraRegistro";
        String dirNorm = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";
        reporteExportador.exportarPdf(response, "Reporte del área", encargado.getNombres()+" "+encargado.getApellidos(), reporteService.historialCompleto(filtro, ordenNorm, dirNorm), false, columnas, graficas, graficasCategorias);
    }

    // -------------------------------------------------------
    // Método interno — obtiene el Usuario completo desde
    // el UserDetails de Spring Security (login por "usuario").
    // -------------------------------------------------------
    private Usuario obtenerEncargado(UserDetails userDetails) {
        return usuarioRepository.findByUsuario(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException(
                        "Encargado no encontrado en base de datos."));
    }
}
