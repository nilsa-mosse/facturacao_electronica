-- V2: Consolidated Seed Data for 35 Tables (High Volume)
-- Compatible with MySQL 5.7 and Flyway 7.15.0 (Java 8)

-- 1. Empresa
INSERT INTO empresa (nome, nif, endereco, telefone, email, website, regime_fiscal) VALUES
('Sua Empresa, Lda', '5412345678', 'Rua Principal, Luanda', '923000000', 'geral@empresa.ao', 'www.empresa.ao', 'Regime Geral');

-- 2. Configuração AGT
INSERT INTO configuracao_agt (url_api, token, modo, reenvio_automatico, tentativas_reenvio, nif_certificado) VALUES
('https://homologacao.agt.minfin.gov.ao/api/v1', 'TOKEN_EXEMPLO_SHA256_12345', 'HOMOLOGACAO', TRUE, 5, '5412345678');

-- 3. Regimes Fiscais
INSERT INTO regime_fiscal (nome, codigo, descricao, icone) VALUES 
('Regime Geral', 'GERAL', 'Regime Geral de Tributação', 'fa-globe'),
('Regime Simplificado', 'SIMPLIFICADO', 'Regime Simplificado', 'fa-bolt'),
('Regime de Exclusão', 'EXCLUSAO', 'Regime de Exclusão', 'fa-ban');

-- 4. Séries de Facturação
INSERT INTO serie (nome, ano, sequencia_atual, ativa) VALUES 
('S1', 2026, 10, TRUE), ('S2', 2026, 1, TRUE);

-- 5. Moedas
INSERT INTO moeda (codigo, simbolo, taxa_cambio) VALUES 
('AOA', 'Kz', 1.0000), ('USD', '$', 850.5000), ('EUR', '€', 920.3000);

-- 6. Estados de Produto
INSERT INTO estado (nome) VALUES ('DISPONÍVEL'), ('INDISPONÍVEL'), ('RESERVADO');

-- 7. Métodos de Pagamento
INSERT INTO metodo_pagamento (codigo, descricao) VALUES 
('NU', 'Numerário'), ('MC', 'Multicaixa'), ('TB', 'Transferência Bancária'), ('CC', 'Cartão de Crédito');

-- 8. Impostos
INSERT INTO imposto (nome, codigo, percentual, tipo) VALUES 
('IVA 14%', 'IVA', 14.00, 'IVA'), ('IVA 7%', 'IVA', 7.00, 'IVA'), ('ISE', 'ISE', 0.00, 'IVA'), ('IS 1%', 'IS', 1.00, 'IS');

-- 9. Taxas (AGT)
INSERT INTO taxa (valor, descricao, codigo_agt) VALUES 
(0.00, 'Isento conforme Artigo 12.º', 'ISE'), (14.00, 'Taxa Normal', 'IVA');

-- 10. Retenção
INSERT INTO retencao (codigo, taxa, descricao) VALUES 
('RET6.5', 6.50, 'Retenção na Fonte de 6.5%'), ('RET10', 10.00, 'Retenção na Fonte de 10%');

-- 11. Categorias
INSERT INTO categoria (nome) VALUES
('Informática'), ('Escritório'), ('Electrodomésticos'), ('Segurança'), ('Smartphones'), ('Energia'), ('Som'), ('Desporto');

-- 12. Clientes (IDs 1-5)
INSERT INTO cliente (nome, nif, email, telefone, endereco, regime_venda) VALUES
('Consumidor Final', '999999999', 'geral@empresa.com', '900000000', 'Luanda, Angola', 'REGAL'),
('João Silva', '500000001', 'joao.silva@email.com', '923000001', 'Talatona, Luanda', 'RESIM'),
('Multiservice, SA', '5400012345', 'comercial@multiservice.ao', '931000555', 'ZEE Viana', 'REGAL'),
('Maria Antónia', '1234567890', 'maria.ant@gmail.com', '912000111', 'Kilamba', 'RESIM'),
('Construções Angola, Lda', '5012223334', 'procurement@constang.ao', '944005006', 'Benguela, Angola', 'REGAL');

