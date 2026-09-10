package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.model.LogAcceso;
import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.bienestar.sistema_bienestar_universitario.repository.LogAccesoRepository;
import com.bienestar.sistema_bienestar_universitario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UsuarioRepository  usuarioRepository;
    private final LogAccesoRepository logAccesoRepository;
    private final CorreoService       correoService;

    // -------------------------------------------------------
    // Requerido por Spring Security para el login.
    // Spring llama este método automáticamente al autenticar.
    // El login es por "usuario" (no por correo), insensible a
    // mayúsculas: el usuario se almacena siempre en minúsculas.
    // -------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usuario)
            throws UsernameNotFoundException {

        if (usuario == null || usuario.isBlank()) {
            throw new UsernameNotFoundException("Debes ingresar tu usuario.");
        }

        Usuario usuarioEntity = usuarioRepository
                .findByUsuario(usuario.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con el usuario: " + usuario));

        if (!usuarioEntity.getActivo()) {
            throw new UsernameNotFoundException(
                    "La cuenta está desactivada.");
        }

        return User.builder()
                .username(usuarioEntity.getUsuario())
                .password(usuarioEntity.getContrasena())
                .roles(usuarioEntity.getRol().name())
                .build();
    }

    // -------------------------------------------------------
    // Registra el acceso en log_accesos y envía notificación
    // por correo al usuario. Se llama desde el controller
    // justo después de que Spring confirma el login exitoso.
    // -------------------------------------------------------
    @Transactional
    public void registrarAcceso(String usuario, String ip) {

        Usuario usuarioEntity = usuarioRepository
                .findByUsuario(usuario.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Usuario no encontrado."));

        LogAcceso log = new LogAcceso();
        log.setUsuario(usuarioEntity);
        log.setIpAcceso(ip);
        log.setNotificacionEnviada(false);
        logAccesoRepository.save(log);

        String fechaFormateada = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        correoService.enviarNotificacionAccesoAsync(
                usuarioEntity.getCorreo(),
                usuarioEntity.getNombres(),
                ip,
                fechaFormateada);
    }
}