package com.barcelonaturisme.inventory.core.dto;

import lombok.Data;

@Data
public abstract class BaseActiuDTO {
    private Long id;
    private String nom;
    private String model;
    private String serialNumber;
    private String ubicacio;
    private Integer estat;
}
