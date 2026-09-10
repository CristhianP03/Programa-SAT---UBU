package com.bienestar.sistema_bienestar_universitario;

import com.bienestar.sistema_bienestar_universitario.model.Area;
import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.bienestar.sistema_bienestar_universitario.repository.AreaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class EncargadoVistasTests {

    @Autowired private MockMvc            mvc;
    @Autowired private UsuarioRepository  usuarioRepository;
    @Autowired private AreaRepository     areaRepository;
    @Autowired private PasswordEncoder    passwordEncoder;

    private Usuario testUser;

    @AfterEach
    void limpiar() {
        if (testUser != null && testUser.getId() != null) {
            usuarioRepository.deleteById(testUser.getId());
        }
    }

    @Test
    void lasVistasDelEncargadoSeRenderizan() throws Exception {
        testUser = crearEncargadoDePrueba();

        mvc.perform(get("/encargado/panel")
                        .with(user(testUser.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(view().name("encargado/panel"));

        mvc.perform(get("/encargado/reporte")
                        .with(user(testUser.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(view().name("encargado/reporte"));

        mvc.perform(get("/encargado/perfil")
                        .with(user(testUser.getUsuario()).roles("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(view().name("personal/perfil"));
    }

    private Usuario crearEncargadoDePrueba() {
        Area area = areaRepository.findAll().stream().findFirst().orElse(null);

        Usuario u = new Usuario();
        u.setNombres("Prueba");
        u.setApellidos("Encargado");
        u.setCorreo("prueba-" + UUID.randomUUID() + "@uteq.edu.ec");
        u.setUsuario("encargado-" + UUID.randomUUID().toString().substring(0, 12));
        u.setContrasena(passwordEncoder.encode("123456"));
        u.setRol(Usuario.Rol.ENCARGADO);
        u.setArea(area);
        return usuarioRepository.save(u);
    }
}
