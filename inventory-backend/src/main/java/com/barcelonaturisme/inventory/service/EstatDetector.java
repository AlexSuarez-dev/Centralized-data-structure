package com.barcelonaturisme.inventory.service;

import com.barcelonaturisme.inventory.model.EstatActiu;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;

/**
 * Deduce el estado de un activo a partir del formato de su fila en los Excel originales,
 * donde las bajas se marcan tachando la fila, pintándola de rojo o poniendo el texto en rojo.
 *
 * Una fila se considera marcada si lo está su celda clave (serie, nº inventario...) o al menos
 * la mitad de sus celdas con contenido. Así una sola celda coloreada por otro motivo no cambia el estado.
 * - Tachado, texto rojo o relleno rojo/rosa → BAIXA
 * - Relleno amarillo → REPARACIO
 * - Sin marcas → EN_US
 *
 * Los colores se comparan por tono (HSV), sean indexados, RGB o de tema, porque cada Excel usa
 * tonos distintos. Heurística provisional: ajustarla con el informe de tools/analisis-excel.
 */
public final class EstatDetector {

    private EstatDetector() {
    }

    public static EstatActiu detectar(Row row, int keyColumn) {
        if (row == null) {
            return EstatActiu.EN_US;
        }

        int cellsAmbContingut = 0;
        int cellsBaixa = 0;
        int cellsRevisio = 0;

        for (Cell cell : row) {
            if (cell.getCellType() == CellType.BLANK || cell.toString().isBlank()) {
                continue;
            }
            cellsAmbContingut++;
            if (esBaixa(cell)) {
                cellsBaixa++;
            } else if (esRevisio(cell)) {
                cellsRevisio++;
            }
        }

        Cell key = row.getCell(keyColumn);
        if ((key != null && esBaixa(key)) || majoria(cellsBaixa, cellsAmbContingut)) {
            return EstatActiu.BAIXA;
        }
        if ((key != null && esRevisio(key)) || majoria(cellsRevisio, cellsAmbContingut)) {
            return EstatActiu.REPARACIO;
        }
        return EstatActiu.EN_US;
    }

    private static boolean majoria(int marcades, int total) {
        return total > 0 && marcades * 2 >= total;
    }

    static boolean esBaixa(Cell cell) {
        if (!(cell.getCellStyle() instanceof XSSFCellStyle style)) {
            return false;
        }
        XSSFFont font = style.getFont();
        return font.getStrikeout() || esVermell(font.getXSSFColor(), 0.4f) || esVermell(farciment(style), 0.15f);
    }

    static boolean esRevisio(Cell cell) {
        return cell.getCellStyle() instanceof XSSFCellStyle style && esGroc(farciment(style));
    }

    private static XSSFColor farciment(XSSFCellStyle style) {
        return style.getFillPattern() == FillPatternType.NO_FILL ? null : style.getFillForegroundXSSFColor();
    }

    // Rojo o rosa. Para el texto se pide más saturación que para el relleno (los rellenos suelen ser pastel).
    private static boolean esVermell(XSSFColor color, float saturacioMinima) {
        float[] hsv = hsv(color);
        return hsv != null && (hsv[0] <= 15 || hsv[0] >= 330) && hsv[1] >= saturacioMinima && hsv[2] >= 0.4f;
    }

    private static boolean esGroc(XSSFColor color) {
        float[] hsv = hsv(color);
        return hsv != null && hsv[0] >= 40 && hsv[0] <= 70 && hsv[1] >= 0.15f && hsv[2] >= 0.7f;
    }

    // Devuelve {tono 0-360, saturación 0-1, valor 0-1}, o null si la celda no tiene color propio.
    private static float[] hsv(XSSFColor color) {
        if (color == null || color.isAuto()) {
            return null;
        }
        byte[] rgb = color.getTint() != 0 ? color.getRGBWithTint() : color.getRGB();
        if (rgb == null || rgb.length < 3) {
            return null;
        }

        float r = (rgb[0] & 0xFF) / 255f;
        float g = (rgb[1] & 0xFF) / 255f;
        float b = (rgb[2] & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float hue = 0;
        if (delta > 0) {
            if (max == r) {
                hue = 60 * (((g - b) / delta) % 6);
            } else if (max == g) {
                hue = 60 * (((b - r) / delta) + 2);
            } else {
                hue = 60 * (((r - g) / delta) + 4);
            }
        }
        if (hue < 0) {
            hue += 360;
        }
        float saturation = max == 0 ? 0 : delta / max;
        return new float[] { hue, saturation, max };
    }
}
