package com.barcelonaturisme.inventory.service;

import com.barcelonaturisme.inventory.dto.OrdinadorDTO;
import com.barcelonaturisme.inventory.model.EstatActiu;
import com.barcelonaturisme.inventory.model.Ordinador;
import com.barcelonaturisme.inventory.repository.ComputerRepository;
import com.barcelonaturisme.inventory.repository.PinpadRepository;
import com.barcelonaturisme.inventory.repository.UsuariRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdinadorService {

    private final ComputerRepository ordinadorRepository;
    private final UsuariRepository usuariRepository;
    private final PinpadRepository pinpadRepository;

    // Renamed from 'execute' to 'getAll' for standard REST naming
    public List<OrdinadorDTO> getAll(String search) {
        List<Ordinador> lista;

        // Added .trim() to ensure empty spaces don't trigger a false search
        if (search != null && !search.trim().isEmpty()) {
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
        Ordinador entity = findOrThrow(id);
        log.debug("Updating Ordinador {} with data {}", id, dto);
        updateEntityWithDto(entity, dto);
        return mapToDTO(ordinadorRepository.save(entity));
    }

    // Antes de borrar se desvinculan la persona y el pinpad: la persona se conserva sin equipo
    // y el pinpad queda sin PC (si no, la FK de pinpad impide el borrado).
    @Transactional
    public void delete(Long id) {
        Ordinador entity = findOrThrow(id);

        usuariRepository.findByActiuId(id).ifPresent(usuari -> {
            usuari.setActiu(null);
            usuariRepository.save(usuari);
        });
        pinpadRepository.findByOrdenadorConectadoId(id).ifPresent(pinpad -> {
            pinpad.setOrdenadorConectado(null);
            pinpadRepository.save(pinpad);
        });

        ordinadorRepository.delete(entity);
    }

    private Ordinador findOrThrow(Long id) {
        return ordinadorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el ordenador " + id));
    }

    private void updateEntityWithDto(Ordinador entity, OrdinadorDTO dto) {
        entity.setSerialNumber(dto.getSerialNumber());
        entity.setNom(dto.getNom());
        entity.setUbicacio(dto.getUbicacio());
        entity.setModel(dto.getModel());
        entity.setRam(dto.getRam());
        entity.setSo(dto.getSo());
        entity.setHdd(dto.getHdd());
        entity.setType(dto.getType());
        entity.setEstat(dto.getEstat() != null ? dto.getEstat() : EstatActiu.EN_US);
        entity.setObservacions(dto.getObservacions());

        if (dto.getPurchaseDate() != null && !dto.getPurchaseDate().isEmpty()) {
            LocalDate purchaseDate = LocalDate.parse(dto.getPurchaseDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            entity.setPurchaseDate(purchaseDate);
        } else {
            entity.setPurchaseDate(null);
        }

    }

    private OrdinadorDTO mapToDTO(Ordinador o) {
        OrdinadorDTO dto = new OrdinadorDTO();
        dto.setId(o.getId());
        dto.setNom(o.getNom());
        dto.setSerialNumber(o.getSerialNumber());
        dto.setUbicacio(o.getUbicacio());
        dto.setRam(o.getRam());
        dto.setHdd(o.getHdd());
        dto.setSo(o.getSo());
        dto.setModel(o.getModel());
        dto.setType(o.getType());
        dto.setEstat(o.getEstat());
        dto.setObservacions(o.getObservacions());
        dto.setPurchaseDate(o.getPurchaseDate() != null ? o.getPurchaseDate().toString() : null);
        return dto;
    }
}
