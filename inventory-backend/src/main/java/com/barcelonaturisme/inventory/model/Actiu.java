package com.barcelonaturisme.inventory.model;

import jakarta.persistence.*; // Importa todas las etiquetas JPA (@Entity, @Table...)
import lombok.Data; // Importa Lombok para los Getters/Setters
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate; // Importa el tipo de dato para fechas

@Entity
@Table(name = "actiu")
@Inheritance(strategy = InheritanceType.JOINED)
@Data // Esto crea Getters, Setters, toString, etc. automáticamente
public class Actiu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom")
    private String nom;

    @Column(name = "n_serie")
    private String serialNumber;

    private String model;
    private String ubicacio;

    @Column(name = "fecha_compra")
    private LocalDate purchaseDate;
    
    private String observacions;

    @Enumerated(EnumType.STRING) // Guarda el texto "EN_US", "BAIXA"... en la BBDD
    private EstatActiu estat;

    // Sin cascade: guardar o borrar un activo no debe tocar a la persona asignada.
    // La FK (usuari.id_actiu) es de Usuari, así que la asignación se guarda desde Usuari.
    // Excluido de toString/equals/hashCode: Usuari también apunta a Actiu y daría recursión infinita.
    @OneToOne(mappedBy = "actiu")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Usuari usuari;
}
