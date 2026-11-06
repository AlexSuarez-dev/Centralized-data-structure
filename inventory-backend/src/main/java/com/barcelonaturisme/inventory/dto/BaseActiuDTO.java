package com.barcelonaturisme.inventory.dto;

import com.barcelonaturisme.inventory.model.EstatActiu;

import lombok.Data;

@Data
public abstract class BaseActiuDTO {
    private Long id;
    private String nom;
    private String model;
    private String serialNumber;
    private String ubicacio;
    private EstatActiu estat;
}
