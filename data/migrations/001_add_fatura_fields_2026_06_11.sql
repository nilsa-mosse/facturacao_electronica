-- Migration SQL para adicionar novos campos à tabela Fatura
-- Data: 2026-06-11

-- Adicionar novos campos à tabela se ela já existir
-- Este script assume que a tabela 'fatura' já foi criada

ALTER TABLE fatura ADD COLUMN data_vencimento DATETIME NULL COMMENT 'Data de vencimento da factura' AFTER data_emissao;

ALTER TABLE fatura ADD COLUMN valor_pago DECIMAL(19, 2) DEFAULT 0.00 COMMENT 'Valor total pago até ao momento' AFTER iva;

ALTER TABLE fatura ADD COLUMN valor_em_aberto DECIMAL(19, 2) DEFAULT 0.00 COMMENT 'Valor em aberto a receber' AFTER valor_pago;

ALTER TABLE fatura ADD COLUMN validada_agt BOOLEAN DEFAULT FALSE COMMENT 'Flag indicando que foi validada pela AGT e é imutável' AFTER tipo_documento;

ALTER TABLE fatura ADD COLUMN impresso BOOLEAN DEFAULT FALSE COMMENT 'Flag indicando que a factura foi impressa' AFTER validada_agt;

ALTER TABLE fatura ADD COLUMN data_impressao DATETIME NULL COMMENT 'Data e hora da impressão' AFTER impresso;

ALTER TABLE fatura ADD COLUMN data_email DATETIME NULL COMMENT 'Data e hora do último envio por email' AFTER data_impressao;

ALTER TABLE fatura ADD COLUMN email_enviado BOOLEAN DEFAULT FALSE COMMENT 'Flag indicando que foi enviada por email' AFTER data_email;

ALTER TABLE fatura ADD COLUMN fatura_referencia_id BIGINT NULL COMMENT 'Referência para NC ou ND' AFTER email_enviado;

ALTER TABLE fatura ADD FOREIGN KEY (fatura_referencia_id) REFERENCES fatura(id) ON DELETE SET NULL;

-- Criar índices para melhorar performance
CREATE INDEX idx_fatura_validada_agt ON fatura(validada_agt);
CREATE INDEX idx_fatura_status ON fatura(status);
CREATE INDEX idx_fatura_data_emissao ON fatura(data_emissao);
CREATE INDEX idx_fatura_empresa_id ON fatura(empresa_id);
CREATE INDEX idx_fatura_tipo_documento ON fatura(tipo_documento);

-- Atualizar facturas existentes que foram validadas na AGT
-- (adaptar a lógica conforme necessário para o seu sistema)
UPDATE fatura SET validada_agt = TRUE, valor_pago = total 
WHERE status = 'VALIDADA' OR status = 'VALIDADA_AGT';

-- Log da migração
INSERT INTO migration_log (migration_name, migration_date, description) 
VALUES ('add_fatura_fields_2026_06_11', NOW(), 'Adição de novos campos para operações e estados de facturas');
