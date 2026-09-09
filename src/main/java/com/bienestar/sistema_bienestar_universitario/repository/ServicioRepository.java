package com.bienestar.sistema_bienestar_universitario.repository;

import com.bienestar.sistema_bienestar_universitario.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Integer> {

    // Solo listar servicios activos de un área — los suspendidos no aparecen al estudiante
    List<Servicio> findByAreaIdAndActivoTrueOrderByNombreAsc(Integer areaId);

    // Listar todos los servicios de un área incluyendo inactivos — solo para el panel ADMIN
    List<Servicio> findByAreaIdOrderByNombreAsc(Integer areaId);

    // Listar todos los servicios activos (todas las áreas) — para selección del estudiante
    List<Servicio> findByActivoTrueOrderByNombreAsc();

    // Listar todos incluyendo inactivos — para el panel ADMIN
    List<Servicio> findAllByOrderByNombreAsc();
}
