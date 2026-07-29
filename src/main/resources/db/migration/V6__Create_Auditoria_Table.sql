-- Migration V6: Criar tabela de auditoria completa do sistema
CREATE TABLE IF NOT EXISTS auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_hora DATETIME NOT NULL,
    usuario_id BIGINT NULL,
    nome_usuario VARCHAR(255) NULL,
    email_usuario VARCHAR(255) NULL,
    empresa_id BIGINT NULL,
    ip_cliente VARCHAR(50) NULL,
    nome_computador VARCHAR(255) NULL,
    acao VARCHAR(100) NOT NULL,
    entidade VARCHAR(100) NOT NULL,
    entidade_id VARCHAR(100) NULL,
    valor_anterior TEXT NULL,
    valor_novo TEXT NULL,
    detalhes TEXT NULL,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES user(id) ON DELETE SET NULL
);
