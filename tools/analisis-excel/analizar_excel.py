"""
Análisis de los Excel de inventario antes de migrarlos a la BBDD.

En los Excel originales las bajas se marcan sin un patrón fijo: filas tachadas, rellenas de rojo,
con el texto en rojo, en amarillo (en revisión)... Este script NO decide por ti: detecta esas
señales fila a fila, propone un estado con un nivel de confianza y te deja un Excel de revisión
para confirmar cada caso. Se trabaja hoja por hoja con un fichero de perfiles.

Uso:
    py analizar_excel.py descubrir  inventario.xlsx
        -> crea inventario_perfiles.json (revísalo: fila de cabecera, primera fila, columna clave...)

    py analizar_excel.py analizar   inventario.xlsx [--perfiles inventario_perfiles.json]
        -> inventario_revision.xlsx  copia con 4 columnas nuevas para revisar. SE QUEDA EN TU EQUIPO.
        -> inventario_informe.md     solo estructura y recuentos, SIN valores de celda. Es lo que se
                                     comparte para ajustar la importación automática.

Requisitos: Python 3.9+ y openpyxl (py -m pip install openpyxl). El Excel original no se modifica.
"""

import argparse
import colorsys
import json
import re
import sys
import unicodedata
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from datetime import datetime
from pathlib import Path

try:
    import openpyxl
    from openpyxl.cell.rich_text import CellRichText, TextBlock
    from openpyxl.styles import Font, PatternFill
    from openpyxl.styles.colors import COLOR_INDEX
    from openpyxl.utils import column_index_from_string, get_column_letter
    from openpyxl.worksheet.datavalidation import DataValidation
except ImportError:
    sys.exit("Falta openpyxl. Instálalo con:  py -m pip install openpyxl")

ESTATS = ["EN_US", "STOCK", "REPARACIO", "BAIXA"]
PALABRAS_BAJA_POR_DEFECTO = ["baja", "baixa", "de baja", "retirat", "retirado", "robat", "robado", "desballestat"]
COLUMNAS_REVISION = ["ESTAT_DETECTAT", "CONFIANÇA", "SENYALS", "ESTAT_REVISAT"]
MAX_FILAS_A_REVISAR_EN_INFORME = 60

# Colores para marcar la columna ESTAT_DETECTAT en el Excel de revisión
RELLENO_ESTAT = {
    "BAIXA": "FFF4CCCC",
    "REPARACIO": "FFFFF2CC",
    "EN_US": "FFD9EAD3",
}


# ---------------------------------------------------------------------------
# Colores: openpyxl devuelve colores RGB, indexados o de tema (+ tint). Se resuelven a RGB
# y se clasifican por tono, porque cada Excel usa un rojo o un amarillo distinto.
# ---------------------------------------------------------------------------

class ResolutorColores:
    # Excel numera los colores de tema intercambiando claro/oscuro respecto al XML del tema
    ORDEN_TEMA = ["lt1", "dk1", "lt2", "dk2", "accent1", "accent2", "accent3", "accent4",
                  "accent5", "accent6", "hlink", "folHlink"]

    def __init__(self, workbook):
        self.tema = self._leer_tema(workbook)

    def _leer_tema(self, workbook):
        colores = {}
        xml = getattr(workbook, "loaded_theme", None)
        if not xml:
            return colores
        try:
            raiz = ET.fromstring(xml)
        except ET.ParseError:
            return colores
        ns = {"a": "http://schemas.openxmlformats.org/drawingml/2006/main"}
        esquema = raiz.find(".//a:clrScheme", ns)
        if esquema is None:
            return colores
        for nodo in esquema:
            nombre = nodo.tag.split("}")[-1]
            srgb = nodo.find("a:srgbClr", ns)
            sys_clr = nodo.find("a:sysClr", ns)
            if srgb is not None:
                colores[nombre] = srgb.get("val")
            elif sys_clr is not None:
                colores[nombre] = sys_clr.get("lastClr")
        return colores

    def resolver(self, color):
        """Devuelve (hex RRGGBB, origen) o (None, None) si no hay color propio."""
        if color is None:
            return None, None
        tipo = getattr(color, "type", None)
        rgb = None
        origen = tipo
        if tipo == "rgb" and isinstance(color.rgb, str):
            rgb = color.rgb[-6:]
            if color.rgb.upper() in ("00000000",):  # "sin color" que algunos ficheros guardan así
                return None, None
        elif tipo == "indexed" and isinstance(color.indexed, int):
            if color.indexed >= 64:  # 64/65 = color automático del sistema
                return None, None
            if color.indexed < len(COLOR_INDEX):
                rgb = COLOR_INDEX[color.indexed][-6:]
            origen = f"indexed:{color.indexed}"
        elif tipo == "theme" and isinstance(color.theme, int):
            if color.theme < len(self.ORDEN_TEMA):
                rgb = self.tema.get(self.ORDEN_TEMA[color.theme])
            origen = f"theme:{color.theme}"
        if not rgb:
            return None, None
        tint = getattr(color, "tint", 0) or 0
        if tint:
            rgb = aplicar_tint(rgb, tint)
            origen = f"{origen} tint {tint:+.2f}"
        return rgb.upper(), origen


