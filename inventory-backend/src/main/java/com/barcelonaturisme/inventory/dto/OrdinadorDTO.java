package com.barcelonaturisme.inventory.dto;

import com.barcelonaturisme.inventory.model.OrdinadorType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true) // Esto es importante para que herede correctamente los campos de BaseActiuDTO
public class OrdinadorDTO extends BaseActiuDTO {
    private String ram;
    private String so;
    private String hdd;
    private OrdinadorType type; // PC o Portàtil
    private Integer estat;
    private String observacions;
    private String purchaseDate; // String para facilitar el manejo en el frontend
}