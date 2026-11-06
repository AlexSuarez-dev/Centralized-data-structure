package com.barcelonaturisme.inventory.service;

import com.barcelonaturisme.inventory.dto.ActiuResumDTO;
import com.barcelonaturisme.inventory.dto.UsuariDTO;
import com.barcelonaturisme.inventory.model.Actiu;
import com.barcelonaturisme.inventory.model.EstatUsuari;
import com.barcelonaturisme.inventory.model.Usuari;
import com.barcelonaturisme.inventory.repository.ActiuRepository;
import com.barcelonaturisme.inventory.repository.UsuariRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuariService {

    private final UsuariRepository usuariRepository;
    private final ActiuRepository actiuRepository;

    public List<UsuariDTO> getAll(String search) {
        List<Usuari> lista;

        if (search != null && !search.trim().isEmpty()) {
            lista = usuariRepository.findByNomContainingIgnoreCaseOrEmailContainingIgnoreCase(search.trim(), search.trim());
        } else {
            lista = usuariRepository.findAll();
        }

        return lista.stream().map(this::mapToDTO).toList();
    }

    public UsuariDTO save(UsuariDTO dto) {
        String nom = blankToNull(dto.getNom());
        if (nom == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El login (nom) es obligatorio");
        }
        if (usuariRepository.existsById(nom)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con login " + nom);
        }

        Usuari entity = new Usuari();
        entity.setNom(nom);
        updateEntityWithDto(entity, dto);
        return mapToDTO(usuariRepository.save(entity));
    }

    // El login es la PK: no se cambia aunque venga otro en el body.
    public UsuariDTO update(String nom, UsuariDTO dto) {
        Usuari entity = findOrThrow(nom);
        updateEntityWithDto(entity, dto);
        return mapToDTO(usuariRepository.save(entity));
    }

    public void delete(String nom) {
        usuariRepository.delete(findOrThrow(nom));
    }

    private Usuari findOrThrow(String nom) {
        return usuariRepository.findById(nom)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario " + nom));
    }

    private void updateEntityWithDto(Usuari entity, UsuariDTO dto) {
        entity.setEmail(blankToNull(dto.getEmail()));
        entity.setDepartament(blankToNull(dto.getDepartament()));
        entity.setKensington(blankToNull(dto.getKensington()));
        entity.setObservacions(blankToNull(dto.getObservacions()));
        entity.setEstat(dto.getEstat() != null ? dto.getEstat() : EstatUsuari.ACTIU);
        entity.setActiu(resolveActiu(entity.getNom(), dto.getActiuId()));
    }

    // Comprueba que el activo existe y que no lo tiene otra persona (usuari.id_actiu es UNIQUE).
    private Actiu resolveActiu(String nom, Long actiuId) {
        if (actiuId == null) {
            return null;
        }

        Actiu actiu = actiuRepository.findById(actiuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe el activo " + actiuId));

        usuariRepository.findByActiuId(actiuId)
                .filter(otro -> !otro.getNom().equals(nom))
                .ifPresent(otro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "El activo " + actiuId + " ya está asignado a " + otro.getNom());
                });

        return actiu;
    }

    private UsuariDTO mapToDTO(Usuari u) {
        UsuariDTO dto = new UsuariDTO();
        dto.setNom(u.getNom());
        dto.setEmail(u.getEmail());
        dto.setDepartament(u.getDepartament());
        dto.setKensington(u.getKensington());
        dto.setObservacions(u.getObservacions());
        dto.setEstat(u.getEstat());

        if (u.getActiu() != null) {
            ActiuResumDTO actiu = new ActiuResumDTO();
            actiu.setId(u.getActiu().getId());
            actiu.setNom(u.getActiu().getNom());
            actiu.setSerialNumber(u.getActiu().getSerialNumber());
            dto.setActiu(actiu);
            dto.setActiuId(actiu.getId());
        }
        return dto;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
