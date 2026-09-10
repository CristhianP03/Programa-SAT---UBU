package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Buscar por correo — dato del perfil
    Optional<Usuario> findByCorreo(String correo);

    // Verificar si ya existe un usuario con ese correo
    boolean existsByCorreo(String correo);

    // Buscar por usuario — usado por Spring Security en el login
    Optional<Usuario> findByUsuario(String usuario);

    // Verificar si ya existe un usuario con ese usuario
    boolean existsByUsuario(String usuario);

    // Listar encargados activos de un área específico
    List<Usuario> findByAreaIdAndActivoTrue(Integer areaId);

    // Listar encargados activos de un área — para notificar nuevo turno
    List<Usuario> findByAreaIdAndRolAndActivoTrue(Integer areaId, Usuario.Rol rol);

    // Listar todos los usuarios activos — para panel ADMIN
    List<Usuario> findByActivoTrueOrderByApellidosAsc();

    // Listar todos los usuarios (activos e inactivos) — para reactivar
    List<Usuario> findAllByOrderByApellidosAsc();
}