def aplicar_tint(hex_rgb, tint):
    r, g, b = (int(hex_rgb[i:i + 2], 16) / 255 for i in (0, 2, 4))
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    l = l * (1 + tint) if tint < 0 else l * (1 - tint) + tint
    r, g, b = colorsys.hls_to_rgb(h, max(0, min(1, l)), s)
    return "".join(f"{round(c * 255):02X}" for c in (r, g, b))


def clasificar(hex_rgb, saturacion_minima):
    """'rojo', 'amarillo' u 'otro' según el tono (mismos umbrales que EstatDetector.java)."""
    if not hex_rgb:
        return None
    r, g, b = (int(hex_rgb[i:i + 2], 16) / 255 for i in (0, 2, 4))
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    h *= 360
    if (h <= 15 or h >= 330) and s >= saturacion_minima and v >= 0.4:
        return "rojo"
    if 40 <= h <= 70 and s >= 0.15 and v >= 0.7:
        return "amarillo"
    return "otro"


# ---------------------------------------------------------------------------
# Utilidades
# ---------------------------------------------------------------------------

def sin_acentos(texto):
    return "".join(c for c in unicodedata.normalize("NFKD", texto) if not unicodedata.combining(c)).lower()


def tiene_contenido(valor):
    return valor is not None and str(valor).strip() != ""


def es_texto(valor):
    return isinstance(valor, (str, CellRichText)) and str(valor).strip() != ""


def tachado_parcial(valor):
    """En celdas con texto enriquecido, solo una parte puede estar tachada."""
    if not isinstance(valor, CellRichText):
        return False
    return any(isinstance(b, TextBlock) and b.font is not None and b.font.strike for b in valor)


def letra_a_indice(letra):
    return column_index_from_string(letra.strip().upper()) if letra else None


def columnas_con_formato_condicional(ws):
    """Para cada fila, las columnas con formato condicional (su color real no está en el estilo de la celda)."""
    columnas_por_fila = defaultdict(set)
    rangos = []
    ultima_fila, ultima_col = ws.max_row, ws.max_column
    for rango in ws.conditional_formatting:
        rangos.append(str(rango.sqref))
        for cr in rango.sqref.ranges:
            # Rangos de columna entera (A:Z) llegan hasta la fila 1048576: se recortan a lo usado
            columnas = range(cr.min_col, min(cr.max_col, ultima_col) + 1)
            for fila in range(cr.min_row, min(cr.max_row, ultima_fila) + 1):
                columnas_por_fila[fila].update(columnas)
    return columnas_por_fila, rangos


# ---------------------------------------------------------------------------
# Modo descubrir
# ---------------------------------------------------------------------------

# Columna clave sugerida, por orden de preferencia: nº de serie > nº de inventario > nombre del equipo
PATRONES_COLUMNA_CLAVE = [
    re.compile(r"serie|serial|s/n", re.I),
    re.compile(r"n[ºo°.]?\s*inv|inventari", re.I),
    re.compile(r"hostname|nom\s+equip|nombre\s+equipo", re.I),
]


