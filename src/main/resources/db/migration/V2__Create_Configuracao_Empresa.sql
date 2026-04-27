-- Migration para adicionar tabela de ConfiguracaoEmpresa
-- Este arquivo deve ser colocado em src/main/resources/db/migration/

-- Criar tabela de Configuração por Empresa
CREATE TABLE configuracao_empresa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    empresa_id BIGINT NOT NULL UNIQUE,
    
    -- Email/SMTP
    email_smtp_host VARCHAR(255) DEFAULT 'smtp.gmail.com',
    email_smtp_porta INT DEFAULT 587,
    email_smtp_username VARCHAR(255),
    email_smtp_password VARCHAR(500),
    email_seguranca_tipo VARCHAR(10) DEFAULT 'TLS',
    email_remetente VARCHAR(255),
    email_nome_remetente VARCHAR(255),
    email_habilitado BOOLEAN DEFAULT FALSE,
    
    -- Storage
    storage_tipo VARCHAR(20) DEFAULT 'LOCAL',
    storage_caminho_base VARCHAR(500) DEFAULT 'uploads/',
    storage_tamanho_max_ficheiro INT DEFAULT 10,
    storage_tamanho_max_request INT DEFAULT 20,
    storage_estrategia_backup VARCHAR(20) DEFAULT 'DIARIO',
    storage_cloud_provider VARCHAR(20),
    storage_cloud_bucket VARCHAR(255),
    storage_cloud_region VARCHAR(100),
    storage_backup_habilitado BOOLEAN DEFAULT TRUE,
    
    -- Segurança
    seg_tempo_expiracao_sessao INT DEFAULT 30,
    seg_two_factor_ativo BOOLEAN DEFAULT FALSE,
    seg_require_uppercase BOOLEAN DEFAULT TRUE,
    seg_require_numbers BOOLEAN DEFAULT TRUE,
    seg_require_special_chars BOOLEAN DEFAULT FALSE,
    seg_comprimento_min_password INT DEFAULT 8,
    seg_ip_whitelist LONGTEXT,
    seg_log_acessos_ativo BOOLEAN DEFAULT TRUE,
    
    -- Notificações
    notificacao_email_habilitada BOOLEAN DEFAULT TRUE,
    notificacao_sms_habilitada BOOLEAN DEFAULT FALSE,
    notificacao_sms_provider VARCHAR(20),
    notificacao_sms_api_key VARCHAR(255),
    
    -- AGT (Autoridade Geral Tributária)
    agt_integracao_habilitada BOOLEAN DEFAULT FALSE,
    agt_url_servico VARCHAR(500),
    agt_usuario VARCHAR(255),
    agt_senha VARCHAR(500),
    agt_certificado VARCHAR(50),
    
    -- Preferências
    usar_logotipo_em_documentos BOOLEAN DEFAULT TRUE,
    usar_cabecalho_personalizado_em_documentos BOOLEAN DEFAULT TRUE,
    usar_rodape_personalizado_em_documentos BOOLEAN DEFAULT FALSE,
    rodape_personalizado LONGTEXT,
    
    -- Constraint
    CONSTRAINT fk_configuracao_empresa 
        FOREIGN KEY (empresa_id) 
        REFERENCES empresa(id) 
        ON DELETE CASCADE
);

-- Criar índices
CREATE INDEX idx_configuracao_empresa_empresa_id ON configuracao_empresa(empresa_id);

-- Adicionar coluna de relacionamento à tabela empresa (opcional, se usar fetch eager)
-- ALTER TABLE empresa ADD COLUMN configuracao_id BIGINT;
-- ALTER TABLE empresa ADD CONSTRAINT fk_empresa_configuracao 
--     FOREIGN KEY (configuracao_id) REFERENCES configuracao_empresa(id);
