-- V5: Add email and forcar_alteracao_senha fields to usuario table
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email VARCHAR(255) NULL;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS forcar_alteracao_senha TINYINT(1) NOT NULL DEFAULT 0;
