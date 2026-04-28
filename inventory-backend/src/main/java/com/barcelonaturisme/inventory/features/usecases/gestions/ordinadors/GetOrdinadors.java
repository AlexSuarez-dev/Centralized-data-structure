package com.barcelonaturisme.inventory.features.usecases.gestions.ordinadors;

import java.util.List;

import com.barcelonaturisme.inventory.features.usecases.gestions.ordinadors.dto.OrdinadorDTO;

public interface GetOrdinadors {
    List<OrdinadorDTO> execute(String searchKeyword);
}
