-- V1: Full Initial Schema Definition for eFacturacao

-- ==========================================================
-- 1. Tabelas de Configuração e Globais
-- ==========================================================

CREATE TABLE IF NOT EXISTS empresa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    nif VARCHAR(20) NOT NULL UNIQUE,
    endereco TEXT,
    telefone VARCHAR(50),
    email VARCHAR(100),
    website VARCHAR(100),
    logotipo VARCHAR(255),
    regime_fiscal VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS configuracao_agt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url_api VARCHAR(255) NOT NULL,
    token TEXT NOT NULL,
    modo VARCHAR(20) DEFAULT 'HOMOLOGACAO',
    reenvio_automatico BOOLEAN DEFAULT FALSE,
    tentativas_reenvio INT DEFAULT 3,
    nif_certificado VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS regime_fiscal (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    descricao VARCHAR(100),
    icone VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS serie (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(20) NOT NULL UNIQUE,
    ano INT,
    sequencia_atual INT DEFAULT 1,
    ativa BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS moeda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL UNIQUE, -- AOA, USD, EUR
    simbolo VARCHAR(5),
    taxa_cambio DECIMAL(15,4)
);

CREATE TABLE IF NOT EXISTS estado (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS metodo_pagamento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    descricao VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS imposto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(50),
    codigo VARCHAR(10),
    percentual DECIMAL(5,2),
    tipo VARCHAR(20) -- IVA, IS, etc.
);

CREATE TABLE IF NOT EXISTS taxa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    valor DECIMAL(10,2),
    descricao VARCHAR(100),
    codigo_agt VARCHAR(10)
);

-- ==========================================================
-- 2. Promoção, CRM e Core Logística
-- ==========================================================

CREATE TABLE IF NOT EXISTS categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    nif VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    telefone VARCHAR(20),
    endereco TEXT,
    regime_venda VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS produto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao TEXT,
    preco DECIMAL(15,2) NOT NULL,
    quantidade_estoque DECIMAL(10,2) DEFAULT 0,
    estoque_minimo DECIMAL(10,2) DEFAULT 0,
    imagem VARCHAR(255),
    imagem_blob LONGBLOB,
    codigo_barra VARCHAR(50),
    iva_percentual DECIMAL(5,2),
    categoria_id BIGINT,
    estado_id BIGINT,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    FOREIGN KEY (estado_id) REFERENCES estado(id)
);

CREATE TABLE IF NOT EXISTS estabelecimento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    endereco TEXT,
    telefone VARCHAR(50),
    tipo VARCHAR(50), -- LOJA, ARMAZEM
    visivel BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS estoque (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT,
    estabelecimento_id BIGINT,
    quantidade DECIMAL(10,2) DEFAULT 0,
    updated_at DATETIME,
    FOREIGN KEY (produto_id) REFERENCES produto(id),
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento(id),
    UNIQUE uk_estoque_produto_estabel (produto_id, estabelecimento_id)
);

CREATE TABLE IF NOT EXISTS movimento_stock (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT,
    tipo VARCHAR(20), -- ENTRADA, SAIDA, AJUSTE
    quantidade DECIMAL(10,2),
    data_movimento DATETIME,
    motivo VARCHAR(255),
    documento_referencia VARCHAR(100),
    origem VARCHAR(100),
    preco_custo DECIMAL(15,2),
    documento_blob LONGBLOB,
    nome_documento VARCHAR(100),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

-- ==========================================================
-- 3. Documentação Comercial (Vendas / Compras / Facturação)
-- ==========================================================

-- A entidade Compra no projeto está sendo usada como "Venda" no POS.
CREATE TABLE IF NOT EXISTS compra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_compra DATETIME NOT NULL,
    total DECIMAL(15,2) NOT NULL,
    cliente_id BIGINT,
    status VARCHAR(20) DEFAULT 'EMITIDA',
    tipo_documento VARCHAR(10), -- FT, FR, FP
    nome_cliente VARCHAR(100),
    nif_cliente VARCHAR(20),
    forma_pagamento VARCHAR(50),
    valor_pago_cash DECIMAL(15,2),
    valor_pago_multicaixa DECIMAL(15,2),
    banco_multicaixa VARCHAR(50),
    referencia_multicaixa VARCHAR(100),
    comissao_multicaixa DECIMAL(15,2),
    valor_liquido_multicaixa DECIMAL(15,2),
    motivo_anulacao TEXT,
    fatura_referencia_id BIGINT,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    FOREIGN KEY (fatura_referencia_id) REFERENCES compra(id)
);

CREATE TABLE IF NOT EXISTS item_compra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    compra_id BIGINT,
    produto_id BIGINT,
    quantidade DECIMAL(10,2) NOT NULL,
    preco DECIMAL(15,2) NOT NULL,
    subtotal DECIMAL(15,2),
    taxa_iva DECIMAL(5,2),
    FOREIGN KEY (compra_id) REFERENCES compra(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE IF NOT EXISTS fatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id BIGINT,
    numero_fatura VARCHAR(50) NOT NULL UNIQUE,
    data_emissao DATETIME NOT NULL,
    enviada_agt BOOLEAN DEFAULT FALSE,
    total DECIMAL(15,2),
    iva DECIMAL(15,2),
    hash VARCHAR(255),
    codigo_agt VARCHAR(100),
    FOREIGN KEY (venda_id) REFERENCES compra(id)
);

