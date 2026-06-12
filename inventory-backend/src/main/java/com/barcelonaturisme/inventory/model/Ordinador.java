package com.barcelonaturisme.inventory.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "ordinador")
@PrimaryKeyJoinColumn(name = "id_actiu") // Esto une 'actiu' con 'ordinador' usando el mismo ID
@Data
@EqualsAndHashCode(callSuper = true) // Importante para que el equals() incluya los campos del padre
public class Ordinador extends Actiu {

    private String ram;
    private String so;
    private String hdd;

    @Enumerated(EnumType.STRING) // Guarda el texto "DESKTOP" o "LAPTOP" en la BBDD
    @Column(name = "tipo_dispositivo")
    private OrdinadorType type;

    @OneToOne(mappedBy = "ordinador", cascade = CascadeType.ALL)
    private Usuari usuari;
}