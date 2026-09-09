package com.bienestar.sistema_bienestar_universitario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "configuracion_sistema")
public class ConfiguracionSistema {

    // Fila única del sistema, id siempre = 1
    @Id
    private Integer id = 1;

    // Hora del día en que se cierra el registro de asistencias.
    // NULL = sin hora configurada; la aplicación usa la hora
    // efectiva por defecto (18:00).
    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    // Fecha en que se ejecutó el último auto-cierre de la cola,
    // evita que la tarea programada se ejecute dos veces el mismo día.
    @Column(name = "fecha_ultimo_cierre")
    private LocalDate fechaUltimoCierre;

    @Version
    private Long version;
}