-- 13. Produtos (IDs 1-36)
INSERT INTO produto (nome, descricao, preco, quantidade_estoque, estoque_minimo, codigo_barra, iva_percentual, categoria_id, estado_id) VALUES
('Notebook Dell', 'Core i7, 16GB RAM', 350000.00, 10, 2, 'INF0001', 14.00, 1, 1),
('Mouse Logitech', 'Bluetooth 5.0', 5000.00, 50, 5, 'INF0002', 14.00, 1, 1),
('iPhone 15', '128GB Black', 750000.00, 5, 1, 'PHO0001', 14.00, 5, 1),
('Impressora HP Laser', 'Multifuncional Color', 120000.00, 15, 3, 'INF0005', 14.00, 1, 1),
('Tablet Samsung Tab S9', '11 pol, 256GB', 180000.00, 8, 2, 'PHO0002', 14.00, 5, 1),
('Bateria Solar 200Ah', 'Gel Ciclo Profundo', 145000.00, 20, 5, 'ENE0005', 14.00, 6, 1),
('Monitor Samsung 24', 'LED Full HD 75Hz', 85000.00, 15, 3, 'INF0010', 14.00, 1, 1),
('Teclado Mecânico Razer', 'RGB Switch Yellow', 45000.00, 10, 2, 'INF0011', 14.00, 1, 1),
('Câmera IP Hikvision', '4MP Dome Exir', 28000.00, 30, 5, 'SEG0001', 14.00, 4, 1),
('DVR Intelbras 16CH', 'Multi-HD 1080p', 75000.00, 5, 1, 'SEG0002', 14.00, 4, 1),
('Ar Condicionado 12000', 'Inverter Split', 220000.00, 12, 2, 'ELE0001', 14.00, 3, 1),
('Microondas LG 30L', 'EasyClean Silver', 65000.00, 18, 3, 'ELE0002', 14.00, 3, 1),
('Frigorífico Samsung', 'Twin Cooling 400L', 450000.00, 4, 1, 'ELE0003', 14.00, 3, 1),
('Switch Cisco 24P', 'Managed Layer 2', 185000.00, 6, 1, 'INF0020', 14.00, 1, 1),
('Router Mikrotik', 'RB4011 Wireless', 95000.00, 10, 2, 'INF0021', 14.00, 1, 1),
('Cabo Rede Cat6 305m', 'Cobre Puro Furukawa', 85000.00, 25, 5, 'INF0022', 14.00, 1, 1),
('Projector Epson X51', '3800 Lumens HDMI', 280000.00, 4, 1, 'IMS0010', 14.00, 7, 1),
('Soundbar JBL SB170', '2.1 Canais 220W', 145000.00, 8, 2, 'IMS0011', 14.00, 7, 1),
('PS5 DualSense', 'Comando Wireless White', 45000.00, 25, 5, 'JOG0010', 14.00, 8, 1),
('SSD 1TB Kingston', 'NVMe M.2 Gen4', 55000.00, 40, 10, 'INF0030', 14.00, 1, 1),
('RAM 16GB Corsair', 'Vengeance LPX DDR4', 42000.00, 30, 5, 'INF0031', 14.00, 1, 1),
('Papel A4 Navigator', 'Carga 5 Resmas', 18500.00, 100, 20, 'ESC0001', 14.00, 2, 1),
('Cadeira Gaming DX', 'Ergonómica Black/Red', 125000.00, 5, 2, 'JOG0020', 14.00, 8, 1),
('Mesa Escritório L', 'MDF 160x140cm', 85000.00, 10, 2, 'ESC0010', 14.00, 2, 1),
('Extintor Pó 6Kg', 'ABC Homologado', 22000.00, 20, 5, 'SEG0010', 14.00, 4, 1),
('Sensor Movimento', 'S/Fio Anti-Pets', 12500.00, 50, 10, 'SEG0020', 14.00, 4, 1),
('Smartphone Xiaomi 13', '256GB Global', 285000.00, 12, 3, 'PHO0010', 14.00, 5, 1),
('Galaxy Watch 6', '44mm Bluetooth', 165000.00, 6, 2, 'PHO0020', 14.00, 5, 1),
('Powerbank 20000mAh', 'Fast Charge 22.5W', 18500.00, 45, 10, 'PHO0030', 14.00, 5, 1),
('Inversor Victron 3kVA', 'MultiPlus II 48V', 985000.00, 2, 1, 'ENE0010', 14.00, 6, 1),
('Painel Solar 550W', 'Monocristalino Jinko', 85000.00, 40, 10, 'ENE0011', 14.00, 6, 1),
('Bateria Lítio 5kWh', 'Pylontech US3000C', 1250000.00, 4, 1, 'ENE0012', 14.00, 6, 1),
('Máquina Café Delta', 'Qool Evolution', 45000.00, 15, 3, 'ELE0010', 14.00, 3, 1),
('Aspirador Robot', 'Xiaomi S10 Plus', 185000.00, 6, 2, 'ELE0020', 14.00, 3, 1),
('TV Box Android 4K', 'Mi Box S 2nd Gen', 42000.00, 20, 5, 'IMS0020', 14.00, 7, 1),
('Auriculares Sony XM5', 'Noise Cancelling', 215000.00, 5, 2, 'IMS0030', 14.00, 7, 1);

-- 14. Estabelecimentos (IDs 1-10)
INSERT INTO estabelecimento (nome, endereco, telefone, tipo, visivel) VALUES
('Sede Talatona', 'Via AL7, Luanda', '923000100', 'LOJA', TRUE),
('Depósito Viana', 'Polo Industrial', '923000200', 'ARMAZEM', TRUE),
('Loja Cacuaco', 'Vila de Cacuaco', '923000300', 'LOJA', TRUE),
('Shopping Fortaleza', 'Marginal de Luanda', '923000400', 'LOJA', TRUE),
('Filial Huambo', 'Av. Independência', '923000500', 'LOJA', TRUE),
('Armazém Lubango', 'Zona Arimba', '923000600', 'ARMAZEM', TRUE),
('Posto Namibe', 'Edifício Marginal', '923000700', 'LOJA', TRUE),
('Base Soyo', 'Base Kwanda', '900111222', 'ARMAZEM', TRUE),
('Loja Cabinda', 'Rua do Porto', '923111222', 'LOJA', TRUE),
('Entreposto Malanje', 'Bairro Canâmbua', '924111333', 'ARMAZEM', TRUE);

