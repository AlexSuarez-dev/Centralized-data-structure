package com.barcelonaturisme.inventory.service;

import com.barcelonaturisme.inventory.model.Entitat;
import com.barcelonaturisme.inventory.model.Ordinador;
import com.barcelonaturisme.inventory.model.OrdinadorType;
import com.barcelonaturisme.inventory.model.Pinpad;
import com.barcelonaturisme.inventory.repository.ActiuRepository;
import com.barcelonaturisme.inventory.repository.PinpadRepository;
import com.barcelonaturisme.inventory.repository.UsuariRepository;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelImportService {

    @Autowired
    private ActiuRepository actiuRepository;

    @Autowired
    private UsuariRepository usuariRepository;

    @Autowired
    private PinpadRepository pinpadRepository;

    public void importInventory(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(inputStream)) {

            // 1. Procesar CPUs (Hoja "CPU'S")
            Sheet sheetCpu = workbook.getSheet("CPU'S");
            if (sheetCpu != null) {
                List<Ordinador> desktops = processCpuSheet(sheetCpu);
                actiuRepository.saveAll(desktops);
            }

            // 2. Procesar Portatiles (Hoja "Portàtils")
            Sheet sheetLaptop = workbook.getSheet("Portàtils");
            if (sheetLaptop != null) {
                List<Ordinador> laptops = processLaptopSheet(sheetLaptop);
                actiuRepository.saveAll(laptops);
            }

            // 3. Procesar PINPADS (Hoja "Pinpads" o el nombre que tenga)
            Sheet sheetPinpad = workbook.getSheet("Pinpads"); // <--- CONFIRMA EL NOMBRE DE LA HOJA
            if (sheetPinpad != null) {
                List<Pinpad> pinpads = processPinpadSheet(sheetPinpad);
                pinpadRepository.saveAll(pinpads);
            }
        }
    }

    // 3. La Lógica del Pinpad
    private List<Pinpad> processPinpadSheet(Sheet sheet) {
        List<Pinpad> list = new ArrayList<>();

        // --- NUEVOS ÍNDICES (Para Pinpads_Estructurat_Final.xlsx) ---
        int colSerial = 0; // A - Serial Number
        int colModelo = 1; // B - Model
        int colUbicacio = 2; // C - Location
        int colTerminal = 3; // D - Terminal
        int colComercio = 4; // E - Num Comercio
        int colPc = 5; // F - PC Serial (Relación)
        int colEntitat = 6; // G - Entitat
        int colUser = 8; // I - User Redsys
        int colPIN = 9; // J - Pin Redsys
        int colSo = 10; // K - Version SO
        int colClaveComercio = 11; // L - Clave Comercio

        // --------------------------------------------------------

        // Iterar filas (ajusta el inicio si tienes cabeceras, ej: i=6 o i=1)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            String serial = getCellValue(row.getCell(colSerial));
            if (serial.isEmpty())
                continue;

            Pinpad p = new Pinpad();

            // Campos Básicos
            p.setSerialNumber(serial);
            p.setModel(getCellValue(row.getCell(colModelo)));
            p.setNom(getCellValue(row.getCell(colUbicacio))); // Ubicación
            p.setNumComercio(getCellValue(row.getCell(colComercio)));

            // Campos Numéricos (Terminal)
            String terminalStr = getCellValue(row.getCell(colTerminal));
            if (!terminalStr.isEmpty()) {
                try {
                    // Limpiar puntos de miles si existen
                    p.setTerminal(Integer.parseInt(terminalStr.replace(".", "").replace(",", "")));
                } catch (NumberFormatException e) {
                    System.err.println("Error formato terminal fila " + i + ": " + terminalStr);
                }
            }

            // ... dentro del bucle
            // --- RELACIÓN CON EL ORDENADOR ---
            String pcNom = getCellValue(row.getCell(colPc)).trim(); // Añadido trim()
            if (!pcNom.isEmpty() && !pcNom.equalsIgnoreCase("nan")) {
                // Buscamos el PC por su identificador visual (C0XXX)
                (actiuRepository.findByNom(pcNom)).ifPresent(comp -> {
                    p.setOrdenadorConectado((Ordinador) comp);
                });
            }

            // --- CORRECCIÓN EN ENTIDAD ---
            String entitatStr = getCellValue(row.getCell(colEntitat)).trim();
            p.setEntitat("BBVA".equalsIgnoreCase(entitatStr) ? Entitat.BBVA : Entitat.CAIXABANK);

            // --- NUEVOS CAMPOS (Asegúrate de tenerlos en tu Entity Pinpad) ---
            p.setPinRedsys(getCellValue(row.getCell(colPIN))); // User Redsys
            p.setUserRedsys(getCellValue(row.getCell(colUser)));
            p.setVersionSo(getCellValue(row.getCell(colSo))); // Versión SO
            p.setClaveComercio(getCellValue(row.getCell(colClaveComercio)));

            // Estado Activo (ya filtrado por INSTAL)
            p.setEstat(1);

            list.add(p);
        }
        return list;
    }

    // MAPEO PARA CPU'S (Fila 41 en adelante)
    private List<Ordinador> processCpuSheet(Sheet sheet) {
        List<Ordinador> list = new ArrayList<>();
        for (int i = 42; i <= 309; i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            String serial = getCellValue(row.getCell(9)); // Col J (Serial)
            if (serial.isEmpty())
                continue;

            Ordinador c = new Ordinador();
            c.setType(OrdinadorType.PC);
            c.setSerialNumber(serial);
            c.setNom(getCellValue(row.getCell(1))); // Col A (Hostname)
            c.setUbicacio(getCellValue(row.getCell(2))); // Col C (Ubicación)
            c.setModel(getCellValue(row.getCell(8))); // Col I
            c.setRam(getCellValue(row.getCell(13))); // Col N
            c.setHdd(getCellValue(row.getCell(14))); // Col O
            c.setSo(getCellValue(row.getCell(24))); // Col Y
            c.setObservacions(getCellValue(row.getCell(35))); // Col AJ
            c.setPurchaseDate(parseDateRobust(row.getCell(33))); // Col AD

            // Estado por color (Miramos la celda del serial o la primera)
            c.setEstat(determinarEstadoPorColor(row.getCell(9)));

            list.add(c);
            System.out.println("Procesado CPU: " + c.getNom() + " - Serial: " + c.getSerialNumber() + " - Estado: "
                    + c.getEstat());
        }
        return list;
    }

    // MAPEO PARA PORTÁTILES (Fila 2 en adelante)
    private List<Ordinador> processLaptopSheet(Sheet sheet) {
        List<Ordinador> list = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            // En portátiles el serial suele ser Col G (6),
            // pero si está vacío usamos el Nº Inventario Col A (0)
            String nom = getCellValue(row.getCell(0)); // Col A (Nº Inventario)
            if (nom.isEmpty())
                continue;

            Ordinador c = new Ordinador();
            c.setNom(nom);
            c.setType(OrdinadorType.Portàtil);
            c.setSerialNumber(getCellValue(row.getCell(6))); // Col G (Serial)

            String nombreUsuarioExcel = getCellValue(row.getCell(1)); // Columna de usuario

            if (!nombreUsuarioExcel.isEmpty()) {
                // Buscamos al usuario por su nombre (ID)
                // Coger primera letra y apellido para buscar en la BBDD
                String[] parts = nombreUsuarioExcel.trim().split(" ");
                if (parts.length >= 2) {
                    String firstLetter = parts[0].substring(0, 1).toLowerCase();
                    String lastName = parts[parts.length - 1].toLowerCase();
                    nombreUsuarioExcel = firstLetter + lastName;
                }

                usuariRepository.findById(nombreUsuarioExcel).ifPresent(user -> {
                    // SOLO si existe en la BBDD, hacemos la vinculación
                    user.setActiu(c);
                    c.setUsuari(user);
                });
                // Si no entra en el ifPresent, el ordenador 'c' simplemente no tendrá usuario
            }

            // Para portátiles, las columnas son distintas:
            c.setUbicacio("Passatge TB");
            c.setModel(getCellValue(row.getCell(5))); // Col F (Model2)
            c.setRam(getCellValue(row.getCell(9))); // Col J
            c.setHdd(getCellValue(row.getCell(10))); // Col K
            c.setSo(getCellValue(row.getCell(17))); // Col R
            c.setPurchaseDate(parseDateRobust(row.getCell(21))); // Col V
            c.setObservacions(getCellValue(row.getCell(20))); // Col U (Comentaris)

            c.setEstat(determinarEstadoPorColor(row.getCell(0)));

            list.add(c);
        }
        return list;
    }

    // SOPORTE PARA FECHAS EN TEXTO (CATALÁN)
    private LocalDate parseDateRobust(Cell cell) {
        if (cell == null)
            return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        String val = getCellValue(cell).toLowerCase();
        if (val.isEmpty())
            return null;

        try {
            // Diccionario básico para meses en Catalán (comunes en tu Excel)
            Map<String, String> meses = new HashMap<>();
            meses.put("gener", "01");
            meses.put("febrer", "02");
            meses.put("març", "03");
            meses.put("abril", "04");
            meses.put("maig", "05");
            meses.put("juny", "06");
            meses.put("juliol", "07");
            meses.put("agost", "08");
            meses.put("setembre", "09");
            meses.put("octubre", "10");
            meses.put("novembre", "11");
            meses.put("desembre", "12");

            for (String mes : meses.keySet()) {
                if (val.contains(mes)) {
                    String year = val.replaceAll("[^0-9]", "");
                    if (year.length() == 4) {
                        return LocalDate.of(Integer.parseInt(year), Integer.parseInt(meses.get(mes)), 1);
                    }
                }
            }

            // Intento de parseo estándar si falla lo anterior
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("[dd/MM/yyyy][yyyy-MM-dd]");
            return LocalDate.parse(getCellValue(cell), fmt);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer determinarEstadoPorColor(Cell cell) {
        if (cell == null)
            return 0;
        CellStyle style = cell.getCellStyle();
        short colorIndex = style.getFillForegroundColor();

        // Rojo (10) o Amarillo (13)
        if (colorIndex == IndexedColors.RED.getIndex() || colorIndex == IndexedColors.ROSE.getIndex()) {
            return 2; // Baja
        } else if (colorIndex == IndexedColors.YELLOW.getIndex()
                || colorIndex == IndexedColors.LIGHT_YELLOW.getIndex()) {
            return 1; // En revisión
        }
        return 0; // Activo
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.format("%.0f", cell.getNumericCellValue());
        }
        return cell.toString().trim();
    }
}