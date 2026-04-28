package com.barcelonaturisme.inventory.infraestructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barcelonaturisme.inventory.core.domain.Usuari;

@Repository
public interface UsuariRepository extends JpaRepository<Usuari, String> {
}