-- 15. Estoque
INSERT INTO estoque (produto_id, estabelecimento_id, quantidade, updated_at) VALUES 
(1, 1, 5, NOW()), (1, 2, 5, NOW()), (2, 1, 30, NOW()), (2, 2, 20, NOW()), 
(3, 1, 3, NOW()), (6, 2, 20, NOW()), (30, 8, 2, NOW());

-- 16. Movimentos de Stock
INSERT INTO movimento_stock (produto_id, tipo, quantidade, data_movimento, motivo, documento_referencia) VALUES
(1, 'ENTRADA', 10.00, NOW(), 'Stock Abertura', 'INI-001'),
(2, 'ENTRADA', 50.00, NOW(), 'Stock Abertura', 'INI-001');

INSERT INTO movimento_stock (produto_id, tipo, quantidade, data_movimento, motivo, documento_referencia) VALUES
(7, 'ENTRADA', 15.00, NOW(), 'Nova Remessa', 'GUI-880'),
(20, 'SAIDA', 5.00, NOW(), 'Transf Provincial', 'TRF-01');

-- 17. Compra (Venda POS)
INSERT INTO compra (data_compra, total, cliente_id, status, tipo_documento, nome_cliente, nif_cliente, forma_pagamento) VALUES
(NOW(), 355000.00, 2, 'EMITIDA', 'FT', 'João Silva', '500000001', 'Multicaixa');

INSERT INTO item_compra (compra_id, produto_id, quantidade, preco, subtotal, taxa_iva) VALUES
(1, 1, 1, 350000.00, 350000.00, 14.00), (1, 2, 1, 5000.00, 5000.00, 14.00);

-- 18. Fatura
INSERT INTO fatura (venda_id, numero_fatura, data_emissao, enviada_agt, total, iva, hash, codigo_agt) VALUES
(1, 'FT-2026-001', NOW(), TRUE, 355000.00, 49700.00, 'HASH_XYZ', 'AGT-ABC');

INSERT INTO item_factura (factura_id, produto, quantidade, preco) VALUES
(1, 'Notebook Dell', 1, 350000.00), (1, 'Mouse Logitech', 1, 5000.00);

-- 19. Logística
INSERT INTO guia_remessa (numero_guia, data_emissao, cliente_id, fatura_origem_id, status) VALUES
('GR-2026-001', NOW(), 2, 1, 'ATIVA');

INSERT INTO evento_tracking (guia_remessa_id, data_evento, descricao, status) VALUES
(1, NOW(), 'Carga Pronta', 'PREPARADO');

-- 20. Rectificações
INSERT INTO nota_credito (numero_nota, data_emissao, cliente_id, fatura_origem_id, total) VALUES
('NC-2026-001', NOW(), 2, 1, 10000.00);

INSERT INTO item_nota_credito (nota_credito_id, produto_id, quantidade, preco) VALUES (1, 1, 1, 10000.00);

INSERT INTO devolucao (venda_id, data_devolucao, motivo, total_devolvido) VALUES (1, NOW(), 'Defeito', 5000.00);

-- 21. Financeiro
INSERT INTO despesa (descricao, valor, data_despesa, categoria, metodo_pagamento_id) VALUES
('Renda Junho', 150000.00, NOW(), 'Imóveis', 3);

-- 22. Usuários
INSERT INTO usuario (login, senha, nome, role, ativo) VALUES
('admin', '$2a$10$8.73pUuYZL7H9p.5A8699OTX8m6l/n9d5T9Y/S.8O./.S./.S./.S', 'Administrador', 'ADMIN', TRUE),
('caixa1', '$2a$10$8.73pUuYZL7H9p.5A8699OTX8m6l/n9d5T9Y/S.8O./.S./.S./.S', 'Operador', 'USER', TRUE);

INSERT INTO usuario_estabelecimento (usuario_id, estabelecimento_id) VALUES (1, 1), (1, 2), (2, 1);

-- 23. Legado
INSERT INTO venda (cliente_id, total, data_venda) VALUES (2, 355000.00, NOW());
INSERT INTO item_carrinho (venda_id, produto_id, quantidade, preco_total) VALUES (1, 1, 1, 350000.00);
INSERT INTO clients (name, email) VALUES ('John Legacy', 'john@legacy.com');

-- 24. Sistema
INSERT INTO sistema (nome, versao, tema) VALUES ('eFacturacao Pro', '2.0.1', 'dark');

-- 25. Integração
INSERT INTO integracao (api_agt, token_agt) VALUES ('https://agt.minfin.gov.ao/api', 'TOKEN_XYZ');
