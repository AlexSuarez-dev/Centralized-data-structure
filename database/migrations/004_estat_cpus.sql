-- =====================================================================
-- Migración 004 · Estado de los PCs según el formato del Excel (hoja CPU'S) — 2026-11-06
-- =====================================================================
-- El importador antiguo solo reconocía el rojo "indexado"; el Excel usa sobre todo rojo RGB (FF0000),
-- así que muchos PCs de baja quedaron como EN_US. Estado detectado con tools/analisis-excel (EstatDetector).
-- Requiere la 001. Idempotente: fija valores. Hacer BACKUP antes en una BBDD real.
-- Total: 48 PCs. Confianza media (solo parte de la fila marcada; revisar a mano): C0083, C0112, C0120, C0165, C0177.
-- NO se tocan los portátiles (ver 002) ni los pinpads.

SET NAMES utf8mb4;

-- EN_US -> BAIXA (42)
UPDATE actiu a JOIN ordinador o ON o.id_actiu = a.id SET a.estat = 'BAIXA'
WHERE o.tipo_dispositivo = 'PC' AND TRIM(a.nom) IN ('C0070', 'C0083', 'C0112', 'C0122', 'C0126', 'C0134', 'C0143', 'C0158', 'C0159', 'C0163', 'C0165', 'C0170', 'C0171', 'C0174', 'C0177', 'C0178', 'C0181', 'C0184', 'C0185', 'C0186', 'C0189', 'C0190', 'C0191', 'C0192', 'C0194', 'C0195', 'C0198', 'C0199', 'C0201', 'C0202', 'C0204', 'C0207', 'C0210', 'C0211', 'C0232', 'C0264', 'C0269', 'C0289', 'C0294', 'C0305', 'C0328', 'C0345');

-- REPARACIO -> BAIXA (2)
UPDATE actiu a JOIN ordinador o ON o.id_actiu = a.id SET a.estat = 'BAIXA'
WHERE o.tipo_dispositivo = 'PC' AND TRIM(a.nom) IN ('C0093', 'C0093B');

-- EN_US -> REPARACIO (4)
UPDATE actiu a JOIN ordinador o ON o.id_actiu = a.id SET a.estat = 'REPARACIO'
WHERE o.tipo_dispositivo = 'PC' AND TRIM(a.nom) IN ('C0120', 'C0266', 'C0323', 'C0325');