def sugerir_columna_clave(cabecera):
    for patron in PATRONES_COLUMNA_CLAVE:
        for idx, nombre in enumerate(cabecera, start=1):
            if nombre is not None and patron.search(str(nombre)):
                return get_column_letter(idx)
    return "A"


def descubrir(ruta_excel, forzar):
    ruta_perfiles = ruta_excel.with_name(f"{ruta_excel.stem}_perfiles.json")
    if ruta_perfiles.exists() and not forzar:
        sys.exit(f"Ya existe {ruta_perfiles.name}. Revísalo o usa --forzar para regenerarlo.")

    wb = openpyxl.load_workbook(ruta_excel, read_only=True, data_only=True)
    perfiles = {}
    print(f"\n{ruta_excel.name}: {len(wb.sheetnames)} hojas\n")

    for ws in wb.worksheets:
        filas = list(ws.iter_rows(min_row=1, max_row=min(ws.max_row or 0, 80), values_only=True))
        # Cabecera sugerida: la fila con más celdas de texto entre las 80 primeras
        mejor_fila, mejor_cuenta = 1, -1
        for i, fila in enumerate(filas, start=1):
            cuenta = sum(1 for v in fila if es_texto(v))
            if cuenta > mejor_cuenta:
                mejor_fila, mejor_cuenta = i, cuenta
        cabecera = filas[mejor_fila - 1] if filas else ()

        columna_clave = sugerir_columna_clave(cabecera)

        perfiles[ws.title] = {
            "analizar": mejor_cuenta >= 2,
            "fila_cabecera": mejor_fila,
            "primera_fila_datos": mejor_fila + 1,
            "ultima_fila_datos": None,
            "columna_clave": columna_clave,
            "columnas_ignoradas": [],
            "umbral_mayoria": 0.5,
            "palabras_baja": PALABRAS_BAJA_POR_DEFECTO,
        }

        nombres = [f"{get_column_letter(i)}={str(v).strip()}" for i, v in enumerate(cabecera, start=1) if es_texto(v)]
        print(f"- {ws.title!r}: {ws.max_row} filas x {ws.max_column} columnas")
        print(f"    cabecera sugerida: fila {mejor_fila} -> {', '.join(nombres[:15])}{' ...' if len(nombres) > 15 else ''}")
        print(f"    columna clave sugerida: {columna_clave}   analizar: {perfiles[ws.title]['analizar']}")

    wb.close()
    ruta_perfiles.write_text(json.dumps(perfiles, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nPerfiles guardados en {ruta_perfiles.name}. Revísalos hoja por hoja y después ejecuta:")
    print(f"  py analizar_excel.py analizar \"{ruta_excel.name}\"")


# ---------------------------------------------------------------------------
# Modo analizar
# ---------------------------------------------------------------------------

def analizar_fila(ws, n_fila, perfil, resolutor, columnas_cf, stats):
    clave = letra_a_indice(perfil.get("columna_clave"))
    ignoradas = {letra_a_indice(c) for c in perfil.get("columnas_ignoradas", [])}
    palabras = [sin_acentos(p) for p in perfil.get("palabras_baja", PALABRAS_BAJA_POR_DEFECTO)]
    umbral = float(perfil.get("umbral_mayoria", 0.5))

    con_contenido = 0
    n_con_cf = 0
    n_baja = 0
    n_amarillo = 0
    marcas = Counter()
    clave_baja = clave_amarillo = False
    palabras_encontradas = set()

    for cell in ws[n_fila]:
        if cell.column in ignoradas or not tiene_contenido(cell.value):
            continue
        con_contenido += 1
        es_clave = cell.column == clave
        if cell.column in columnas_cf.get(n_fila, ()):
            n_con_cf += 1

        tachada = bool(cell.font and cell.font.strike)
        parcial = tachado_parcial(cell.value)
        hex_fuente, origen_fuente = resolutor.resolver(cell.font.color if cell.font else None)
        cat_fuente = clasificar(hex_fuente, 0.4)
        hex_relleno, origen_relleno = (None, None)
        if cell.fill is not None and cell.fill.fill_type not in (None, "none"):
            hex_relleno, origen_relleno = resolutor.resolver(cell.fill.fgColor)
        cat_relleno = clasificar(hex_relleno, 0.15)

        if hex_relleno:
            stats["rellenos"][(hex_relleno, origen_relleno, cat_relleno)] += 1
        if hex_fuente and hex_fuente not in ("000000",):
            stats["fuentes"][(hex_fuente, origen_fuente, cat_fuente)] += 1

        baja = tachada or cat_fuente == "rojo" or cat_relleno == "rojo"
        if tachada:
            marcas["tachado"] += 1
        if parcial:
            marcas["tachado_parcial"] += 1
        if cat_fuente == "rojo":
            marcas["fuente_roja"] += 1
        if cat_relleno == "rojo":
            marcas["relleno_rojo"] += 1
        if baja:
            n_baja += 1
            clave_baja = clave_baja or es_clave
        elif cat_relleno == "amarillo":
            marcas["relleno_amarillo"] += 1
            n_amarillo += 1
            clave_amarillo = clave_amarillo or es_clave

        texto = sin_acentos(str(cell.value))
        for p in palabras:
            if re.search(rf"\b{re.escape(p)}\b", texto):
                palabras_encontradas.add(p)
                stats["palabras_por_columna"][(p, stats["cabecera"].get(cell.column, get_column_letter(cell.column)))] += 1

    if con_contenido == 0:
        return None

    oculta = bool(ws.row_dimensions[n_fila].hidden)
    # Solo cuenta si el formato condicional podría estar marcando la fila entera (clave o mayoría de celdas);
    # uno que solo colorea una columna (p. ej. la RAM) se menciona en el informe de la hoja, no en cada fila.
    en_cf = clave in columnas_cf.get(n_fila, ()) or n_con_cf / con_contenido >= umbral
    mayoria_baja = n_baja / con_contenido >= umbral
    mayoria_amarillo = n_amarillo / con_contenido >= umbral

    senyals = [f"{k} {v}/{con_contenido}" for k, v in marcas.items()]
    if clave_baja:
        senyals.append("clave marcada como baja")
    if clave_amarillo:
        senyals.append("clave en amarillo")
    senyals += [f"palabra '{p}'" for p in sorted(palabras_encontradas)]
    if oculta:
        senyals.append("fila oculta")
    if en_cf:
        senyals.append("formato condicional (color no visible para el script)")

    # Decisión: alta = varias señales coinciden; media = una sola señal; baja = señales débiles o contradictorias
    if mayoria_baja and (clave_baja or clave is None):
        estat, confianza = "BAIXA", "alta"
    elif mayoria_baja or clave_baja:
        estat, confianza = "BAIXA", "media"
    elif mayoria_amarillo and (clave_amarillo or clave is None):
        estat, confianza = "REPARACIO", "alta"
    elif mayoria_amarillo or clave_amarillo:
        estat, confianza = "REPARACIO", "media"
    elif palabras_encontradas or marcas.get("tachado_parcial"):
        estat, confianza = "BAIXA", "baja"
    elif marcas:
        estat, confianza = "EN_US", "baja"  # alguna celda marcada, pero en minoría
    else:
        estat, confianza = "EN_US", "alta"

    if (oculta or en_cf) and confianza == "alta":
        confianza = "media"

    return estat, confianza, senyals


def analizar(ruta_excel, ruta_perfiles):
    if ruta_perfiles is None:
        ruta_perfiles = ruta_excel.with_name(f"{ruta_excel.stem}_perfiles.json")
    if not ruta_perfiles.exists():
        sys.exit(f"No encuentro {ruta_perfiles.name}. Ejecuta antes:  py analizar_excel.py descubrir \"{ruta_excel.name}\"")
    perfiles = json.loads(ruta_perfiles.read_text(encoding="utf-8"))

    # rich_text=True para detectar tachados parciales dentro de una celda
    wb = openpyxl.load_workbook(ruta_excel, rich_text=True)
    resolutor = ResolutorColores(wb)
    informe_hojas = []

    for nombre_hoja, perfil in perfiles.items():
        if not perfil.get("analizar", True):
            continue
        if nombre_hoja not in wb.sheetnames:
            print(f"  ! La hoja {nombre_hoja!r} del perfil no existe en el Excel, se ignora")
            continue
        ws = wb[nombre_hoja]
        print(f"  Analizando {nombre_hoja!r}...")

        fila_cab = int(perfil["fila_cabecera"])
        primera = int(perfil["primera_fila_datos"])
        ultima = int(perfil["ultima_fila_datos"] or ws.max_row)
        col_original_max = ws.max_column
        cabecera = {c.column: str(c.value).strip() for c in ws[fila_cab] if tiene_contenido(c.value)}
        columnas_cf, rangos_cf = columnas_con_formato_condicional(ws)

        stats = {
            "rellenos": Counter(), "fuentes": Counter(), "palabras_por_columna": Counter(),
            "cabecera": cabecera,
        }
        resultados = {}
        for n in range(primera, ultima + 1):
            r = analizar_fila(ws, n, perfil, resolutor, columnas_cf, stats)
            if r is not None:
                resultados[n] = r

        escribir_columnas_revision(ws, fila_cab, col_original_max, resultados)
        informe_hojas.append({
            "nombre": nombre_hoja, "perfil": perfil, "cabecera": cabecera, "resultados": resultados,
            "stats": stats, "rangos_cf": rangos_cf, "vacias": (ultima - primera + 1) - len(resultados),
            "ocultas": sum(1 for n in resultados if ws.row_dimensions[n].hidden),
        })

    ruta_revision = ruta_excel.with_name(f"{ruta_excel.stem}_revision.xlsx")
    ruta_informe = ruta_excel.with_name(f"{ruta_excel.stem}_informe.md")
    wb.save(ruta_revision)
    ruta_informe.write_text(generar_informe(ruta_excel.name, informe_hojas), encoding="utf-8")

    print(f"\nListo:\n  {ruta_revision.name}  -> revisa y rellena ESTAT_REVISAT (NO lo compartas: contiene los datos)")
    print(f"  {ruta_informe.name}  -> recuentos sin datos, es lo que puedes compartir")


def escribir_columnas_revision(ws, fila_cab, col_original_max, resultados):
    base = col_original_max + 1
    negrita = Font(bold=True)
    for i, titulo in enumerate(COLUMNAS_REVISION):
        celda = ws.cell(row=fila_cab, column=base + i, value=titulo)
        celda.font = negrita

    validacion = DataValidation(type="list", formula1='"' + ",".join(ESTATS) + '"', allow_blank=True)
    ws.add_data_validation(validacion)

    for n, (estat, confianza, senyals) in resultados.items():
        c_estat = ws.cell(row=n, column=base, value=estat)
        c_estat.fill = PatternFill("solid", fgColor=RELLENO_ESTAT.get(estat, "FFFFFFFF"))
        c_estat.font = Font(strike=False, color="FF000000")
        ws.cell(row=n, column=base + 1, value=confianza).font = Font(strike=False, color="FF000000")
        ws.cell(row=n, column=base + 2, value="; ".join(senyals)).font = Font(strike=False, color="FF000000")
        revisat = ws.cell(row=n, column=base + 3)
        # Si la confianza es alta se propone el mismo estado; el resto se deja vacío para decidir a mano
        revisat.value = estat if confianza == "alta" else None
        validacion.add(revisat)

    for i, ancho in enumerate((16, 12, 60, 16)):
        ws.column_dimensions[get_column_letter(base + i)].width = ancho


def generar_informe(nombre_excel, hojas):
    """Informe SIN valores de celda: solo nombres de columna, números de fila, colores y recuentos."""
    out = [
        f"# Informe de análisis · {nombre_excel}",
        "",
        f"Generado el {datetime.now():%Y-%m-%d %H:%M}. No contiene valores de celda: solo la estructura, "
        "los formatos encontrados y recuentos.",
        "",
    ]
    for h in hojas:
        p = h["perfil"]
        res = h["resultados"]
        out += [
            f"## Hoja `{h['nombre']}`",
            "",
            f"- Perfil: cabecera fila {p['fila_cabecera']}, datos {p['primera_fila_datos']}–{p['ultima_fila_datos'] or 'final'}, "
            f"columna clave {p.get('columna_clave')}, ignoradas {p.get('columnas_ignoradas') or '—'}, umbral {p.get('umbral_mayoria')}",
            f"- Filas con datos: {len(res)} · vacías en el rango: {h['vacias']} · ocultas: {h['ocultas']}",
            f"- Formato condicional: {len(h['rangos_cf'])} rango(s){': ' + ', '.join(h['rangos_cf'][:10]) if h['rangos_cf'] else ''}",
            f"- Columnas: {', '.join(f'{get_column_letter(c)}={n}' for c, n in sorted(h['cabecera'].items()))}",
            "",
            "### Resultado propuesto",
            "",
            "| Estado | alta | media | baja | total |",
            "|---|---|---|---|---|",
        ]
        tabla = defaultdict(Counter)
        for estat, conf, _ in res.values():
            tabla[estat][conf] += 1
        for estat in ESTATS:
            if tabla[estat]:
                t = tabla[estat]
                out.append(f"| {estat} | {t['alta']} | {t['media']} | {t['baja']} | {sum(t.values())} |")

        senyales = Counter()
        for _, _, s in res.values():
            for sen in s:
                senyales[re.sub(r" \d+/\d+$", "", sen)] += 1
        out += ["", "### Señales (nº de filas)", ""]
        out += [f"- {s}: {n}" for s, n in senyales.most_common()] or ["- Ninguna"]

        for titulo, clave in (("Colores de relleno", "rellenos"), ("Colores de fuente (sin contar negro)", "fuentes")):
            out += ["", f"### {titulo}", "", "| Color | Origen | Categoría | Nº celdas |", "|---|---|---|---|"]
            filas = h["stats"][clave].most_common(25)
            out += [f"| #{hx} | {orig} | {cat} | {n} |" for (hx, orig, cat), n in filas] or ["| — | | | |"]

        if h["stats"]["palabras_por_columna"]:
            out += ["", "### Palabras clave encontradas (por columna)", ""]
            out += [f"- '{p}' en columna {col}: {n}" for (p, col), n in h["stats"]["palabras_por_columna"].most_common()]

        dudosas = [n for n, (_, conf, _) in res.items() if conf != "alta"]
        out += ["", f"### Filas a revisar ({len(dudosas)})", ""]
        for n in dudosas[:MAX_FILAS_A_REVISAR_EN_INFORME]:
            estat, conf, s = res[n]
            out.append(f"- Fila {n}: {estat} ({conf}) — {'; '.join(s)}")
        if len(dudosas) > MAX_FILAS_A_REVISAR_EN_INFORME:
            out.append(f"- … y {len(dudosas) - MAX_FILAS_A_REVISAR_EN_INFORME} más (ver el Excel de revisión)")
        out.append("")
    return "\n".join(out)


def main():
    # Acentos correctos también cuando la salida va a un pipe o a la consola de Git Bash
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description="Detecta filas de baja en los Excel de inventario.")
    sub = parser.add_subparsers(dest="modo", required=True)

    p_desc = sub.add_parser("descubrir", help="Genera el fichero de perfiles por hoja")
    p_desc.add_argument("excel", type=Path)
    p_desc.add_argument("--forzar", action="store_true", help="Sobrescribe el fichero de perfiles")

    p_an = sub.add_parser("analizar", help="Genera el Excel de revisión y el informe")
    p_an.add_argument("excel", type=Path)
    p_an.add_argument("--perfiles", type=Path, default=None)

    args = parser.parse_args()
    if not args.excel.exists():
        sys.exit(f"No existe {args.excel}")
    if args.excel.suffix.lower() not in (".xlsx", ".xlsm"):
        sys.exit("Solo .xlsx/.xlsm. Si es un .xls antiguo, ábrelo y guárdalo como .xlsx.")

    if args.modo == "descubrir":
        descubrir(args.excel, args.forzar)
    else:
        analizar(args.excel, args.perfiles)


if __name__ == "__main__":
    main()
