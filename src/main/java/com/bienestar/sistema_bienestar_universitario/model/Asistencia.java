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
@ToString(exclude = {"estudiante", "servicio", "usuarioAsignado", "usuarioAtendio"})
@Entity
@Table(name = "asistencias")
public class Asistencia {

    public enum Estado {
        PENDIENTE_VERIFICACION, // Registrado, esperando confirmar correo
        ESPERANDO,              // Verificado, en cola
        EN_ATENCION,            // Siendo atendido ahora
        ATENDIDO,               // Proceso completado
        NO_SE_PRESENTO,         // Llamado pero ausente (reversible 2 minutos)
        CANCELADO               // Token expirado o cancelado
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    // Estudiante que solicita el servicio
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    // Servicio solicitado
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    // Encargado visible para el estudiante desde que se registra
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_asignado_id")
    private Usuario usuarioAsignado;

    // Encargado que realmente cerró el caso
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_atendio_id")
    private Usuario usuarioAtendio;

    @Column(name = "numero_turno", nullable = false)
    private Integer numeroTurno;

    // Token enviado al correo del estudiante para verificar identidad
    @Column(name = "token_verificacion", length = 64)
    private String tokenVerificacion;

    // El estudiante tiene 30 minutos para verificar (configurado en properties)
    @Column(name = "fecha_expiracion_token")
    private LocalDateTime fechaExpiracionToken;

    @Column(name = "fecha_hora_registro", nullable = false)
    private LocalDateTime fechaHoraRegistro;

    // Momento en que el encargado marca "En atención"
    @Column(name = "fecha_hora_atencion")
    private LocalDateTime fechaHoraAtencion;

    // Momento en que el registro se cierra por cualquier motivo
    @Column(name = "fecha_hora_cierre")
    private LocalDateTime fechaHoraCierre;

    // Base para la regla de los 2 minutos al revertir NO_SE_PRESENTO
    // La app verifica: Duration.between(ultimoCambioEstado, now) <= 2 min
    @Column(name = "ultimo_cambio_estado", nullable = false)
    private LocalDateTime ultimoCambioEstado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private Estado estado = Estado.PENDIENTE_VERIFICACION;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "es_primera_vez", nullable = false)
    private Boolean esPrimeraVez = false;

    @PrePersist
    public void prePersist() {
        this.fechaHoraRegistro  = LocalDateTime.now();
        this.ultimoCambioEstado = LocalDateTime.now();
        if (this.estado      == null) this.estado      = Estado.PENDIENTE_VERIFICACION;
        if (this.esPrimeraVez == null) this.esPrimeraVez = false;
    }

    @PreUpdate
    public void preUpdate() {
        if (this.estado != null && this.estado != Estado.PENDIENTE_VERIFICACION) {
            this.ultimoCambioEstado = LocalDateTime.now();
        }
    }

    // -------------------------------------------------------
    // Turno legible: letra del área + número (ej. D01, P02).
    // No se persiste: se calcula al leer.
    // -------------------------------------------------------
    @Transient
    public String getTurnoTexto() {
        String numero = this.numeroTurno != null
                ? String.format("%02d", this.numeroTurno) : "??";
        if (this.servicio == null || this.servicio.getArea() == null
                || this.servicio.getArea().getNombre() == null
                || this.servicio.getArea().getNombre().isBlank()) {
            return numero;
        }
        String inicial = this.servicio.getArea().getNombre().substring(0, 1)
                .toUpperCase();
        return inicial + numero;
    }
}
