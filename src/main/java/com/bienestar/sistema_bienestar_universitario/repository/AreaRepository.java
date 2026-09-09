package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {

    List<Area> findAllByOrderByNombreAsc();

    List<Area> findByActivoTrueOrderByNombreAsc();

    Optional<Area> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
