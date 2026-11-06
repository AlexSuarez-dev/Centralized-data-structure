package com.barcelonaturisme.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barcelonaturisme.inventory.model.Usuari;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuariRepository extends JpaRepository<Usuari, String> {

    // Busca coincidencias en login O email, ignorando mayúsculas/minúsculas
    List<Usuari> findByNomContainingIgnoreCaseOrEmailContainingIgnoreCase(String nom, String email);

    // Persona que tiene asignado un activo (usuari.id_actiu es UNIQUE)
    Optional<Usuari> findByActiuId(Long actiuId);
}
