package com.barcelonaturisme.inventory.core.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "pinpad") // Asegúrate de que la tabla en BBDD se llame así
@PrimaryKeyJoinColumn(name = "id_actiu") // Conecta con la tabla padre 'actiu'
@Data
@EqualsAndHashCode(callSuper = true)
public class Pinpad extends Actiu {

    @Column(name = "terminal")
    private int terminal;

    @Column(name = "num_comercio")
    private String numComercio;

    @Column(name = "clave_comercio")
    private String claveComercio;

    @Column(name = "version_so")
    private String versionSo;

    @Enumerated(EnumType.STRING) // Guarda el texto "caixabank" o "bbva" en la BBDD
    @Column(name = "entitat")
    private Entitat entitat;

    @Column(name = "pin_resdsys")
    private String pinRedsys;

    @Column(name = "user_redsys")
    private String userRedsys;

    // Dentro de Pinpad.java
    @OneToOne // Si un PC solo puede tener un Pinpad
    @JoinColumn(name = "id_ordinador") 
    private Ordinador ordenadorConectado;
}