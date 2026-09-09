package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FacultadRepository extends JpaRepository<Facultad, Integer> {

    // Buscar facultad por nombre exacto
    Optional<Facultad> findByNombre(String nombre);

    // Verificar si ya existe una facultad con ese nombre
    boolean existsByNombre(String nombre);

    // Listar todas ordenadas alfabéticamente
    List<Facultad> findAllByOrderByNombreAsc();
}
