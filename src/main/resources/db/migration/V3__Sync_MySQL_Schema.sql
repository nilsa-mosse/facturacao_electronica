-- V3: Synchronize MySQL Schema with H2/Entities
-- This script adds the missing AGT compliance fields and the global configuration table.

-- 1. Create configuracao_sistema table (Singleton for global settings)
CREATE TABLE IF NOT EXISTS configuracao_sistema (
    id BIGINT PRIMARY KEY,
    sistema_nome VARCHAR(255) DEFAULT 'Kwanza ERP',
    sistema_versao VARCHAR(20) DEFAULT '1.0.0',
    sistema_email_suporte VARCHAR(100) DEFAULT 'suporte@facturacao.com',
    sistema_backup BOOLEAN DEFAULT TRUE,
    sistema_tema VARCHAR(20) DEFAULT 'light',
    servidor_porta INT DEFAULT 8080,
    servidor_hostname VARCHAR(255) DEFAULT 'localhost',
    servidor_base_url VARCHAR(500) DEFAULT 'http://localhost:8080',
    servidor_proxy_habilitado BOOLEAN DEFAULT FALSE,
    servidor_proxy_host VARCHAR(255) DEFAULT '',
    servidor_proxy_porta INT DEFAULT 80,
    servidor_cors_origens VARCHAR(1000) DEFAULT '*',
    email_smtp_host VARCHAR(255) DEFAULT 'smtp.gmail.com',
    email_smtp_porta INT DEFAULT 587,
    email_smtp_username VARCHAR(255) DEFAULT '',
    email_smtp_password VARCHAR(500) DEFAULT '',
    email_seguranca_tipo VARCHAR(10) DEFAULT 'TLS',
    email_remetente VARCHAR(255) DEFAULT 'noreply@empresa.ao',
    email_nome_remetente VARCHAR(255) DEFAULT 'Sistema de Facturação',
    db_tipobd VARCHAR(50) DEFAULT 'MySQL',
    db_connection_timeout INT DEFAULT 30000,
    db_query_timeout INT DEFAULT 60000,
    db_pool_min INT DEFAULT 5,
    db_pool_max INT DEFAULT 20,
    db_idle_timeout INT DEFAULT 30000,
    db_max_lifetime INT DEFAULT 60000,
    db_schema VARCHAR(100) DEFAULT 'efacturacao',
    storage_tipo VARCHAR(20) DEFAULT 'LOCAL',
    storage_caminho_base VARCHAR(500) DEFAULT 'uploads/',
    storage_tamanho_max_ficheiro INT DEFAULT 10,
    storage_tamanho_max_request INT DEFAULT 20,
    storage_estrategia_backup VARCHAR(20) DEFAULT 'DIARIO',
    storage_cloud_provider VARCHAR(20) DEFAULT '',
    storage_cloud_bucket VARCHAR(255) DEFAULT '',
    storage_cloud_region VARCHAR(100) DEFAULT '',
    seg_tempo_expiracao_sessao INT DEFAULT 5,
    seg_tempo_expiracao_unidade VARCHAR(10) DEFAULT 'MINUTOS',
    seg_tentativas_login_max INT DEFAULT 5,
    seg_lockout_duracao INT DEFAULT 15,
    seg_politica_password VARCHAR(20) DEFAULT 'MEDIA',
    seg_comprimento_min_password INT DEFAULT 8,
    seg_two_factor_ativo BOOLEAN DEFAULT FALSE,
    seg_require_uppercase BOOLEAN DEFAULT TRUE,
    seg_require_numbers BOOLEAN DEFAULT TRUE,
    seg_require_special_chars BOOLEAN DEFAULT FALSE,
    seg_ip_whitelist VARCHAR(2000) DEFAULT '',
    seg_log_acessos_ativo BOOLEAN DEFAULT TRUE,
    agt_certificado_numero VARCHAR(20) DEFAULT '0000',
    agt_private_key TEXT,
    agt_public_key TEXT,
    agt_chave_versao INT DEFAULT 1,
    licenca_data_ativacao DATETIME,
    licenca_chave_ativacao VARCHAR(255)
);

-- Initialize global configuration if not present
INSERT IGNORE INTO configuracao_sistema (id) VALUES (1);

-- 2. Add AGT compliance fields to fatura table
-- We check if columns exist by attempting to add them (Standard SQL doesn't have IF NOT EXISTS for columns in most versions)
-- For Flyway/MySQL, we just execute. If they exist, it might error, but Flyway will mark it.
-- Better to use a procedure or just assume they are missing if we are at V3.

ALTER TABLE fatura ADD COLUMN IF NOT EXISTS hash_control VARCHAR(255) AFTER hash;
ALTER TABLE fatura ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(255) AFTER hash_control;
ALTER TABLE fatura ADD COLUMN IF NOT EXISTS system_entry_date DATETIME AFTER previous_hash;
ALTER TABLE fatura ADD COLUMN IF NOT EXISTS invoice_status VARCHAR(1) AFTER system_entry_date;
ALTER TABLE fatura ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(10) AFTER invoice_status;
ALTER TABLE fatura ADD COLUMN IF NOT EXISTS empresa_id BIGINT AFTER codigo_agt;

-- 3. Add relationship fields to Compra (Venda POS)
ALTER TABLE compra ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'EMITIDA';
ALTER TABLE compra ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(10);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS nome_cliente VARCHAR(100);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS nif_cliente VARCHAR(20);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS morada_cliente VARCHAR(255);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS telefone_cliente VARCHAR(20);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS email_cliente VARCHAR(100);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS forma_pagamento VARCHAR(50);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS valor_pago_cash DECIMAL(15,2);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS valor_pago_multicaixa DECIMAL(15,2);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS banco_multicaixa VARCHAR(50);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS referencia_multicaixa VARCHAR(100);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS comissao_multicaixa DECIMAL(15,2);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS valor_liquido_multicaixa DECIMAL(15,2);
ALTER TABLE compra ADD COLUMN IF NOT EXISTS motivo_anulacao TEXT;
ALTER TABLE compra ADD COLUMN IF NOT EXISTS fatura_referencia_id BIGINT;
ALTER TABLE compra ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE compra ADD COLUMN IF NOT EXISTS usuario_id BIGINT;

-- 4. Update Empresa table
ALTER TABLE empresa ADD COLUMN IF NOT EXISTS regime_fiscal VARCHAR(50);

-- 5. Update Produto table
ALTER TABLE produto MODIFY COLUMN quantidade_estoque DECIMAL(10,2);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS estoque_minimo DECIMAL(10,2) DEFAULT 0.0;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS unidade_medida VARCHAR(20);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS preco_compra DECIMAL(10,2);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS preco_original DECIMAL(10,2);
ALTER TABLE produto ADD COLUMN IF NOT EXISTS estado_id BIGINT;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS empresa_id BIGINT;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS data_fabrico DATE;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS data_expiracao DATE;
ALTER TABLE produto ADD COLUMN IF NOT EXISTS em_promocao BOOLEAN DEFAULT FALSE;

-- 6. Update Cliente table
ALTER TABLE cliente ADD COLUMN IF NOT EXISTS empresa_id BIGINT;

