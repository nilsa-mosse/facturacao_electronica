-- ==============================================================================
-- Script: Relacionar a Tabela Produto com a Tabela Estado
-- ==============================================================================

-- 1. Cria a coluna que servirá de Chave Estrangeira (Foreign Key). 
-- O tipo (BIGINT) deve ser igual ao tipo do ID da tabela 'estado'.
ALTER TABLE produto ADD COLUMN estado_id BIGINT;

-- 2. Constrói a relação rigorosa entre as duas tabelas no Banco de Dados
ALTER TABLE produto 
ADD CONSTRAINT fk_produto_estado 
FOREIGN KEY (estado_id) REFERENCES estado(id);

-- ==============================================================================
-- 3. Migração Inteligente de Dados Antigos
-- Se a tabela produto já tiver dados gravados na antiga coluna de texto 'estado', 
-- este comando migra e vincula os IDs magicamente, sem estragar os registos.
-- ==============================================================================
UPDATE produto p
JOIN estado e ON e.nome = p.estado
SET p.estado_id = e.id
WHERE p.estado IS NOT NULL AND p.estado_id IS NULL;

-- ==============================================================================
-- 4. Remoção da coluna antiga de estado textual
-- Removemos fisicamente a antiga coluna 'estado' da tabela produto, uma vez que
-- o sistema Java já foi atualizado para usar a relação via estado_id.
-- ==============================================================================
ALTER TABLE produto DROP COLUMN estado;
