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
@ToString(exclude = {"logAccesos", "area", "contrasena"})
@Entity
@Table(name = "usuarios")
public class Usuario {

    // Roles posibles — sin RECEPCIONISTA porque el estudiante se registra solo
    public enum Rol {
        ADMIN,      // Acceso total, ve todos los reportes
        ENCARGADO   // Solo gestiona su propio servicio
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    // Nombre de usuario para el login, independiente del correo
    @Column(nullable = false, unique = true, length = 50)
    private String usuario;

    @Column(nullable = false, unique = true, length = 100)
    private String correo;

    // Siempre almacenar con BCrypt desde la aplicación, nunca texto plano
    @Column(nullable = false, length = 255)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    // Solo los ENCARGADO tienen área asignada, ADMIN puede ser null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<LogAcceso> logAccesos;

    @PrePersist
    public void prePersist() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
    }
}