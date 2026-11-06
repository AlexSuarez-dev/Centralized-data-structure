package com.barcelonaturisme.inventory.service;

import com.barcelonaturisme.inventory.model.EstatActiu;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

// Sin BBDD ni Spring: construye filas en memoria con los formatos que aparecen en los Excel.
class EstatDetectorTest {

    private static final int KEY = 0;

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private int nextRow;

    @BeforeEach
    void setUp() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet();
        nextRow = 0;
    }

    @AfterEach
    void tearDown() throws IOException {
        workbook.close();
    }

    @Test
    void filaSinFormatoEstaEnUs() {
        assertThat(EstatDetector.detectar(fila(null, 4), KEY)).isEqualTo(EstatActiu.EN_US);
    }

    @Test
    void filaNulaEstaEnUs() {
        assertThat(EstatDetector.detectar(null, KEY)).isEqualTo(EstatActiu.EN_US);
    }

    @Test
    void celdaClaveTachadaEsBaixa() {
        XSSFRow row = fila(null, 4);
        XSSFCellStyle tachado = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setStrikeout(true);
        tachado.setFont(font);
        row.getCell(KEY).setCellStyle(tachado);

        assertThat(EstatDetector.detectar(row, KEY)).isEqualTo(EstatActiu.BAIXA);
    }

    @Test
    void rellenoRojoRgbEnTodaLaFilaEsBaixa() {
        assertThat(EstatDetector.detectar(fila(relleno(rgb(0xFF, 0x00, 0x00)), 4), KEY)).isEqualTo(EstatActiu.BAIXA);
    }

    @Test
    void rellenoRojoClaroEstiloMaloDeExcelEsBaixa() {
        // FFC7CE: el relleno "Incorrecto" de los estilos de Excel
        assertThat(EstatDetector.detectar(fila(relleno(rgb(0xFF, 0xC7, 0xCE)), 4), KEY)).isEqualTo(EstatActiu.BAIXA);
    }

    @Test
    void textoRojoOscuroEnLaMayoriaDeCeldasEsBaixa() {
        XSSFCellStyle textoRojo = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setColor(rgb(0xC0, 0x00, 0x00));
        textoRojo.setFont(font);

        XSSFRow row = fila(null, 4);
        row.getCell(1).setCellStyle(textoRojo);
        row.getCell(2).setCellStyle(textoRojo);

        assertThat(EstatDetector.detectar(row, KEY)).isEqualTo(EstatActiu.BAIXA);
    }

    @Test
    void rellenoAmarilloIndexadoEnLaClaveEsReparacio() {
        XSSFCellStyle amarillo = workbook.createCellStyle();
        amarillo.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        amarillo.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        XSSFRow row = fila(null, 4);
        row.getCell(KEY).setCellStyle(amarillo);

        assertThat(EstatDetector.detectar(row, KEY)).isEqualTo(EstatActiu.REPARACIO);
    }

    @Test
    void unaSolaCeldaNoClaveColoreadaNoCambiaElEstado() {
        XSSFRow row = fila(null, 4);
        row.getCell(3).setCellStyle(relleno(rgb(0xFF, 0x00, 0x00)));

        assertThat(EstatDetector.detectar(row, KEY)).isEqualTo(EstatActiu.EN_US);
    }

    @Test
    void rellenoVerdeOAzulNoEsBaja() {
        assertThat(EstatDetector.detectar(fila(relleno(rgb(0xC6, 0xEF, 0xCE)), 4), KEY)).isEqualTo(EstatActiu.EN_US);
        assertThat(EstatDetector.detectar(fila(relleno(rgb(0xDD, 0xEB, 0xF7)), 4), KEY)).isEqualTo(EstatActiu.EN_US);
    }

    // --- helpers ---

    private XSSFRow fila(XSSFCellStyle style, int columnas) {
        XSSFRow row = sheet.createRow(nextRow++);
        for (int c = 0; c < columnas; c++) {
            var cell = row.createCell(c);
            cell.setCellValue("valor " + c);
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
        return row;
    }

    private XSSFCellStyle relleno(XSSFColor color) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static XSSFColor rgb(int r, int g, int b) {
        return new XSSFColor(new byte[] { (byte) r, (byte) g, (byte) b }, null);
    }
}
