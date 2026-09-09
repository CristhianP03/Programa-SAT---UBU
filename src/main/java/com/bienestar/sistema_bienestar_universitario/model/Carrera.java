package com.bienestar.sistema_bienestar_universitario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"estudiantes", "facultad"})
@Entity
@Table(name = "carreras",
       uniqueConstraints = @UniqueConstraint(columnNames = {"nombre", "facultad_id"}))
public class Carrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @Column(nullable = false, length = 150)
    private String nombre;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facultad_id", nullable = false)
    private Facultad facultad;

    @JsonIgnore
    @OneToMany(mappedBy = "carrera", fetch = FetchType.LAZY)
    private List<Estudiante> estudiantes;
}
