package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import com.bienestar.sistema_bienestar_universitario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder   passwordEncoder;

    // Actualiza los datos del perfil (usuario, correo, nombre) y,
    // opcionalmente, la contraseña. La contraseña solo cambia si el
    // usuario escribe una nueva (no se fuerza el cambio en el primer login).
    @Transactional
    public Usuario actualizarPerfil(Usuario usuario,
                                    String nombres,
                                    String apellidos,
                                    String correo,
                                    String usuarioLogin,
                                    String contrasenaActual,
                                    String nuevaContrasena) {

        if (nombres == null || nombres.isBlank()
                || apellidos == null || apellidos.isBlank()) {
            throw new IllegalArgumentException(
                    "Los nombres y apellidos son obligatorios.");
        }

        String usuarioNormalizado = normalizar(usuarioLogin);
        String correoNormalizado = normalizar(correo);

        if (usuarioNormalizado == null || usuarioNormalizado.isBlank()
                || correoNormalizado == null || correoNormalizado.isBlank()) {
            throw new IllegalArgumentException(
                    "El usuario y el correo son obligatorios.");
        }

        if (!usuarioNormalizado.equals(usuario.getUsuario())
                && usuarioRepository.existsByUsuario(usuarioNormalizado)) {
            throw new IllegalArgumentException(
                    "Ese usuario ya está en uso.");
        }

        if (!correoNormalizado.equals(usuario.getCorreo())
                && usuarioRepository.existsByCorreo(correoNormalizado)) {
            throw new IllegalArgumentException(
                    "Ese correo ya está registrado.");
        }

        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            if (contrasenaActual == null
                    || !passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) {
                throw new IllegalArgumentException(
                        "La contraseña actual no es correcta.");
            }
            if (nuevaContrasena.length() < 6) {
                throw new IllegalArgumentException(
                        "La nueva contraseña debe tener al menos 6 caracteres.");
            }
            usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        }

        usuario.setNombres(nombres.trim());
        usuario.setApellidos(apellidos.trim());
        usuario.setCorreo(correoNormalizado);
        usuario.setUsuario(usuarioNormalizado);

        return usuarioRepository.save(usuario);
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.trim().toLowerCase();
    }
}
