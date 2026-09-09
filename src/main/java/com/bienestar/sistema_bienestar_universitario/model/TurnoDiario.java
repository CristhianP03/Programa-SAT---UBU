package com.bienestar.sistema_bienestar_universitario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"area"})
@Entity
@Table(name = "turnos_diarios",
       uniqueConstraints = @UniqueConstraint(columnNames = {"area_id", "fecha"}))
public class TurnoDiario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    // Contador de turnos por área por día
    // Se crea un registro nuevo cada día automáticamente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(nullable = false)
    private LocalDate fecha;

    // Último número de turno asignado ese día para ese servicio
    @Column(name = "ultimo_turno", nullable = false)
    private Integer ultimoTurno = 0;

    @PrePersist
    public void prePersist() {
        if (this.fecha == null) this.fecha = LocalDate.now();
        if (this.ultimoTurno == null) this.ultimoTurno = 0;
    }
}
