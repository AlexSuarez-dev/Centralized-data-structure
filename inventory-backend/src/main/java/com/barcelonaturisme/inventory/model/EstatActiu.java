package com.barcelonaturisme.inventory.model;

// Estado de un activo. Se guarda como texto en la columna actiu.estat.
public enum EstatActiu {
    EN_US,      // En uso / instalado
    STOCK,      // En almacén, disponible para asignar
    REPARACIO,  // En revisión o reparación (amarillo en los Excel)
    BAIXA       // De baja (rojo o tachado en los Excel)
}