CREATE TABLE IF NOT EXISTS item_factura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    factura_id BIGINT,
    produto VARCHAR(255) NOT NULL,
    quantidade DECIMAL(10,2) NOT NULL,
    preco DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (factura_id) REFERENCES fatura(id)
);

CREATE TABLE IF NOT EXISTS guia_remessa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_guia VARCHAR(50) UNIQUE,
    data_emissao DATETIME,
    cliente_id BIGINT,
    local_carga VARCHAR(255),
    local_descarga VARCHAR(255),
    matricula_viatura VARCHAR(50),
    motorista VARCHAR(100),
    fatura_origem_id BIGINT,
    status VARCHAR(20) DEFAULT 'ATIVA',
    motivo_anulacao TEXT,
    guia_referencia_id BIGINT,
    tracking_status VARCHAR(50),
    hash_agt VARCHAR(255),
    codigo_validacao VARCHAR(100),
    data_validacao_agt DATETIME,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    FOREIGN KEY (fatura_origem_id) REFERENCES compra(id),
    FOREIGN KEY (guia_referencia_id) REFERENCES guia_remessa(id)
);

CREATE TABLE IF NOT EXISTS item_guia_remessa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guia_remessa_id BIGINT,
    produto_id BIGINT,
    nome_produto VARCHAR(255),
    quantidade DECIMAL(10,2),
    preco DECIMAL(15,2),
    taxa_iva DECIMAL(5,2),
    subtotal DECIMAL(15,2),
    FOREIGN KEY (guia_remessa_id) REFERENCES guia_remessa(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE IF NOT EXISTS evento_tracking (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    guia_remessa_id BIGINT,
    data_evento DATETIME,
    descricao TEXT,
    localizacao VARCHAR(255),
    status VARCHAR(50),
    FOREIGN KEY (guia_remessa_id) REFERENCES guia_remessa(id)
);

CREATE TABLE IF NOT EXISTS nota_credito (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    numero_nota VARCHAR(50) UNIQUE,
    data_emissao DATETIME,
    cliente_id BIGINT,
    fatura_origem_id BIGINT,
    motivo_retificacao TEXT,
    total DECIMAL(15,2),
    hash_agt VARCHAR(255),
    FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    FOREIGN KEY (fatura_origem_id) REFERENCES compra(id)
);

CREATE TABLE IF NOT EXISTS item_nota_credito (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nota_credito_id BIGINT,
    produto_id BIGINT,
    quantidade DECIMAL(10,2),
    preco DECIMAL(15,2),
    FOREIGN KEY (nota_credito_id) REFERENCES nota_credito(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE IF NOT EXISTS devolucao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id BIGINT,
    data_devolucao DATETIME,
    motivo TEXT,
    total_devolvido DECIMAL(15,2),
    FOREIGN KEY (venda_id) REFERENCES compra(id)
);

CREATE TABLE IF NOT EXISTS item_devolucao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    devolucao_id BIGINT,
    produto_id BIGINT,
    quantidade DECIMAL(10,2),
    preco_unitario DECIMAL(15,2),
    FOREIGN KEY (devolucao_id) REFERENCES devolucao(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE IF NOT EXISTS despesa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(255),
    valor DECIMAL(15,2),
    data_despesa DATETIME,
    categoria VARCHAR(50),
    metodo_pagamento_id BIGINT,
    FOREIGN KEY (metodo_pagamento_id) REFERENCES metodo_pagamento(id)
);

CREATE TABLE IF NOT EXISTS retencao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(20),
    taxa DECIMAL(5,2),
    descricao VARCHAR(100)
);

-- ==========================================================
-- 4. Segurança e Acessos
-- ==========================================================

CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    telefone VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE
);

-- ==========================================================
-- 5. Tabelas Legadas / Compatibilidade (db.sql)
-- ==========================================================

CREATE TABLE IF NOT EXISTS venda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    total DECIMAL(15,2) NOT NULL,
    data_venda DATETIME NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

CREATE TABLE IF NOT EXISTS item_carrinho (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id BIGINT,
    produto_id BIGINT,
    quantidade DECIMAL(10,2) NOT NULL,
    preco_total DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (venda_id) REFERENCES venda(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE IF NOT EXISTS clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT
);

-- ==========================================================
-- 6. Tabelas de Configuração Global (Sistema e Integração)
-- ==========================================================

CREATE TABLE IF NOT EXISTS sistema (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100),
    versao VARCHAR(20),
    email_suporte VARCHAR(100),
    backup BOOLEAN DEFAULT TRUE,
    tema VARCHAR(20) DEFAULT 'light'
);

CREATE TABLE IF NOT EXISTS integracao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_agt VARCHAR(255),
    token_agt TEXT
);

CREATE TABLE IF NOT EXISTS usuario_estabelecimento (
    usuario_id BIGINT NOT NULL,
    estabelecimento_id BIGINT NOT NULL,
    PRIMARY KEY (usuario_id, estabelecimento_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento(id)
);
