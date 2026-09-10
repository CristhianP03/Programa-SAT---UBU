package com.bienestar.sistema_bienestar_universitario.service;

import com.bienestar.sistema_bienestar_universitario.model.Carrera;
import com.bienestar.sistema_bienestar_universitario.model.Estudiante;
import com.bienestar.sistema_bienestar_universitario.model.Etnia;
import com.bienestar.sistema_bienestar_universitario.model.Sexo;
import com.bienestar.sistema_bienestar_universitario.repository.CarreraRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EstudianteRepository;
import com.bienestar.sistema_bienestar_universitario.repository.EtniaRepository;
import com.bienestar.sistema_bienestar_universitario.repository.SexoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final CarreraRepository    carreraRepository;
    private final SexoRepository       sexoRepository;
    private final EtniaRepository      etniaRepository;

    @Transactional
    public Estudiante buscarOCrear(String cedula, String nombres, String apellidos,
                                    String correoInstitucional, Integer sexoId,
                                    Integer etniaId, Integer carreraId) {

        validarDatosBasicos(cedula, nombres, apellidos, correoInstitucional);

        if (sexoId == null || etniaId == null || carreraId == null) {
            throw new IllegalArgumentException(
                "Debe seleccionar sexo, etnia y carrera.");
        }

        Optional<Estudiante> existente = estudianteRepository.findByCedula(cedula);
        if (existente.isPresent()) {
            return existente.get();
        }

        Carrera carrera = carreraRepository.findById(carreraId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La carrera seleccionada no existe."));
        Sexo sexo = sexoRepository.findById(sexoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El sexo seleccionado no es válido."));
        Etnia etnia = etniaRepository.findById(etniaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La etnia seleccionada no es válida."));

        Estudiante estudiante = new Estudiante();
        estudiante.setCedula(cedula);
        estudiante.setNombres(nombres.trim());
        estudiante.setApellidos(apellidos.trim());
        estudiante.setCorreoInstitucional(correoInstitucional.trim().toLowerCase());
        estudiante.setCarrera(carrera);
        estudiante.setSexo(sexo);
        estudiante.setEtnia(etnia);

        return estudianteRepository.save(estudiante);
    }

    @Transactional
    public Estudiante actualizarDatos(Estudiante estudiante, String cedula,
                                       String nombres, String apellidos,
                                       String correoInstitucional, Integer sexoId,
                                       Integer etniaId, Integer carreraId) {

        validarDatosBasicos(cedula, nombres, apellidos, correoInstitucional);

        if (sexoId == null || etniaId == null || carreraId == null) {
            throw new IllegalArgumentException(
                "Debe seleccionar sexo, etnia y carrera.");
        }

        String cedulaNueva = cedula.trim();
        if (!cedulaNueva.equals(estudiante.getCedula())
                && estudianteRepository.existsByCedula(cedulaNueva)) {
            throw new IllegalArgumentException(
                "La cédula ingresada ya pertenece a otro estudiante.");
        }

        String correoNuevo = correoInstitucional.trim().toLowerCase();
        if (!correoNuevo.equals(estudiante.getCorreoInstitucional())
                && estudianteRepository.existsByCorreoInstitucional(correoNuevo)) {
            throw new IllegalArgumentException(
                "El correo ingresado ya pertenece a otro estudiante.");
        }

        Carrera carrera = carreraRepository.findById(carreraId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La carrera seleccionada no existe."));
        Sexo sexo = sexoRepository.findById(sexoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El sexo seleccionado no es válido."));
        Etnia etnia = etniaRepository.findById(etniaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La etnia seleccionada no es válida."));

        estudiante.setCedula(cedulaNueva);
        estudiante.setNombres(nombres.trim());
        estudiante.setApellidos(apellidos.trim());
        estudiante.setCorreoInstitucional(correoNuevo);
        estudiante.setCarrera(carrera);
        estudiante.setSexo(sexo);
        estudiante.setEtnia(etnia);

        return estudianteRepository.save(estudiante);
    }

    private void validarDatosBasicos(String cedula, String nombres, String apellidos,
                                     String correoInstitucional) {
        if (cedula == null || !validarCedulaEcuatoriana(cedula)) {
            throw new IllegalArgumentException(
                "La cédula no es válida. Debe contener 10 dígitos y pasar la validación ecuatoriana (módulo 10).");
        }

        if (correoInstitucional == null
                || !correoInstitucional.matches("^[\\w.-]+@uteq\\.edu\\.ec$")) {
            throw new IllegalArgumentException(
                "El correo debe ser institucional (@uteq.edu.ec).");
        }

        if (!hasText(nombres) || !hasText(apellidos)) {
            throw new IllegalArgumentException(
                "Los nombres y apellidos no pueden estar vacíos.");
        }
    }

    @Transactional(readOnly = true)
    public boolean existePorCedula(String cedula) {
        return estudianteRepository.existsByCedula(cedula);
    }

    @Transactional(readOnly = true)
    public Optional<Estudiante> buscarPorCedula(String cedula) {
        return estudianteRepository.findByCedula(cedula);
    }

    // Corrección por admin/encargado: cédula bloqueada, solo corrige resto
    @Transactional
    public Estudiante actualizarDatosAdmin(Estudiante estudiante, String nombres, String apellidos,
                                           String correoInstitucional, Integer sexoId,
                                           Integer etniaId, Integer carreraId) {
        if (!hasText(nombres) || !hasText(apellidos)) {
            throw new IllegalArgumentException("Los nombres y apellidos no pueden estar vacíos.");
        }
        if (correoInstitucional == null || !correoInstitucional.matches("^[\\w.-]+@uteq\\.edu\\.ec$")) {
            throw new IllegalArgumentException("El correo debe ser institucional (@uteq.edu.ec).");
        }
        if (sexoId == null || etniaId == null || carreraId == null) {
            throw new IllegalArgumentException("Debe seleccionar sexo, etnia y carrera.");
        }
        String correoNuevo = correoInstitucional.trim().toLowerCase();
        if (!correoNuevo.equals(estudiante.getCorreoInstitucional())
                && estudianteRepository.existsByCorreoInstitucional(correoNuevo)) {
            throw new IllegalArgumentException("El correo ingresado ya pertenece a otro estudiante.");
        }
        Carrera carrera = carreraRepository.findById(carreraId)
                .orElseThrow(() -> new IllegalArgumentException("La carrera seleccionada no existe."));
        Sexo sexo = sexoRepository.findById(sexoId)
                .orElseThrow(() -> new IllegalArgumentException("El sexo seleccionado no es válido."));
        Etnia etnia = etniaRepository.findById(etniaId)
                .orElseThrow(() -> new IllegalArgumentException("La etnia seleccionada no es válida."));
        estudiante.setNombres(nombres.trim());
        estudiante.setApellidos(apellidos.trim());
        estudiante.setCorreoInstitucional(correoNuevo);
        estudiante.setCarrera(carrera);
        estudiante.setSexo(sexo);
        estudiante.setEtnia(etnia);
        return estudianteRepository.save(estudiante);
    }

    public static boolean validarCedulaEcuatoriana(String cedula) {
        if (cedula == null || cedula.length() != 10 || !cedula.matches("\\d{10}")) {
            return false;
        }

        int provincia = Integer.parseInt(cedula.substring(0, 2));
        if (provincia < 0 || provincia > 24) {
            return false;
        }

        int[] coeficientes = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int suma = 0;

        for (int i = 0; i < 9; i++) {
            int digito = cedula.charAt(i) - '0';
            int producto = digito * coeficientes[i];
            if (producto >= 10) {
                producto -= 9;
            }
            suma += producto;
        }

        int digitoVerificador = cedula.charAt(9) - '0';
        int resultado = (10 - (suma % 10)) % 10;

        return digitoVerificador == resultado;
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
