-- =====================================================================================
-- SCRIPT DDL: Associação das Tabelas Produto, Estabelecimento e Estoque
-- =====================================================================================
-- Este script assume que as tabelas 'produto', 'estabelecimento' e 'estoque' já existem.
-- Ele focará em criar os relacionamentos (Foreign Keys) para garantir a integridade.

-- 1. Criação das Chaves Estrangeiras (Foreign Keys) na tabela 'estoque'
-- Vincula cada registro de estoque a um produto e a um estabelecimento específico.

-- Adiciona a restrição que liga 'estoque' -> 'produto'
ALTER TABLE estoque
ADD CONSTRAINT fk_estoque_produto
FOREIGN KEY (produto_id) REFERENCES produto(id)
ON DELETE CASCADE
ON UPDATE CASCADE;

-- Adiciona a restrição que liga 'estoque' -> 'estabelecimento'
ALTER TABLE estoque
ADD CONSTRAINT fk_estoque_estabelecimento
FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento(id)
ON DELETE CASCADE
ON UPDATE CASCADE;

-- 2. Garantia de Unicidade (Opcional, mas Altamente Recomendado)
-- Um produto não deveria ter mais de uma linha de estoque no MESMO estabelecimento.
-- A restrição UNIQUE evita a duplicidade de produtos no mesmo local.
ALTER TABLE estoque
ADD CONSTRAINT uk_estoque_produto_estabel 
UNIQUE (produto_id, estabelecimento_id);


-- =====================================================================================
-- ASSOCIAÇÕES EXTRAS (MOVIMENTOS DE STOCK)
-- =====================================================================================

-- Caso a tabela 'movimento_stock' ainda não tenha sua associação explícita (Foreign Key)
-- Podemos garantir que seu relacionamento com Produto está íntegro também.
ALTER TABLE movimento_stock
ADD CONSTRAINT fk_movimento_produto
FOREIGN KEY (produto_id) REFERENCES produto(id)
ON DELETE CASCADE
ON UPDATE CASCADE;

-- Se no futuro desejar que o "Movimento de Stock" também saiba em qual Estabelecimento ocorreu
-- (Atualmente mapeado só com produto no JPA, criar esta associação ajuda muito no histórico por loja/armazém)
/*
ALTER TABLE movimento_stock ADD COLUMN estabelecimento_id BIGINT;

ALTER TABLE movimento_stock
ADD CONSTRAINT fk_movimento_estabelecimento
FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento(id)
ON DELETE CASCADE
ON UPDATE CASCADE;
*/
