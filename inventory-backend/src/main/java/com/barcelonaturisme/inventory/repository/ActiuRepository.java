package com.barcelonaturisme.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barcelonaturisme.inventory.model.Actiu;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActiuRepository extends JpaRepository<Actiu, Long> {

    // Encuentra cualquier activo por su número de serie
    Optional<Actiu> findBySerialNumber(String serialNumber);

    // Encuentra activos por ubicación
    List<Actiu> findByUbicacio(String ubicacio);

    // Encuentra por nombre
    Optional<Actiu> findByNom(String nom);

}