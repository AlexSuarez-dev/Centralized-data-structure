package com.barcelonaturisme.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.barcelonaturisme.inventory.dto.UsuariDTO;
import com.barcelonaturisme.inventory.service.UsuariService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuaris")
@RequiredArgsConstructor
@Tag(name = "Usuaris", description = "Personas y el equipo que tienen asignado")
public class UsuariController {

    private final UsuariService usuariService;

    @GetMapping
    @Operation(summary = "Lista usuarios", description = "Si se pasa `search`, filtra por login o email.")
    public ResponseEntity<List<UsuariDTO>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(usuariService.getAll(search));
    }

    @PostMapping
    @Operation(summary = "Crea un usuario",
            description = "409 si el login ya existe o si el activo ya está asignado a otra persona.")
    public ResponseEntity<UsuariDTO> create(@RequestBody UsuariDTO dto) {
        return new ResponseEntity<>(usuariService.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{nom}")
    @Operation(summary = "Actualiza un usuario",
            description = "El login no se puede cambiar. `actiuId: null` deja a la persona sin equipo.")
    public ResponseEntity<UsuariDTO> update(@PathVariable String nom, @RequestBody UsuariDTO dto) {
        return ResponseEntity.ok(usuariService.update(nom, dto));
    }

    @DeleteMapping("/{nom}")
    @Operation(summary = "Borra un usuario",
            description = "El activo que tenía asignado no se borra. Para una persona que se va es preferible marcarla como BAIXA.")
    public ResponseEntity<Void> delete(@PathVariable String nom) {
        usuariService.delete(nom);
        return ResponseEntity.noContent().build();
    }
}
