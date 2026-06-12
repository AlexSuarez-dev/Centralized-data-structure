package com.barcelonaturisme.inventory.controller;

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
public class OrdinadorController {

    private final OrdinadorService ordinadorService;

    @GetMapping
    public ResponseEntity<List<OrdinadorDTO>> getActius(@RequestParam(required = false) String search) {
        // Now points directly to the service
        return ResponseEntity.ok(ordinadorService.getAll(search)); 
    }

    @PostMapping
    public ResponseEntity<OrdinadorDTO> create(@RequestBody OrdinadorDTO dto) {
        return new ResponseEntity<>(ordinadorService.save(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdinadorDTO> update(@PathVariable Long id, @RequestBody OrdinadorDTO dto) {
        return ResponseEntity.ok(ordinadorService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ordinadorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}