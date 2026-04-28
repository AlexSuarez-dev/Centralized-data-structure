package com.barcelonaturisme.inventory.features.usecases.gestions.ordinadors;

import com.barcelonaturisme.inventory.core.domain.Ordinador;
import com.barcelonaturisme.inventory.features.usecases.gestions.ordinadors.dto.OrdinadorDTO;
import com.barcelonaturisme.inventory.infraestructure.persistance.ComputerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdinadorService implements GetOrdinadors {

    private final ComputerRepository ordinadorRepository;

    @Override
    public List<OrdinadorDTO> execute(String search) {
        List<Ordinador> lista;

        if (search != null && !search.isEmpty()) {
            lista = ordinadorRepository.findByNomContainingIgnoreCaseOrSerialNumberContainingIgnoreCase(search, search);
        } else {
            lista = ordinadorRepository.findAll();
        }

        return lista.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public OrdinadorDTO save(OrdinadorDTO dto) {
        Ordinador entity = new Ordinador();
        updateEntityWithDto(entity, dto);
        return mapToDTO(ordinadorRepository.save(entity));
    }

    public OrdinadorDTO update(Long id, OrdinadorDTO dto) {
        Ordinador entity = ordinadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        updateEntityWithDto(entity, dto);
        return mapToDTO(ordinadorRepository.save(entity));
    }

    public void delete(Long id) {
        ordinadorRepository.deleteById(id);
    }

    // Mappers manuales (luego podrías usar MapStruct para automatizar esto)
    private void updateEntityWithDto(Ordinador entity, OrdinadorDTO dto) {
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setNom(dto.getNom());
        entity.setUbicacio(dto.getUbicacio());
        entity.setModel(dto.getModel());
        entity.setRam(dto.getRam());
        entity.setSo(dto.getSo());
        entity.setHdd(dto.getHdd());
        entity.setType(dto.getType());
        entity.setEstat(dto.getEstat());
        entity.setObservacions(dto.getObservacions());
    }

    private OrdinadorDTO mapToDTO(Ordinador o) {
        OrdinadorDTO dto = new OrdinadorDTO();
        // Datos comunes (de la clase padre Actiu)
        dto.setId(o.getId());
        dto.setNom(o.getNom());
        dto.setSerialNumber(o.getSerialNumber());
        dto.setUbicacio(o.getUbicacio());
        
        // DATOS ESPECÍFICOS
        dto.setRam(o.getRam());
        dto.setHdd(o.getHdd());
        dto.setSo(o.getSo());
        dto.setModel(o.getModel());
        dto.setType(o.getType());
        return dto;
    }
}