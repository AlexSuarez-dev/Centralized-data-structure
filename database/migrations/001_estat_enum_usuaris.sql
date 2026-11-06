-- =====================================================================
-- Migración 001 · estat como enum único, FK duplicada de pinpad y
--                 nuevos campos de usuari (sin teléfono)
-- =====================================================================
-- Instalación nueva: MySQL la ejecuta sola después de init.sql (ver docker-compose.yml).
-- BBDD existente: HACER BACKUP ANTES y aplicarla a mano, por ejemplo:
--   docker exec inventario-mysql sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' > backup.sql
--   docker exec -i inventario-mysql sh -c 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < database/migrations/001_estat_enum_usuaris.sql
--
-- Es idempotente: cada paso comprueba si ya está aplicado, así que ejecutarla dos veces no rompe nada.
-- Necesita permiso CREATE ROUTINE (usar root).

DROP PROCEDURE IF EXISTS migracio_001;

DELIMITER //
CREATE PROCEDURE migracio_001()
BEGIN
    DECLARE fk_duplicada VARCHAR(64);

    -- 1. actiu.estat: INT -> ENUM('EN_US','STOCK','REPARACIO','BAIXA')
    -- Los valores existentes vienen de la importación por colores:
    --   0 = activo -> EN_US · 1 = en revisión (amarillo) -> REPARACIO · 2 = baja (rojo) -> BAIXA
    IF (SELECT DATA_TYPE FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'actiu' AND COLUMN_NAME = 'estat') = 'int' THEN

        ALTER TABLE actiu ADD COLUMN estat_nou ENUM('EN_US','STOCK','REPARACIO','BAIXA') NULL AFTER estat;

        UPDATE actiu SET estat_nou = CASE estat
            WHEN 0 THEN 'EN_US'
            WHEN 1 THEN 'REPARACIO'
            WHEN 2 THEN 'BAIXA'
            ELSE NULL
        END;

        -- Los pinpads se importaron todos con 1 porque ya venían filtrados como "INSTAL": están en uso.
        UPDATE actiu a JOIN pinpad p ON p.id_actiu = a.id SET a.estat_nou = 'EN_US';

        ALTER TABLE actiu DROP COLUMN estat;
        ALTER TABLE actiu RENAME COLUMN estat_nou TO estat;
    END IF;

    -- 2. pinpad.id_ordinador tenía dos FKs (a ordinador y a actiu), restos de ddl-auto=update.
    -- Se conserva la que apunta a ordinador. El nombre se busca porque puede variar entre BBDD.
    SELECT CONSTRAINT_NAME INTO fk_duplicada
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pinpad'
      AND COLUMN_NAME = 'id_ordinador' AND REFERENCED_TABLE_NAME = 'actiu'
    LIMIT 1;

    IF fk_duplicada IS NOT NULL THEN
        SET @sql_fk = CONCAT('ALTER TABLE pinpad DROP FOREIGN KEY `', fk_duplicada, '`');
        PREPARE stmt FROM @sql_fk;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    -- 3. usuari: fuera teléfono (se usa Teams) y nuevos campos que ya pide el frontend.
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuari' AND COLUMN_NAME = 'telefon') THEN
        ALTER TABLE usuari DROP COLUMN telefon;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'usuari' AND COLUMN_NAME = 'email') THEN
        ALTER TABLE usuari
            ADD COLUMN email VARCHAR(100) NULL,
            ADD COLUMN departament VARCHAR(100) NULL,
            ADD COLUMN kensington VARCHAR(50) NULL,
            ADD COLUMN observacions VARCHAR(255) NULL,
            ADD COLUMN estat ENUM('ACTIU','BAIXA') NOT NULL DEFAULT 'ACTIU';
    END IF;
END //
DELIMITER ;

CALL migracio_001();
DROP PROCEDURE migracio_001;
