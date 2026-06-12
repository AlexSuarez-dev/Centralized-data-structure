package com.barcelonaturisme.inventory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.barcelonaturisme.inventory.service.ExcelImportService;

@RestController
@RequestMapping("/api/import")
public class ImportInventoryController {

    @Autowired
    private ExcelImportService excelImportService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            excelImportService.importInventory(file);
            return ResponseEntity.ok("¡Archivo procesado correctamente! Base de datos actualizada.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al procesar el archivo: " + e.getMessage());
        }
    }
}