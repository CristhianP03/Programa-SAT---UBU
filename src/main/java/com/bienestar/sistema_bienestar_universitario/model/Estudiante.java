package com.bienestar.sistema_bienestar_universitario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"asistencias", "carrera", "sexo", "etnia"})
@Entity
@Table(name = "estudiantes")
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    // Solo acepta correos @uteq.edu.ec — validado también en BD
    @Column(name = "correo_institucional", nullable = false, unique = true, length = 100)
    private String correoInstitucional;

    // Muchos estudiantes pertenecen a una carrera
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrera_id", nullable = false)
    private Carrera carrera;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sexo_id", nullable = false)
    private Sexo sexo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etnia_id", nullable = false)
    private Etnia etnia;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    // false = desactivado sin borrar historial
    @Column(nullable = false)
    private Boolean activo = true;

    // Un estudiante puede tener muchas asistencias
    @OneToMany(mappedBy = "estudiante", fetch = FetchType.LAZY)
    private List<Asistencia> asistencias;

    @PrePersist
    public void prePersist() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
    }
}
