package com.bienestar.sistema_bienestar_universitario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"usuario"})
@Entity
@Table(name = "log_accesos")
public class LogAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    // Muchos logs pertenecen a un usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    // Soporta IPv4 e IPv6 (máximo 45 caracteres)
    @Column(name = "ip_acceso", length = 45)
    private String ipAcceso;

    // Indica si ya se envió el correo de notificación de acceso
    @Column(name = "notificacion_enviada", nullable = false)
    private Boolean notificacionEnviada = false;

    @PrePersist
    public void prePersist() {
        this.fechaHora = LocalDateTime.now();
        if (this.notificacionEnviada == null) this.notificacionEnviada = false;
    }
}
