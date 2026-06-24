package com.barcelonaturisme.inventory.model;

import jakarta.persistence.*; // Importa todas las etiquetas JPA (@Entity, @Table...)
import lombok.Data; // Importa Lombok para los Getters/Setters
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
    private Integer estat; // true = activo, false = inactivo

    @OneToOne(mappedBy = "actiu", cascade = CascadeType.ALL)
    private Usuari usuari;
}