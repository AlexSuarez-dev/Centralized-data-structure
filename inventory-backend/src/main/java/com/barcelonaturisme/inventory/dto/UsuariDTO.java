package com.barcelonaturisme.inventory.dto;

import com.barcelonaturisme.inventory.model.EstatUsuari;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UsuariDTO {

    @Schema(description = "Login de la persona (PK). No se puede cambiar una vez creado.", example = "mlopez")
    private String nom;

    @Schema(example = "mlopez@bcnturisme.cat")
    private String email;

    @Schema(example = "Atención al cliente")
    private String departament;

    @Schema(description = "Código de la llave Kensington", example = "KENS-001")
    private String kensington;

    private String observacions;

    @Schema(description = "Si se omite, ACTIU")
    private EstatUsuari estat;

    @Schema(description = "ID del activo asignado (escritura). null = sin equipo. Un activo solo puede tener una persona.",
            example = "1570")
    private Long actiuId;

    @Schema(description = "Activo asignado (solo lectura, se ignora al guardar)", accessMode = Schema.AccessMode.READ_ONLY)
    private ActiuResumDTO actiu;
}
