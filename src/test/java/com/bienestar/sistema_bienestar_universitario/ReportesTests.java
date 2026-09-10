package com.bienestar.sistema_bienestar_universitario;

import com.bienestar.sistema_bienestar_universitario.dto.FiltroReporte;
import com.bienestar.sistema_bienestar_universitario.model.Area;
import com.bienestar.sistema_bienestar_universitario.model.Asistencia;
import com.bienestar.sistema_bienestar_universitario.model.Estudiante;
import com.bienestar.sistema_bienestar_universitario.model.Servicio;
import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.bienestar.sistema_bienestar_universitario.repository.AreaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.AsistenciaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.CarreraRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EstudianteRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EtniaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.ServicioRepository;
import com.bienestar.sistema_bienestar_universitario.repository.SexoRepository;
import com.bienestar.sistema_bienestar_universitario.repository.UsuarioRepository;
import com.bienestar.sistema_bienestar_universitario.service.ReporteService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportesTests {

    @Autowired private MockMvc            mvc;
    @Autowired private UsuarioRepository  usuarioRepository;
    @Autowired private AreaRepository     areaRepository;
    @Autowired private PasswordEncoder    passwordEncoder;
    @Autowired private AsistenciaRepository asistenciaRepository;
    @Autowired private EstudianteRepository estudianteRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private CarreraRepository  carreraRepository;
    @Autowired private SexoRepository     sexoRepository;
    @Autowired private EtniaRepository    etniaRepository;
    @Autowired private ReporteService     reporteService;

    private Usuario admin;
    private Usuario encargado;

    private final List<Asistencia> asistenciasCreadas = new ArrayList<>();
    private final List<Estudiante> estudiantesCreados = new ArrayList<>();

    @BeforeEach
    void crearUsuarios() {
        admin = usuarioRepository.save(usuario(Usuario.Rol.ADMIN, null));
        Area area = areaRepository.findAll().stream().findFirst().orElse(null);
        encargado = usuarioRepository.save(usuario(Usuario.Rol.ENCARGADO, area));
    }

    @AfterEach
    void limpiar() {
        for (Asistencia a : asistenciasCreadas) {
            asistenciaRepository.deleteById(a.getId());
        }
        asistenciasCreadas.clear();
        for (Estudiante e : estudiantesCreados) {
            if (e.getId() != null) {
                estudianteRepository.deleteById(e.getId());
            }
        }
        estudiantesCreados.clear();
        usuarioRepository.deleteById(admin.getId());
        if (encargado != null && encargado.getId() != null) {
            usuarioRepository.deleteById(encargado.getId());
        }
    }

    @Test
    void reporteAdminNoCaeConFiltrosVacios() throws Exception {
        mvc.perform(get("/admin/reportes").with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk());

        mvc.perform(get("/admin/reportes").queryParam("desde", "").queryParam("hasta", "")
                        .with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk());

        mvc.perform(get("/admin/reportes").queryParam("desde", "07/08/2026")
                        .with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk());

        mvc.perform(get("/admin/reportes").queryParam("desde", "2026-08-07")
                        .queryParam("hasta", "2026-08-07")
                        .with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void reporteAdminNoCaeConFiltrosInvalidos() throws Exception {
        mvc.perform(get("/admin/reportes").queryParam("estado", "INEXISTENTE")
                        .queryParam("page", "abc")
                        .queryParam("areaId", "99999")
                        .with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void reporteEncargadoNoCaeConFechasVaciasOInvalidas() throws Exception {
        mvc.perform(get("/encargado/reporte").with(user(encargado.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk());

        mvc.perform(get("/encargado/reporte").queryParam("desde", "").queryParam("hasta", "")
                        .with(user(encargado.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk());

        mvc.perform(get("/encargado/reporte").queryParam("desde", "07/08/2026")
                        .with(user(encargado.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk());

        mvc.perform(get("/encargado/reporte").queryParam("estado", "ATENDIDO")
                        .queryParam("page", "-5")
                        .with(user(encargado.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk());
    }

    @Test
    void exportacionAdminGeneraExcelYpdf() throws Exception {
        mvc.perform(get("/admin/reportes/excel").with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        mvc.perform(get("/admin/reportes/pdf").with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void exportacionEncargadoGeneraExcelYpdf() throws Exception {
        mvc.perform(get("/encargado/reporte/excel")
                        .with(user(encargado.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

        mvc.perform(get("/encargado/reporte/pdf")
                        .with(user(encargado.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void dashboardNoCaeConAsistenciasDeHoy() throws Exception {
        crearAsistencia(Asistencia.Estado.ATENDIDO);
        crearAsistencia(Asistencia.Estado.ESPERANDO);

        mvc.perform(get("/admin/dashboard").with(user(admin.getUsuario()).roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void reporteSinFechasIncluyeTodoElHistorial() {
        Asistencia vieja = crearAsistenciaConFecha(Asistencia.Estado.ATENDIDO,
                LocalDateTime.of(2020, 1, 15, 9, 30));

        FiltroReporte filtro = new FiltroReporte(null, null, null, null, null, null);
        List<Asistencia> historial = reporteService.historial(filtro, 0, 100,
                "fechaHoraRegistro", "desc").getContent();

        assertTrue(historial.stream().anyMatch(a -> a.getId().equals(vieja.getId())),
                "Sin fechas el reporte debe incluir los registros antiguos");
    }

    @Test
    void reporteExcluyeCanceladosDelHistorialYResumen() {
        Asistencia atendido = crearAsistencia(Asistencia.Estado.ATENDIDO);
        Asistencia cancelado = crearAsistencia(Asistencia.Estado.CANCELADO);

        FiltroReporte filtro = new FiltroReporte(null, null, null, null, null, null);

        List<Asistencia> historial = reporteService.historialCompleto(filtro,
                "fechaHoraRegistro", "desc");
        assertTrue(historial.stream().anyMatch(a -> a.getId().equals(atendido.getId())));
        assertFalse(historial.stream().anyMatch(a -> a.getId().equals(cancelado.getId())),
                "Los registros CANCELADO no deben aparecer en el historial");

        Map<String, Long> resumen = reporteService.resumenEstados(filtro);
        assertFalse(resumen.containsKey("CANCELADO"),
                "El resumen no debe contener el estado CANCELADO");
    }

    @Test
    void reporteEncargadoSinColumnaObservaciones() throws Exception {
        mvc.perform(get("/encargado/reporte").with(user(encargado.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Observaciones"))));
    }

    private Asistencia crearAsistencia(Asistencia.Estado estado) {
        return crearAsistenciaConFecha(estado, LocalDateTime.now());
    }

    private Asistencia crearAsistenciaConFecha(Asistencia.Estado estado, LocalDateTime registro) {
        Estudiante est = crearEstudiante();
        Servicio serv = servicioRepository.findAll().stream().findFirst().orElseThrow();
        Asistencia a = new Asistencia();
        a.setEstudiante(est);
        a.setServicio(serv);
        a.setNumeroTurno(90000 + asistenciasCreadas.size());
        a = asistenciaRepository.save(a);

        a.setFechaHoraRegistro(registro);
        a.setEstado(estado);
        switch (estado) {
            case EN_ATENCION -> a.setFechaHoraAtencion(registro.plusMinutes(2));
            case ATENDIDO -> {
                a.setFechaHoraAtencion(registro.plusMinutes(2));
                a.setFechaHoraCierre(registro.plusMinutes(5));
            }
            case NO_SE_PRESENTO, CANCELADO -> a.setFechaHoraCierre(registro.plusMinutes(5));
            default -> { }
        }
        a = asistenciaRepository.save(a);
        asistenciasCreadas.add(a);
        return a;
    }

    private Estudiante crearEstudiante() {
        String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Estudiante e = new Estudiante();
        e.setCedula("17" + sufijo.substring(0, 10));
        e.setNombres("Estudiante " + sufijo);
        e.setApellidos("Prueba " + sufijo);
        e.setCorreoInstitucional("est" + sufijo + "@uteq.edu.ec");
        e.setCarrera(carreraRepository.findAll().stream().findFirst().orElseThrow());
        e.setSexo(sexoRepository.findAll().stream().findFirst().orElseThrow());
        e.setEtnia(etniaRepository.findAll().stream().findFirst().orElseThrow());
        e = estudianteRepository.save(e);
        estudiantesCreados.add(e);
        return e;
    }

    private Usuario usuario(Usuario.Rol rol, Area area) {
        Usuario u = new Usuario();
        u.setNombres("Prueba");
        u.setApellidos(rol.name());
        u.setCorreo(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@uteq.edu.ec");
        u.setUsuario(rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 12));
        u.setContrasena(passwordEncoder.encode("123456"));
        u.setRol(rol);
        u.setArea(area);
        return u;
    }
}
