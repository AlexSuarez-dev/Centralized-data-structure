package com.barcelonaturisme.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barcelonaturisme.inventory.model.Ordinador;
import com.barcelonaturisme.inventory.model.OrdinadorType;

import java.util.List;

@Repository
public interface ComputerRepository extends JpaRepository<Ordinador, Long> {

    List<Ordinador> findByType(OrdinadorType type);

    // Encontrar por nombre
    List<Ordinador> findByNom(String nom);

    // Busca coincidencias en nombre O número de serie, ignorando
    // mayúsculas/minúsculas
    List<Ordinador> findByNomContainingIgnoreCaseOrSerialNumberContainingIgnoreCase(String nom, String serialNumber);
}