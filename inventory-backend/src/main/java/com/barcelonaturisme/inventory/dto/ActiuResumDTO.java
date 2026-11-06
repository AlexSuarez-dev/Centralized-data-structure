package com.barcelonaturisme.inventory.dto;

import lombok.Data;

// Datos mínimos de un activo para mostrarlo dentro de otra entidad (p. ej. el equipo de un usuario).
@Data
public class ActiuResumDTO {
    private Long id;
    private String nom;
    private String serialNumber;
}
