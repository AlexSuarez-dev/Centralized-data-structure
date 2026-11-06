package com.barcelonaturisme.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.barcelonaturisme.inventory.dto.OrdinadorDTO;
import com.barcelonaturisme.inventory.service.OrdinadorService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ordinadors")
@RequiredArgsConstructor
@Tag(name = "Ordinadors", description = "PCs y portátiles")
public class OrdinadorController {

    private final OrdinadorService ordinadorService;

    @GetMapping
    @Operation(summary = "Lista ordenadores",
            description = "Si se pasa `search`, filtra por nombre o número de serie (contiene, sin distinguir mayúsculas).")
    public ResponseEntity<List<OrdinadorDTO>> getActius(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ordinadorService.getAll(search));
    }

    @PostMapping
    @Operation(summary = "Crea un ordenador", description = "Si no se indica `estat`, se guarda como EN_US.")
    public ResponseEntity<OrdinadorDTO> create(@RequestBody OrdinadorDTO dto) {
        return new ResponseEntity<>(ordinadorService.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un ordenador",
            description = "Sustituye todos los campos editables: los que no se envíen quedan vacíos.")
    public ResponseEntity<OrdinadorDTO> update(@PathVariable Long id, @RequestBody OrdinadorDTO dto) {
        return ResponseEntity.ok(ordinadorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Borra un ordenador",
            description = "La persona asignada se conserva sin equipo y el pinpad conectado queda sin PC. "
                    + "Para retirar un equipo es preferible marcarlo como BAIXA.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ordinadorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
