package com.barcelonaturisme.inventory.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.barcelonaturisme.inventory.service.ExcelImportService;

@Slf4j
@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
@Tag(name = "Importació", description = "Carga del inventario desde el Excel original")
public class ImportInventoryController {

    private final ExcelImportService excelImportService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa el Excel de inventario",
            description = "Lee las hojas CPU'S, Portàtils y Pinpads (.xlsx, máx. 10 MB) y crea los activos en una sola transacción. "
                    + "El estado se deduce del formato de cada fila (tachado/rojo = BAIXA, amarillo = REPARACIO). "
                    + "No deduplica: importar dos veces el mismo fichero crea duplicados.")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            excelImportService.importInventory(file);
            return ResponseEntity.ok("¡Archivo procesado correctamente! Base de datos actualizada.");
        } catch (Exception e) {
            log.error("Error importando {}", file.getOriginalFilename(), e);
            return ResponseEntity.internalServerError().body("Error al procesar el archivo: " + e.getMessage());
        }
    }
}
