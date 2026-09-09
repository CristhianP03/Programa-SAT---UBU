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
@ToString(exclude = {"carreras"})
@Entity
@Table(name = "facultades")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Facultad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Version
    private Long version;

    @Column(nullable = false, unique = true, length = 150)
    private String nombre;

    // Una facultad tiene muchas carreras
    @JsonIgnore
    @OneToMany(mappedBy = "facultad", fetch = FetchType.LAZY)
    private List<Carrera> carreras;
}
