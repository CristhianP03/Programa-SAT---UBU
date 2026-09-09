package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.Carrera;
import com.bienestar.sistema_bienestar_universitario.model.Facultad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarreraRepository extends JpaRepository<Carrera, Integer> {
    // Listar carreras de una facultad específica ordenadas alfabéticamente
    // Útil para el formulario de registro del estudiante (carga dinámica)
    List<Carrera> findByFacultadOrderByNombreAsc(Facultad facultad);
    // Listar carreras por ID de facultad directamente
    List<Carrera> findByFacultadIdOrderByNombreAsc(Integer facultadId);
    // Verificar si ya existe esa carrera en esa facultad
    boolean existsByNombreAndFacultad(String nombre, Facultad facultad);
}