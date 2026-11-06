-- =====================================================================
-- Migración 002 · Corrección de datos de portátiles (revisión del Excel Inventari_2012, hoja Portàtils)
-- =====================================================================
-- Requiere la 001 (actiu.estat como ENUM). Idempotente: solo fija valores, se puede repetir.
-- BBDD existente: HACER BACKUP ANTES y aplicarla a mano igual que la 001.
--
-- Decisiones tomadas al revisar el Excel (2026-11-06):
--  1. El estat=1 que tenían casi todos los portátiles no salía de ningún color del Excel (significaba
--     "en uso"), así que tras la 001 aparecían como REPARACIO. En la hoja no hay ningún portátil en revisión.
--  2. Baja: todo lo anterior a P41 (equipos de 2002-2012) y las filas con el texto en rojo.
--  3. Números de serie duplicados entre portátiles: se corrigen con el fichero data/002b_serials_portatils.sql
--     (no versionado: lleva números de serie reales).

-- Imprescindible: sin esto el literal 'Portàtil' llega mal codificado y no coincide con ninguna fila
SET NAMES utf8mb4;

-- Portátil con nº de inventario "P<número>": número para comparar (P100 > P41)
-- 1. Todo portátil pasa a EN_US salvo los de baja (pasos 2 y 3)
UPDATE actiu a
JOIN ordinador o ON o.id_actiu = a.id
SET a.estat = 'EN_US'
WHERE o.tipo_dispositivo = 'Portàtil';

-- 2. Anteriores a P41
UPDATE actiu a
JOIN ordinador o ON o.id_actiu = a.id
SET a.estat = 'BAIXA'
WHERE o.tipo_dispositivo = 'Portàtil'
  AND TRIM(a.nom) REGEXP '^P[0-9]+$'
  AND CAST(SUBSTRING(TRIM(a.nom), 2) AS UNSIGNED) < 41;

-- 3. Filas con el texto en rojo en el Excel (P06, P07, P08 y P21 ya entran en el paso 2)
--    + P42: retirado al quedarse su usuario con P107
UPDATE actiu a
JOIN ordinador o ON o.id_actiu = a.id
SET a.estat = 'BAIXA'
WHERE o.tipo_dispositivo = 'Portàtil'
  AND TRIM(a.nom) IN ('P41', 'P51', 'P53', 'P54', 'P57', 'P64', 'P42');
