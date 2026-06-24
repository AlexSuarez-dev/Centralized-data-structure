package com.barcelonaturisme.inventory.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuari")
@Data
public class Usuari {

    @Id
    @Column(name = "nom", length = 25)
    private String nom;

    private Integer telefon;

    @OneToOne
    @JoinColumn(name = "id_actiu") 
    private Actiu actiu; 
}