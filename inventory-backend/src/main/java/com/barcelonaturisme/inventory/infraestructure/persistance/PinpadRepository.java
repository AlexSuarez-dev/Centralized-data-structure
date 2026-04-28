package com.barcelonaturisme.inventory.infraestructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barcelonaturisme.inventory.core.domain.Pinpad;

import java.util.Optional;

@Repository
public interface PinpadRepository extends JpaRepository<Pinpad, Long> {

    // Buscar por número de terminal (como es Integer, usamos Optional por si no existe)
    Optional<Pinpad> findByTerminal(Integer terminal);
    
    // Buscar por número de comercio
    Optional<Pinpad> findByNumComercio(String numComercio);
}