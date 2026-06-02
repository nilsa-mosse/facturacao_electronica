-- V4: Criar tabelas do módulo de Recursos Humanos
-- Tabelas: departamento, colaborador, subsidio, colaborador_subsidio,
--          folha_processamento, salario_processado, salario_processado_subsidio,
--          parametro_payroll

-- 1. Departamento (sem FK, apenas referencia empresa que já existe)
CREATE TABLE IF NOT EXISTS departamento (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    nome        VARCHAR(255) NOT NULL,
    descricao   VARCHAR(500),
    empresa_id  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_departamento_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

-- 2. Colaborador (depende de departamento e empresa)
CREATE TABLE IF NOT EXISTS colaborador (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nome            VARCHAR(255),
    nif             VARCHAR(20),
    email           VARCHAR(255),
    telefone        VARCHAR(20),
    endereco        VARCHAR(500),
    iban            VARCHAR(34),
    salario_base    DOUBLE       NOT NULL DEFAULT 0,
    data_admissao   DATE,
    cargo           VARCHAR(255),
    habilitacoes    VARCHAR(255),
    dependentes     INT          NOT NULL DEFAULT 0,
    tipo_contrato   VARCHAR(50),
    departamento_id BIGINT,
    empresa_id      BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_colaborador_departamento FOREIGN KEY (departamento_id) REFERENCES departamento (id),
    CONSTRAINT fk_colaborador_empresa      FOREIGN KEY (empresa_id)      REFERENCES empresa (id)
);

-- 3. Subsídio (depende de empresa)
CREATE TABLE IF NOT EXISTS subsidio (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    nome                VARCHAR(255) NOT NULL,
    codigo              VARCHAR(50),
    limite_isencao_inss DOUBLE       DEFAULT 0.0,
    limite_isencao_irt  DOUBLE       DEFAULT 0.0,
    sujeito_irt         BIT(1)       NOT NULL DEFAULT 1,
    sujeito_inss        BIT(1)       NOT NULL DEFAULT 1,
    empresa_id          BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_subsidio_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

-- 4. ColaboradorSubsidio – subsídios atribuídos a cada colaborador
CREATE TABLE IF NOT EXISTS colaborador_subsidio (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    colaborador_id BIGINT NOT NULL,
    subsidio_id    BIGINT NOT NULL,
    valor          DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_colsub_colaborador FOREIGN KEY (colaborador_id) REFERENCES colaborador (id),
    CONSTRAINT fk_colsub_subsidio    FOREIGN KEY (subsidio_id)    REFERENCES subsidio (id)
);

-- 5. FolhaProcessamento (depende de empresa)
CREATE TABLE IF NOT EXISTS folha_processamento (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    mes                 INT          NOT NULL,
    ano                 INT          NOT NULL,
    data_processamento  DATETIME,
    estado              VARCHAR(20),   -- RASCUNHO | PROCESSADO | PAGO
    empresa_id          BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT fk_folha_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);

-- 6. SalarioProcessado (depende de folha_processamento e colaborador)
CREATE TABLE IF NOT EXISTS salario_processado (
    id                              BIGINT NOT NULL AUTO_INCREMENT,
    folha_id                        BIGINT,
    colaborador_id                  BIGINT,
    salario_base                    DOUBLE NOT NULL DEFAULT 0,
    subsidio_ferias                 DOUBLE NOT NULL DEFAULT 0,
    subsidio_natal                  DOUBLE NOT NULL DEFAULT 0,
    rendimento_iliquido             DOUBLE NOT NULL DEFAULT 0,
    desconto_seguranca_social       DOUBLE NOT NULL DEFAULT 0,
    desconto_irt                    DOUBLE NOT NULL DEFAULT 0,
    encargo_empresa_seguranca_social DOUBLE NOT NULL DEFAULT 0,
    salario_liquido                 DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_salproc_folha       FOREIGN KEY (folha_id)       REFERENCES folha_processamento (id),
    CONSTRAINT fk_salproc_colaborador FOREIGN KEY (colaborador_id) REFERENCES colaborador (id)
);

-- 7. SalarioProcessadoSubsidio – snapshot dos subsídios por recibo
CREATE TABLE IF NOT EXISTS salario_processado_subsidio (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    salario_processado_id BIGINT NOT NULL,
    subsidio_id          BIGINT NOT NULL,
    valor                DOUBLE NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_spsubsidio_salario  FOREIGN KEY (salario_processado_id) REFERENCES salario_processado (id),
    CONSTRAINT fk_spsubsidio_subsidio FOREIGN KEY (subsidio_id)           REFERENCES subsidio (id)
);

-- 8. ParametroPayroll – configuração de payroll por empresa (1 registo por empresa)
CREATE TABLE IF NOT EXISTS parametro_payroll (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    empresa_id                  BIGINT NOT NULL UNIQUE,
    taxa_inss_trabalhador       DOUBLE NOT NULL DEFAULT 3.0,
    taxa_inss_empresa           DOUBLE NOT NULL DEFAULT 8.0,
    desconto_irt_dependente     DOUBLE NOT NULL DEFAULT 10.0,
    dias_padrao_processamento   INT    NOT NULL DEFAULT 30,
    PRIMARY KEY (id),
    CONSTRAINT fk_parametro_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id)
);
