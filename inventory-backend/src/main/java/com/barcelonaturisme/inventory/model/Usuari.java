package com.barcelonaturisme.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuari")
@Data
public class Usuari {

    // Login de la persona (inicial + apellido, p. ej. "mlopez"). Es la PK, no se puede cambiar.
    @Id
    @Column(name = "nom", length = 25)
    private String nom;

    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String departament;

    @Column(length = 50)
    private String kensington; // Código de la llave Kensington, p. ej. "KENS-001"

    private String observacions;

    @Enumerated(EnumType.STRING)
    private EstatUsuari estat = EstatUsuari.ACTIU;

    @OneToOne
    @JoinColumn(name = "id_actiu") 
    private Actiu actiu; 
}
