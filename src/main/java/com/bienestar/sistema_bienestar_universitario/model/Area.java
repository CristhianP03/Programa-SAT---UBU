package com.bienestar.sistema_bienestar_universitario.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"servicios"})
@Entity
@Table(name = "areas")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    @OneToMany(mappedBy = "area", fetch = FetchType.LAZY)
    private List<Servicio> servicios;
}
