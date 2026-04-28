package com.barcelonaturisme.inventory.features.usecases.gestions.ordinadors.dto;

import com.barcelonaturisme.inventory.core.domain.OrdinadorType;
import com.barcelonaturisme.inventory.core.dto.BaseActiuDTO;

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