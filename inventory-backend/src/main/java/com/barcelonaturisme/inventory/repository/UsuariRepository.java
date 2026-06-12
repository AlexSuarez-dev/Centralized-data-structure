package com.barcelonaturisme.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barcelonaturisme.inventory.model.Usuari;

@Repository
public interface UsuariRepository extends JpaRepository<Usuari, String> {
}