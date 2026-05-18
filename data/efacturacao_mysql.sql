-- Script de Base de Dados Convertido de H2 para MySQL
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = NO_AUTO_VALUE_ON_ZERO;
SET NAMES utf8mb4;

CREATE TABLE caixa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    data_abertura DATETIME,
    data_fecho DATETIME,
    estado VARCHAR(255),
    observacoes VARCHAR(255),
    quebra_caixa DOUBLE,
    saldo_final DOUBLE,
    saldo_inicial DOUBLE,
    total_faturado DOUBLE,
    total_multicaixa DOUBLE,
    total_numerario DOUBLE,
    empresa_id BIGINT NOT NULL,
    estabelecimento_id BIGINT,
    operador_id BIGINT NOT NULL
);               
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.caixa;   
INSERT INTO caixa VALUES
(1, '2026-05-15 09:48:44.027', NULL, 'ABERTO','' , 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 82, 1, 1);    
CREATE TABLE categoria(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    nome VARCHAR(255),
    empresa_id BIGINT
);           
-- 11 +/- SELECT COUNT(*) FROM PUBLIC.categoria;              
INSERT INTO categoria VALUES
(15, 'Hospedagens Web', 1),
(16, 'Manutencao & Reparacao', 1),
(17, 'Mecanica', 1),
(18, 'Electrecidade', 1),
(19, 'Automacao', 1),
(20, 'Informática', 82),
(21, 'Automacao Industrial', 82),
(22, 'Frio', 82),
(23, 'Mecânica Auto', 82),
(24, 'Carros', 82),
(25, 'Softwares', 82);         
CREATE TABLE cliente(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    email VARCHAR(255),
    endereco VARCHAR(255),
    nif VARCHAR(255),
    nome VARCHAR(255),
    telefone VARCHAR(255),
    empresa_id BIGINT
);          
-- 3 +/- SELECT COUNT(*) FROM PUBLIC.cliente; 
INSERT INTO cliente VALUES
(1, NULL, NULL, 999999999, 'Consumidor Final', NULL, 1),
(2, 'sebastiao.mosse@grupoprodusol.com', 'Centralidade do Kilamba, quarteirão K', '003057262CA034', 'Sebastiao Vunda Mosse', '934068121', 82),
(3,'' ,'' , '003057262CA034', 'Sebastiao Vunda Mosse', '', 82); 
CREATE TABLE compra(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    banco_multicaixa VARCHAR(255),
    comissao_multicaixa DOUBLE,
    data_compra DATETIME,
    email_cliente VARCHAR(255),
    forma_pagamento VARCHAR(255),
    morada_cliente VARCHAR(255),
    motivo_anulacao VARCHAR(255),
    nif_cliente VARCHAR(255),
    nome_cliente VARCHAR(255),
    referencia_multicaixa VARCHAR(255),
    status VARCHAR(255),
    telefone_cliente VARCHAR(255),
    tipo_documento VARCHAR(255),
    total DOUBLE,
    valor_liquido_multicaixa DOUBLE,
    valor_pago_cash DOUBLE,
    valor_pago_multicaixa DOUBLE,
    cliente_id BIGINT,
    empresa_id BIGINT,
    fatura_referencia_id BIGINT,
    usuario_id BIGINT
);                  
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.compra;  
CREATE TABLE configuracao_agt(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    modo VARCHAR(255),
    nif_certificado VARCHAR(255),
    reenvio_automatico BOOLEAN NOT NULL,
    tentativas_reenvio INTEGER,
    token VARCHAR(255) NOT NULL,
    url_api VARCHAR(255) NOT NULL,
    data_ultimo_envio DATE,
    documentos_enviados_hoje INTEGER,
    envio_agt_ativo BOOLEAN NOT NULL,
    limite_documentos_diarios INTEGER
);                  
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.configuracao_agt;        
CREATE TABLE configuracao_empresa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    agt_certificado VARCHAR(50),
    agt_integracao_habilitada BOOLEAN NOT NULL,
    agt_senha VARCHAR(500),
    agt_url_servico VARCHAR(500),
    agt_usuario VARCHAR(255),
    email_habilitado BOOLEAN NOT NULL,
    email_nome_remetente VARCHAR(255),
    email_remetente VARCHAR(255),
    email_seguranca_tipo VARCHAR(10),
    email_smtp_host VARCHAR(255),
    email_smtp_password VARCHAR(500),
    email_smtp_porta INTEGER NOT NULL,
    email_smtp_username VARCHAR(255),
    notificacao_email_habilitada BOOLEAN NOT NULL,
    notificacao_sms_api_key VARCHAR(255),
    notificacao_sms_habilitada BOOLEAN NOT NULL,
    notificacao_sms_provider VARCHAR(20),
    rodape_personalizado VARCHAR(1000),
    seg_comprimento_min_password INTEGER NOT NULL,
    seg_ip_whitelist VARCHAR(2000),
    seg_log_acessos_ativo BOOLEAN NOT NULL,
    seg_require_numbers BOOLEAN NOT NULL,
    seg_require_special_chars BOOLEAN NOT NULL,
    seg_require_uppercase BOOLEAN NOT NULL,
    seg_tempo_expiracao_sessao INTEGER NOT NULL,
    seg_two_factor_ativo BOOLEAN NOT NULL,
    storage_backup_habilitado BOOLEAN NOT NULL,
    storage_caminho_base VARCHAR(500),
    storage_cloud_bucket VARCHAR(255),
    storage_cloud_provider VARCHAR(20),
    storage_cloud_region VARCHAR(100),
    storage_estrategia_backup VARCHAR(20),
    storage_tamanho_max_ficheiro INTEGER NOT NULL,
    storage_tamanho_max_request INTEGER NOT NULL,
    storage_tipo VARCHAR(20),
    usar_cabecalho_personalizado_em_documentos BOOLEAN NOT NULL,
    usar_logotipo_em_documentos BOOLEAN NOT NULL,
    usar_rodape_personalizado_em_documentos BOOLEAN NOT NULL,
    empresa_id BIGINT NOT NULL
);               
-- 2 +/- SELECT COUNT(*) FROM PUBLIC.configuracao_empresa;    
INSERT INTO configuracao_empresa VALUES
(42, '', FALSE, '', '', '', FALSE, '', '', 'TLS', 'smtp.gmail.com', '', 587, '', TRUE, '', FALSE, '', '', 8, '', TRUE, TRUE, FALSE, TRUE, 30, FALSE, TRUE, 'uploads/', '', '', '', 'DIARIO', 10, 20, 'LOCAL', TRUE, TRUE, FALSE, 1),
(43, '', FALSE, '', '', '', FALSE, '', '', 'TLS', 'smtp.gmail.com', '', 587, '', TRUE, '', FALSE, '', '', 8, '', TRUE, TRUE, FALSE, TRUE, 30, FALSE, TRUE, 'uploads/', '', '', '', 'DIARIO', 10, 20, 'LOCAL', TRUE, TRUE, FALSE, 82);               



CREATE TABLE despesa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    categoria VARCHAR(255),
    data_despesa DATE,
    descricao VARCHAR(255),
    fatura_path VARCHAR(255),
    status VARCHAR(255),
    valor DOUBLE,
    empresa_id BIGINT
);         
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.despesa; 
CREATE TABLE empresa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    email VARCHAR(255),
    endereco VARCHAR(255),
    logotipo VARCHAR(255),
    nif VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    regime_fiscal VARCHAR(255),
    telefone VARCHAR(255),
    website VARCHAR(255)
);    
-- 2 +/- SELECT COUNT(*) FROM PUBLIC.empresa; 
INSERT INTO empresa VALUES
(1, 'analistamosse@gmail.com', 'Luanda, Angola', '/uploads/logo/logo_empresa_1_WhatsApp_Image_2026-04-25_at_14.55.29.jpeg', '5000000000', 'HZ Consultoria Lda', 'GERAL', '934068121', ''),
(82, 'geral@grupoff.pt', 'Bairro Talatona - Luanda, Angola', '/uploads/logo/logo_empresa_82_WhatsApp_Image_2026-04-25_at_14.55.29.jpeg', '5002895196', 'F.FIAU, LDA- Comércio Geral e Prestacao de Serviços', 'GERAL', '+351 XXX XXX XXX', 'https://elegante-corporate-web.lovable.app/');       


CREATE TABLE estabelecimento(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    endereco VARCHAR(255),
    nome VARCHAR(255),
    telefone VARCHAR(255),
    tipo VARCHAR(255),
    visivel BOOLEAN,
    empresa_id BIGINT
);                    
-- 4 +/- SELECT COUNT(*) FROM PUBLIC.estabelecimento;         
INSERT INTO estabelecimento VALUES
(1, NULL, 'Sede Luanda', NULL, 'LOJA', TRUE, 1),
(2, 'CENTRALIDADE DO KILAMBA', 'FFKLIMA', NULL, 'LOJA', TRUE, 82),
(3, 'LISBOA PORTUGAL', 'FFAUTO', NULL, 'LOJA', TRUE, 82),
(4, 'Cidade do Lobito', 'Filial de Benguela', NULL, 'ARMAZEM', TRUE, 82);

CREATE TABLE estado(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    descricao VARCHAR(255),
    nome VARCHAR(255) NOT NULL
);          
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.estado;  
CREATE TABLE estoque(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    quantidade DOUBLE,
    updated_at DATETIME,
    estabelecimento_id BIGINT,
    produto_id BIGINT
);                 
-- 8 +/- SELECT COUNT(*) FROM PUBLIC.estoque; 
INSERT INTO estoque VALUES
(2, 71.0, '2026-05-08 23:33:35.147', 2, 2),
(3, 77.0, '2026-05-15 09:33:18.151', 2, 3),
(4, 35.0, '2026-05-15 09:33:18.228', 2, 5),
(5, 78.0, '2026-05-11 15:21:53.73', 2, 6),
(6, 69.0, '2026-05-11 15:22:07.1', 2, 7),
(7, 15.0, '2026-05-11 15:22:18.584', 2, 8),
(8, 35.0, '2026-05-11 15:22:43.266', 2, 9),
(9, 8.0, '2026-05-15 09:50:54.423', 2, 10);     
CREATE TABLE evento_tracking(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    data_hora DATETIME,
    localizacao VARCHAR(255),
    observacao VARCHAR(255),
    status VARCHAR(255),
    guia_remessa_id BIGINT
);                      
-- 13 +/- SELECT COUNT(*) FROM PUBLIC.evento_tracking;        
INSERT INTO evento_tracking VALUES
(1, '2026-05-10 09:26:39.682', 'Armazens de Benguela', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 1),
(2, '2026-05-11 09:17:03.034', 'xbdfgdf', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 2),
(3, '2026-05-11 09:17:25.66', 'asfas', 'sdtstwer', 'RETORNADO', 2),
(4, '2026-05-11 09:17:35.878', 'fgsdfg', 'fgdfg', 'AGUARDANDO_CLIENTE', 2),
(5, '2026-05-11 09:18:43.289', 'asrwtrw', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 3),
(6, '2026-05-11 09:24:55.551', 'gdfgdf', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 4),
(7, '2026-05-11 09:26:19.612', 'asfsdgdfhdfghfg', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 5),
(8, '2026-05-11 09:38:51.699', 'xg', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 6),
(9, '2026-05-11 09:45:45.844', 'sdfsdfdghfjghkghk Maianga', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 7),
(10, '2026-05-11 09:47:12.954', 'Luanda, Maianga', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 8),
(11, '2026-05-11 09:47:51.526', 'Luanda, Maianga', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 9),
(12, '2026-05-11 09:48:26.066', 'ROCHA PADARIA', '', 'EM_TRANSITO', 9),
(13, '2026-05-11 09:56:11.632', 'Luanda, Maianga', 'Guia emitida e aguardando processamento.', 'EM_PROCESSAMENTO', 10); 
CREATE TABLE fatura(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    codigo_agt VARCHAR(255),
    data_emissao DATETIME,
    enviada_agt BOOLEAN NOT NULL,
    hash VARCHAR(255),
    iva DOUBLE,
    numero_fatura VARCHAR(255),
    status VARCHAR(255),
    tipo_documento VARCHAR(255),
    total DOUBLE,
    compra_id BIGINT,
    empresa_id BIGINT,
    hash_control VARCHAR(255),
    invoice_status VARCHAR(255),
    previous_hash VARCHAR(255),
    system_entry_date DATETIME
);                  
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.fatura;  
CREATE TABLE devolucao(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    data_devolucao DATETIME,
    total DOUBLE,
    motivo VARCHAR(255),
    empresa_id BIGINT,
    fatura_id BIGINT,
    usuario_id BIGINT,
    nota_credito_id BIGINT,
    iva DOUBLE
);            
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.devolucao;               
CREATE TABLE imposto(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    codigo_agt VARCHAR(255),
    motivo_isencao VARCHAR(255),
    nome VARCHAR(255) NOT NULL,
    percentagem NUMERIC(19, 2) NOT NULL,
    tipo VARCHAR(255)
);          
-- 3 +/- SELECT COUNT(*) FROM PUBLIC.imposto; 
INSERT INTO imposto VALUES
(1, 'NOR', NULL, 'IVA - Taxa Normal', 14.00, 'IVA'),
(2, 'SIM', NULL, 'IVA - Taxa Simplificada', 7.00, 'IVA'),
(3, 'ISE', 'Isenção nos termos da lei', 'Isento', 0.00, 'IVA');   

CREATE TABLE inventarios(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    armazem VARCHAR(255),
    codigo VARCHAR(255) NOT NULL,
    created_at DATETIME,
    data_abertura DATE,
    data_previsao_fecho DATE,
    estado VARCHAR(255),
    localizacao VARCHAR(255),
    nome VARCHAR(255) NOT NULL,
    responsavel VARCHAR(255),
    tipo VARCHAR(255),
    empresa_id BIGINT
); 
-- 4 +/- SELECT COUNT(*) FROM PUBLIC.inventarios;             
INSERT INTO inventarios VALUES
(1, 'FFKLIMA', 'INV-2026-001', '2026-05-09 01:23:40.125', '2026-05-09', NULL, 'FINALIZADO', '', 'Inventario da Categoria Informatica', 'Sebastiao Mosse', 'PARCIAL', 82),
(2, 'FFKLIMA', 'INV-2026-002', '2026-05-09 01:24:49.495', '2026-05-09', NULL, 'CANCELADO', '', 'Inventario da Categoria Informatica', 'Sebastiao Mosse', 'PARCIAL', 82),
(3, 'FFKLIMA', 'INV-2026-003', '2026-05-09 01:33:26.394', '2026-05-09', NULL, 'CANCELADO', 'dASD', 'xfsf', 'adsADSa', 'PARCIAL', 82),
(4, 'FFKLIMA', 'INV-2026-004', '2026-05-10 08:52:14.683', '2026-05-10', NULL, 'EM_CONTAGEM', 'TESTE', 'iNVENTARIO DOS CARROS', 'Sebastiao Mosse', 'PARCIAL', 82);                  
CREATE TABLE item_compra(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    iva DOUBLE,
    iva_percentual DOUBLE,
    nome_produto VARCHAR(255),
    preco DOUBLE,
    quantidade INTEGER,
    subtotal DOUBLE,
    compra_id BIGINT,
    produto_id BIGINT
);      
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.item_compra;             
CREATE TABLE guia_remessa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    codigo_validacao VARCHAR(100),
    data_emissao DATETIME,
    data_validacao_agt DATETIME,
    hash_agt VARCHAR(255),
    local_carga VARCHAR(255),
    local_descarga VARCHAR(255),
    matricula_viatura VARCHAR(255),
    motivo_anulacao VARCHAR(255),
    motorista VARCHAR(255),
    numero_guia VARCHAR(255),
    status VARCHAR(255),
    tracking_status VARCHAR(255),
    cliente_id BIGINT,
    fatura_origem_id BIGINT,
    guia_referencia_id BIGINT,
    empresa_id BIGINT
); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.guia_remessa;            
CREATE TABLE item_factura(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    preco DOUBLE,
    produto VARCHAR(255),
    quantidade INTEGER,
    factura_id BIGINT
);          
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.item_factura;            
CREATE TABLE item_guia_remessa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    nome_produto VARCHAR(255),
    quantidade DOUBLE,
    unidade_medida VARCHAR(255),
    guia_remessa_id BIGINT
);             
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.item_guia_remessa;       
CREATE TABLE item_nota_credito(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    nome_produto VARCHAR(255),
    preco_unitario DOUBLE,
    quantidade DOUBLE,
    subtotal DOUBLE,
    nota_credito_id BIGINT
);                  
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.item_nota_credito;       
CREATE TABLE itens_inventario(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    quantidade_contada DOUBLE,
    quantidade_sistema DOUBLE,
    inventario_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL
);          
-- 19 +/- SELECT COUNT(*) FROM PUBLIC.itens_inventario;       
INSERT INTO itens_inventario VALUES
(1, 1.0, 1.0, 1, 2),
(2, 84.0, 87.0, 1, 3),
(3, 36.0, 30.0, 1, 5),
(4, 83.0, 89.0, 1, 6),
(5, 70.0, 79.0, 1, 7),
(6, 45.0, 50.0, 1, 8),
(7, 1.0, 1.0, 2, 2),
(8, 87.0, 87.0, 2, 3),
(9, 30.0, 30.0, 2, 5),
(10, 89.0, 89.0, 2, 6),
(11, 79.0, 79.0, 2, 7),
(12, 50.0, 50.0, 2, 8),
(13, 1.0, 1.0, 3, 2),
(14, 80.0, 87.0, 3, 3),
(15, 25.0, 30.0, 3, 5),
(16, 89.0, 89.0, 3, 6),
(17, 79.0, 79.0, 3, 7),
(18, 7.0, 50.0, 3, 8),
(19, 40.0, 45.0, 4, 9);     
CREATE TABLE metodo_pagamento(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    activo BOOLEAN NOT NULL,
    codigo_agt VARCHAR(10) NOT NULL,
    nome VARCHAR(255) NOT NULL
);             
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.metodo_pagamento;        
CREATE TABLE moeda(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    padrao BOOLEAN NOT NULL,
    sigla VARCHAR(10) NOT NULL,
    taxa_cambio NUMERIC(10, 4)
);                  
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.moeda;   
CREATE TABLE movimento_stock(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    data_movimento DATETIME,
    documento_blob LONGBLOB,
    documento_referencia VARCHAR(255),
    motivo VARCHAR(255),
    nome_documento VARCHAR(255),
    origem VARCHAR(255),
    preco_custo DOUBLE,
    quantidade DOUBLE,
    tipo VARCHAR(255),
    produto_id BIGINT
);              
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.movimento_stock;         
INSERT INTO movimento_stock VALUES
(1, '2026-05-15 09:50:54.443', NULL,'' , 'Aumentando o produto no stock', NULL,'' , 80.0, 5.0, 'ENTRA', 10);         
CREATE TABLE nota_credito(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    data_emissao DATETIME,
    motivo VARCHAR(255),
    numero_nota VARCHAR(255),
    status VARCHAR(255),
    total_credito DOUBLE,
    cliente_id BIGINT,
    fatura_original_id BIGINT
);            
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.nota_credito;            
CREATE TABLE permissao_item_usuario(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    ativo BOOLEAN NOT NULL,
    item VARCHAR(255) NOT NULL,
    modulo VARCHAR(255) NOT NULL,
    usuario_id BIGINT NOT NULL
);                 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.permissao_item_usuario;  
CREATE TABLE permissao_modulo_usuario(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    ativo BOOLEAN NOT NULL,
    modulo VARCHAR(255) NOT NULL,
    usuario_id BIGINT NOT NULL
);           
-- 30 +/- SELECT COUNT(*) FROM PUBLIC.permissao_modulo_usuario;               
INSERT INTO permissao_modulo_usuario VALUES
(1, TRUE, 'DASHBOARD', 1),
(2, TRUE, 'VENDAS', 1),
(3, TRUE, 'STOCK', 1),
(4, TRUE, 'ENTIDADES', 1),
(5, TRUE, 'FACTURACAO', 1),
(6, TRUE, 'FINANCEIRO', 1),
(7, TRUE, 'ADMINISTRACAO', 1),
(8, FALSE, 'DASHBOARD', 2),
(9, TRUE, 'VENDAS', 2),
(10, FALSE, 'STOCK', 2),
(11, FALSE, 'ENTIDADES', 2),
(12, FALSE, 'FACTURACAO', 2),
(13, FALSE, 'FINANCEIRO', 2),
(14, FALSE, 'ADMINISTRACAO', 2),
(15, FALSE, 'DASHBOARD', 3),
(16, TRUE, 'VENDAS', 3),
(17, FALSE, 'STOCK', 3),
(18, FALSE, 'ENTIDADES', 3),
(19, FALSE, 'FACTURACAO', 3),
(20, FALSE, 'FINANCEIRO', 3),
(21, FALSE, 'ADMINISTRACAO', 3),
(22, FALSE, 'DASHBOARD', 4),
(23, TRUE, 'VENDAS', 4),
(24, FALSE, 'STOCK', 4),
(25, FALSE, 'ENTIDADES', 4),
(26, FALSE, 'FACTURACAO', 4),
(27, FALSE, 'FINANCEIRO', 4),
(28, FALSE, 'ADMINISTRACAO', 4),
(29, FALSE, 'PAINEL_GLOBAL', 1),
(30, FALSE, 'PAINEL_GLOBAL', 4);    
CREATE TABLE produto(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    codigo_barra VARCHAR(255),
    data_expiracao DATE,
    data_fabrico DATE,
    descricao VARCHAR(255),
    em_promocao BOOLEAN NOT NULL,
    estoque_minimo DOUBLE,
    imagem VARCHAR(255),
    imagem_blob LONGBLOB,
    iva_percentual DOUBLE,
    nome VARCHAR(255),
    preco DOUBLE NOT NULL,
    preco_compra DOUBLE,
    preco_original DOUBLE,
    quantidade_estoque DOUBLE,
    unidade_medida VARCHAR(255),
    categoria_id BIGINT,
    empresa_id BIGINT,
    estado_id BIGINT
);            
-- 8 +/- SELECT COUNT(*) FROM PUBLIC.produto; 
INSERT INTO produto VALUES
(2, NULL, NULL, NULL, NULL, FALSE, 0.0, NULL, NULL, NULL, NULL, 0.0, NULL, NULL, 1.0, NULL, NULL, 1, NULL),
(3, 'GSDGSDFGDFG', NULL, NULL, 'Computer Monitors for Home, Work & Gaming | Dell USA', FALSE, 0.0, '/uploads/produtos/f2950cef-554f-4ccf-a3ef-9c74e642a85d_Monitor Dell.PNG', NULL, 14.0, 'Monitor Dell', 1000.0, 0.0, NULL, 77.0, NULL, 20, 82, NULL),
(5, 'SSSSFF', NULL, NULL, 'HP Omen 15-5001na
HP Omen 15-5001na (Omen 15-ce Serie)
ProcessadorIntel Core i7-4710HQ 4c/8t 4 x 2.5 - 3.5 GHz, Haswell
', FALSE, 0.0, '/uploads/produtos/0b42dbf0-8851-427f-b14f-94f911c0217a_HP Omen.PNG', NULL, 14.0, 'HP Omen', 3000.0, 0.0, NULL, 35.0, NULL, 20, 82, NULL),
(6, 'fyeDFGSDFGSDFG', NULL, NULL, 'HP OMEN 16-AM0004TX (2024) Gaming Laptop - 14th Gen i9-14900HX, 32GB, 1TB SSD, NVIDIA GeForce RTX 5070 8GB, 16` WQXGA', FALSE, 0.0, '/uploads/produtos/c24cf6a6-16dc-45e7-9483-601b885f57c1_ho omen 16.PNG', NULL, 14.0, 'HP OMEN 16', 2000.0, 0.0, NULL, 78.0, NULL, 20, 82, NULL),
(7, 'hdfhfhfg', NULL, NULL, 'Apple MacBook Pro 2019 | 16`', FALSE, 0.0, '/uploads/produtos/eb9555af-f618-43ad-86e1-2c7b3714f0cc_mackbook Pro.PNG', NULL, 14.0, 'Mackbook Pro', 100.0, 0.0, NULL, 69.0, NULL, 20, 82, NULL),
(8, 'KWZERP', NULL, NULL, 'Sistema de Facturacao Online e Desktop Certificado pela AGT(Administração Geral Tributária)', FALSE, 0.0, '/uploads/produtos/c608f632-080c-4d73-b2bf-d19d11fd3f79_Captura de Tela (5).png', NULL, 14.0, 'KwanzaERP- Sistema de Facturacao', 200.0, 0.0, NULL, 15.0, NULL, 25, 82, NULL),
(9, 'HJJAJSFJSDSD', NULL, NULL, 'Descricao do Produto', FALSE, 0.0, '/uploads/produtos/dc545733-008f-4f92-99b4-ca0921ef45ff_Carro- Hilux.PNG', NULL, 14.0, 'Yundai Santa Fe', 105.0, 0.0, NULL, 35.0, NULL, 24, 82, NULL),
(10, 'FSDGDFGDFDFGHHFG', NULL, NULL, 'Teste', TRUE, 0.0, '/uploads/produtos/9c0225db-9aff-4079-8428-8cc75a6c4b11_COMPUTADOR DESKTOP.PNG', NULL, 0.0, 'PRODUTO DE TESTE1', 80.0, 0.0, 800000.0, 8.0, NULL, 20, 82, NULL);           
CREATE TABLE regime_fiscal(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    codigo VARCHAR(255) NOT NULL,
    descricao VARCHAR(255),
    icone VARCHAR(255),
    nome VARCHAR(255) NOT NULL
);                         
-- 3 +/- SELECT COUNT(*) FROM PUBLIC.regime_fiscal;           
INSERT INTO regime_fiscal VALUES
(1, 'GERAL', 'Para empresas com facturação superior a 250 Milhões de Kz.', 'fas fa-balance-scale', 'Regime Geral'),
(2, 'SIMPLIFICADO', 'Para empresas com facturação entre 7.5 e 250 Milhões de Kz.', 'fas fa-shield-alt', 'Regime Simplificado'),
(3, 'EXCLUSAO', 'Para empresas com facturação inferior a 7.5 Milhões de Kz.', 'fas fa-ban', 'Regime de Exclusão');                
CREATE TABLE retencao(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    activo BOOLEAN NOT NULL,
    nome VARCHAR(255) NOT NULL,
    percentagem NUMERIC(19, 2) NOT NULL
); 
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.retencao;
CREATE TABLE serie(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    activo BOOLEAN NOT NULL,
    ano INTEGER NOT NULL,
    descricao VARCHAR(255),
    prefixo VARCHAR(255) NOT NULL,
    proximo_numero INTEGER NOT NULL,
    empresa_id BIGINT
);        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.serie;   
CREATE TABLE taxa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    activo BOOLEAN NOT NULL,
    codigo VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    percentual BOOLEAN NOT NULL,
    valor NUMERIC(19, 2)
);        
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.taxa;    
CREATE TABLE usuario(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    ativo BOOLEAN NOT NULL,
    bloqueado_ate DATETIME,
    login VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    tentativas_login INTEGER NOT NULL,
    empresa_id BIGINT
);     
-- 4 +/- SELECT COUNT(*) FROM PUBLIC.usuario; 
INSERT INTO usuario VALUES
(1, TRUE, NULL, 'admin', 'Administrador', 'ADMIN', '$2a$10$aY6prH.XxBaIQ7VEnWIT8eq.1gK/OJCWm2QpNpZa4786ERWQwOVry', 0, 82),
(2, TRUE, NULL, 'superadmin', 'Super Administrador', 'SUPERADMIN', '$2a$10$/7s99ypY2BZv8OXm4fLuh.lR0felmBoyQB992Nd4mfJIQ2qgtIFXK', 0, 1),
(3, TRUE, NULL, 'sebastiao.mosse', 'Sebastiao Vunda Mosse', 'SUPERADMIN', '$2a$10$1S9mXav5DZPlUjm6k1rDG.PvOES8q7vl1h63pbTlKDMnlklydYRg6', 0, 1),
(4, TRUE, NULL, 'fiau', 'Fortunato Fiau', 'OPERADOR', '$2a$10$n9VtQIlFjpCgAszzXJLdHeMBWjS4LsG.GA9LHw0qcFu1tR.siPmfe', 0, 82); 
CREATE TABLE usuario_estabelecimento(
    usuario_id BIGINT NOT NULL,
    estabelecimento_id BIGINT NOT NULL
);      
ALTER TABLE usuario_estabelecimento ADD CONSTRAINT CONSTRAINT_C0F PRIMARY KEY(usuario_id, estabelecimento_id);      
-- 2 +/- SELECT COUNT(*) FROM PUBLIC.usuario_estabelecimento; 
INSERT INTO usuario_estabelecimento VALUES
(1, 1),
(4, 2);       
CREATE TABLE usuario_permissoes(
    usuario_id BIGINT NOT NULL,
    permissao VARCHAR(255)
);             
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.usuario_permissoes;      
CREATE TABLE venda_suspensa(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    cliente_nome VARCHAR(255),
    data_hora DATETIME,
    itens_json VARCHAR(255),
    empresa_id BIGINT,
    operador_id BIGINT
);             
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.venda_suspensa;          
CREATE TABLE licencas_geradas(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    ativa BOOLEAN NOT NULL,
    chave_gerada VARCHAR(255),
    cliente_nome VARCHAR(255),
    data_emissao DATETIME,
    data_expiracao DATETIME,
    machine_id VARCHAR(255),
    observacoes VARCHAR(255)
);               
-- 11 +/- SELECT COUNT(*) FROM PUBLIC.licencas_geradas;       
INSERT INTO licencas_geradas VALUES
(1, TRUE, '2v4AFgguPtWHpZNQSAJJFp9E1LWfSg1r14HgOWSDivE4AUfT9f31XZtiXsGBKVAZ', 'Computador- FFFIAU', '2026-05-04 22:03:20.212', '2026-05-04 22:07:00', 'D77F2E2E', ''),
(2, TRUE, '2v4AFgguPtWHpZNQSAJJFp9E1LWfSg1r14HgOWSDivE4AUfT9f31XZtiXsGBKVAZ', 'Computador- FFFIAU', '2026-05-04 22:04:50.623', '2026-05-04 22:07:00', 'D77F2E2E', ''),
(3, TRUE, '2v4AFgguPtWHpZNQSAJJFnQz1+cZFbdjuZ3kdhrBaFc4AUfT9f31XZtiXsGBKVAZ', 'Edificio Sede - Empresa FFIAU', '2026-05-04 22:09:50.616', '2026-05-04 22:15:00', 'D77F2E2E', ''),
(4, TRUE, 'oX9tr8g2BxoqiMxUSJWmEMde14eqRzXd+5ZQ9Kw47pY4AUfT9f31XZtiXsGBKVAZ', 'PRODUSOL- Sebastiao Mosse', '2026-05-05 09:40:39.518', '2026-05-05 11:00:00', '736E21F0', 'Licenca de 3 horas'),
(5, TRUE, 'oX9tr8g2BxoqiMxUSJWmEOyycqae216BErRFxc6fPd44AUfT9f31XZtiXsGBKVAZ', 'Sebastiao Mosse', '2026-05-06 11:37:00.885', '2026-05-06 15:00:00', '736E21F0', 'Licenca de desenvolvimento diario'),
(6, TRUE, 'oX9tr8g2BxoqiMxUSJWmEHpRwOwqVzqocgRu1+kWcRU4AUfT9f31XZtiXsGBKVAZ', 'HZConsulting_Sebastiao Mosse', '2026-05-07 19:45:26.252', '2026-05-08 12:02:00', '736E21F0', 'Licenca de desenvolvimento'),
(7, TRUE, 'oX9tr8g2BxoqiMxUSJWmECdcd4vybEVaM7JtbiFLd/Y4AUfT9f31XZtiXsGBKVAZ', 'Sebastiao Mosse', '2026-05-08 14:22:23.683', '2026-05-08 23:59:00', '736E21F0', ''),
(8, TRUE, 'oX9tr8g2BxoqiMxUSJWmEKHSN/loMkrc4d1UFms1Zao4AUfT9f31XZtiXsGBKVAZ', 'mosse', '2026-05-09 00:03:11.892', '2026-05-09 23:59:00', '736E21F0', 'teste'),
(9, TRUE, 'oX9tr8g2BxoqiMxUSJWmECAmZo3dL/hoN/VupFsc3vE4AUfT9f31XZtiXsGBKVAZ', 'Empresa Teste- Delcio', '2026-05-10 08:24:50.762', '2026-05-11 23:59:00', '736E21F0', 'Licenca de teste'),
(10, TRUE, 'oX9tr8g2BxoqiMxUSJWmEL82l7jiuvwCPyXT01rWTVk4AUfT9f31XZtiXsGBKVAZ', 'HZConsultoria - Sebastiao Mosse ', '2026-05-15 09:25:14.823', '2026-05-15 23:59:00', '736E21F0', 'Licenca de Producao - '),
(11, TRUE, '2v4AFgguPtWHpZNQSAJJFn97CLxdD2SpWdGOgvH9vT84AUfT9f31XZtiXsGBKVAZ', 'Sebastiao Mosse', '2026-05-18 09:43:44.438', '2026-05-18 23:59:00', 'D77F2E2E', 'Licenca de Desenvolvimento'); 
 
CREATE TABLE configuracao_sistema(
    id BIGINT NOT NULL PRIMARY KEY,
    db_connection_timeout INTEGER NOT NULL,
    db_idle_timeout INTEGER NOT NULL,
    db_max_lifetime INTEGER NOT NULL,
    db_pool_max INTEGER NOT NULL,
    db_pool_min INTEGER NOT NULL,
    db_query_timeout INTEGER NOT NULL,
    db_schema VARCHAR(100),
    db_tipobd VARCHAR(50),
    email_nome_remetente VARCHAR(255),
    email_remetente VARCHAR(255),
    email_seguranca_tipo VARCHAR(10),
    email_smtp_host VARCHAR(255),
    email_smtp_password VARCHAR(500),
    email_smtp_porta INTEGER NOT NULL,
    email_smtp_username VARCHAR(255),
    seg_comprimento_min_password INTEGER NOT NULL,
    seg_ip_whitelist VARCHAR(2000),
    seg_lockout_duracao INTEGER NOT NULL,
    seg_log_acessos_ativo BOOLEAN NOT NULL,
    seg_politica_password VARCHAR(20),
    seg_require_numbers BOOLEAN NOT NULL,
    seg_require_special_chars BOOLEAN NOT NULL,
    seg_require_uppercase BOOLEAN NOT NULL,
    seg_tempo_expiracao_sessao INTEGER NOT NULL,
    seg_tentativas_login_max INTEGER NOT NULL,
    seg_two_factor_ativo BOOLEAN NOT NULL,
    servidor_base_url VARCHAR(500),
    servidor_cors_origens VARCHAR(1000),
    servidor_hostname VARCHAR(255),
    servidor_porta INTEGER NOT NULL,
    servidor_proxy_habilitado BOOLEAN NOT NULL,
    servidor_proxy_host VARCHAR(255),
    servidor_proxy_porta INTEGER NOT NULL,
    sistema_backup BOOLEAN NOT NULL,
    sistema_email_suporte VARCHAR(255),
    sistema_nome VARCHAR(255),
    sistema_tema VARCHAR(255),
    sistema_versao VARCHAR(255),
    storage_caminho_base VARCHAR(500),
    storage_cloud_bucket VARCHAR(255),
    storage_cloud_provider VARCHAR(20),
    storage_cloud_region VARCHAR(100),
    storage_estrategia_backup VARCHAR(20),
    storage_tamanho_max_ficheiro INTEGER NOT NULL,
    storage_tamanho_max_request INTEGER NOT NULL,
    storage_tipo VARCHAR(20),
    licenca_data_ativacao DATETIME,
    licenca_chave_ativacao VARCHAR(255),
    seg_tempo_expiracao_unidade VARCHAR(10),
    agt_certificado_numero VARCHAR(20),
    agt_private_key VARCHAR(255),
    agt_public_key VARCHAR(255),
    agt_chave_versao INTEGER,
    exibir_datas_validade BOOLEAN DEFAULT TRUE
);                   
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.configuracao_sistema;    
INSERT INTO configuracao_sistema VALUES
(1, 30000, 30000, 60000, 20, 5, 60000, 'efacturacao', 'MySQL', 'Sistema de Facturação', 'noreply@empresa.ao', 'TLS', 'smtp.gmail.com', '', 587, '', 8, '', 15, TRUE, 'MEDIA', TRUE, FALSE, TRUE, 5, 5, FALSE, 'http://localhost:8080', '*', 'localhost', 8080, FALSE, '', 80, TRUE, 'suporte@facturacao.com', 'Kwanza ERP', 'light', '1.0.0', 'uploads/', '', '', '', 'DIARIO', 10, 20, 'LOCAL', '2026-05-02 13:34:09.931', '2v4AFgguPtWHpZNQSAJJFn97CLxdD2SpWdGOgvH9vT84AUfT9f31XZtiXsGBKVAZ', 'MINUTOS', '000', '', NULL, 1, FALSE);

CREATE TABLE fornecedor(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    email VARCHAR(255),
    endereco VARCHAR(255),
    nif VARCHAR(255),
    nome VARCHAR(255),
    telefone VARCHAR(255),
    empresa_id BIGINT
);  
-- 1 +/- SELECT COUNT(*) FROM PUBLIC.fornecedor;              
INSERT INTO fornecedor VALUES
(1, 'sacos.plasticos@gmail.com', 'Vila de Cacuaco', '5000000000', 'Fornecedor de Sacos plasticos', '934068121', 82);
CREATE TABLE item_devolucao(
    id BIGINT AUTO_INCREMENT NOT NULL PRIMARY KEY,
    nome_produto VARCHAR(255),
    preco DOUBLE,
    quantidade INTEGER,
    subtotal DOUBLE,
    devolucao_id BIGINT,
    produto_id BIGINT,
    iva_percentual DOUBLE,
    iva_valor DOUBLE
);                      
-- 0 +/- SELECT COUNT(*) FROM PUBLIC.item_devolucao;          
   
ALTER TABLE moeda ADD CONSTRAINT UK_3ttl4yaqp84ke6tmp0tfjmfc0 UNIQUE(sigla);          
ALTER TABLE estado ADD CONSTRAINT UK_gfot2y0318rs8hc74ppp0n87p UNIQUE(nome);          
ALTER TABLE inventarios ADD CONSTRAINT UK_mlnadny79moaiuv3fhuu8nmyu UNIQUE(codigo);   
ALTER TABLE usuario ADD CONSTRAINT UK_pm3f4m4fqv89oeeeac4tbe2f4 UNIQUE(login);        
ALTER TABLE permissao_item_usuario ADD CONSTRAINT UK3hhiyx5enul587k3tscsco9j3 UNIQUE(modulo, item, usuario_id);   
ALTER TABLE empresa ADD CONSTRAINT UK_ehmupcalijww3n7835ujr8xwt UNIQUE(nif);          
ALTER TABLE regime_fiscal ADD CONSTRAINT UK_mxi1a9ppu9anlbgpjac6n8s35 UNIQUE(nome);   
ALTER TABLE regime_fiscal ADD CONSTRAINT UK_a0y4l46wm4h70q47cb7uarb0s UNIQUE(codigo); 
ALTER TABLE categoria ADD CONSTRAINT UKcdwvmsokrgmvfvqsgkcqcw613 UNIQUE(nome, empresa_id);          
ALTER TABLE configuracao_empresa ADD CONSTRAINT UK_97xbyq04i680b3dr04m5234r1 UNIQUE(empresa_id);      
ALTER TABLE caixa ADD CONSTRAINT FKptnl9bmsvsaqusrj7qcyktrx9 FOREIGN KEY(estabelecimento_id) REFERENCES estabelecimento(id);     
ALTER TABLE guia_remessa ADD CONSTRAINT FKo6kjfhit4n94flmuaa5wu0yrp FOREIGN KEY(fatura_origem_id) REFERENCES compra(id);         
ALTER TABLE venda_suspensa ADD CONSTRAINT FKk9vklxihqrcl137jwnwmledu6 FOREIGN KEY(operador_id) REFERENCES usuario(id);           
ALTER TABLE fatura ADD CONSTRAINT FKd1ghrf5ki7vf9gcq4jmjpqrcc FOREIGN KEY(empresa_id) REFERENCES empresa(id);    
ALTER TABLE configuracao_empresa ADD CONSTRAINT FKsxch6jluxisut9o4pslcvdpk7 FOREIGN KEY(empresa_id) REFERENCES empresa(id);      
ALTER TABLE usuario_estabelecimento ADD CONSTRAINT FKj3xtpi5ujwwkn6ep7mufsita FOREIGN KEY(estabelecimento_id) REFERENCES estabelecimento(id);    
ALTER TABLE itens_inventario ADD CONSTRAINT FKp680m9omb6xb9hcig0x48lie9 FOREIGN KEY(inventario_id) REFERENCES inventarios(id);   
ALTER TABLE nota_credito ADD CONSTRAINT FKjdhvro6ewowkl0gf26ngf2fti FOREIGN KEY(cliente_id) REFERENCES cliente(id);              
ALTER TABLE devolucao ADD CONSTRAINT FKinowm6a0x6pncqsnd10e54rum FOREIGN KEY(fatura_id) REFERENCES fatura(id);   
ALTER TABLE item_nota_credito ADD CONSTRAINT FKl1amni0w00rbg4k52mb8y6yg6 FOREIGN KEY(nota_credito_id) REFERENCES nota_credito(id);               
ALTER TABLE fornecedor ADD CONSTRAINT FK6meujwuxsioxaiv65lpplf3it FOREIGN KEY(empresa_id) REFERENCES empresa(id);
ALTER TABLE permissao_modulo_usuario ADD CONSTRAINT FKjshrpg8v1aax28t8r9ycdgqog FOREIGN KEY(usuario_id) REFERENCES usuario(id);  
ALTER TABLE cliente ADD CONSTRAINT FKkbui05oidjdj4nb0283u4t319 FOREIGN KEY(empresa_id) REFERENCES empresa(id);   
ALTER TABLE itens_inventario ADD CONSTRAINT FKcwgfqx7qxll2r3tyy5sfnoetb FOREIGN KEY(produto_id) REFERENCES produto(id);          
ALTER TABLE devolucao ADD CONSTRAINT FKr7k6r9rhsgdpv540t14gyq10y FOREIGN KEY(nota_credito_id) REFERENCES fatura(id);             
ALTER TABLE guia_remessa ADD CONSTRAINT FKehrnlw3s9qcaxq7msse4vbbs9 FOREIGN KEY(guia_referencia_id) REFERENCES guia_remessa(id); 
ALTER TABLE item_devolucao ADD CONSTRAINT FKip9yc7yoshh8tkxhqn2a0k4ow FOREIGN KEY(devolucao_id) REFERENCES devolucao(id);        
ALTER TABLE item_compra ADD CONSTRAINT FKovscx99wpxanu7sytiqarv700 FOREIGN KEY(compra_id) REFERENCES compra(id); 
ALTER TABLE caixa ADD CONSTRAINT FKahfjgfx5mwchqkie0wa1ahc3j FOREIGN KEY(operador_id) REFERENCES usuario(id);    
ALTER TABLE caixa ADD CONSTRAINT FKdn6revbbflokfle34y0gd072h FOREIGN KEY(empresa_id) REFERENCES empresa(id);     
ALTER TABLE item_compra ADD CONSTRAINT FKhi8mnlw3bmx5g5rs232xx4i3g FOREIGN KEY(produto_id) REFERENCES produto(id);               
ALTER TABLE evento_tracking ADD CONSTRAINT FKeq1rulq2ys8g1vbssxfspost0 FOREIGN KEY(guia_remessa_id) REFERENCES guia_remessa(id); 
ALTER TABLE guia_remessa ADD CONSTRAINT FK5ngke7gjs7rk4kij334l8kke3 FOREIGN KEY(empresa_id) REFERENCES empresa(id);              
ALTER TABLE item_guia_remessa ADD CONSTRAINT FKh4bca6ynya8pxnl6ri2322c8u FOREIGN KEY(guia_remessa_id) REFERENCES guia_remessa(id);               
ALTER TABLE compra ADD CONSTRAINT FKk39qkguq7uka81tdyvhrkvppf FOREIGN KEY(usuario_id) REFERENCES usuario(id);    
ALTER TABLE usuario_permissoes ADD CONSTRAINT FK9sfyda5c59jju642rrxdb8eml FOREIGN KEY(usuario_id) REFERENCES usuario(id);        
ALTER TABLE movimento_stock ADD CONSTRAINT FKd22x4xu9279wt46mj0kh9rbji FOREIGN KEY(produto_id) REFERENCES produto(id);           
ALTER TABLE item_devolucao ADD CONSTRAINT FKeqeyf0k22ar392c5gm335smuv FOREIGN KEY(produto_id) REFERENCES produto(id);            
ALTER TABLE categoria ADD CONSTRAINT FKph8u7xb0x8axyrppb6gx47766 FOREIGN KEY(empresa_id) REFERENCES empresa(id); 
ALTER TABLE despesa ADD CONSTRAINT FKpdy7sp88krw49dnurmya9uwb8 FOREIGN KEY(empresa_id) REFERENCES empresa(id);   
ALTER TABLE guia_remessa ADD CONSTRAINT FKalhj9hdto2n3r0vv59wokonyg FOREIGN KEY(cliente_id) REFERENCES cliente(id);              
ALTER TABLE serie ADD CONSTRAINT FKgi1146162rttkaf3wec9cevhr FOREIGN KEY(empresa_id) REFERENCES empresa(id);     
ALTER TABLE estoque ADD CONSTRAINT FKh201uorwvq9pjj4dsvjyo73ft FOREIGN KEY(produto_id) REFERENCES produto(id);   
ALTER TABLE item_factura ADD CONSTRAINT FKodo4bwss9y9u6eatk4se4gt76 FOREIGN KEY(factura_id) REFERENCES fatura(id);               
ALTER TABLE nota_credito ADD CONSTRAINT FKlltq97ehobqcxqinhaw8m2irc FOREIGN KEY(fatura_original_id) REFERENCES compra(id);       
ALTER TABLE devolucao ADD CONSTRAINT FKk8nn0iwvnaety52h8bij9c328 FOREIGN KEY(usuario_id) REFERENCES usuario(id); 
ALTER TABLE venda_suspensa ADD CONSTRAINT FK59idydrmiypubd06va0j24meu FOREIGN KEY(empresa_id) REFERENCES empresa(id);            
ALTER TABLE produto ADD CONSTRAINT FKopu9jggwnamfv0c8k2ri3kx0a FOREIGN KEY(categoria_id) REFERENCES categoria(id);               
ALTER TABLE usuario ADD CONSTRAINT FK87ckfs30l64gnivnfk7ywp8l6 FOREIGN KEY(empresa_id) REFERENCES empresa(id);   
ALTER TABLE compra ADD CONSTRAINT FK5hjfoaj3hu0bfnm4s7j3fg5pu FOREIGN KEY(fatura_referencia_id) REFERENCES compra(id);           
ALTER TABLE permissao_item_usuario ADD CONSTRAINT FK7dvia2dm9m264d2e62kx8sryf FOREIGN KEY(usuario_id) REFERENCES usuario(id);    
ALTER TABLE usuario_estabelecimento ADD CONSTRAINT FKdodedv1wjb5ikg9yx8dng5h6a FOREIGN KEY(usuario_id) REFERENCES usuario(id);   
ALTER TABLE produto ADD CONSTRAINT FKhd2qcuv0aepvey17rumtskj9t FOREIGN KEY(empresa_id) REFERENCES empresa(id);   
ALTER TABLE inventarios ADD CONSTRAINT FKej6aqh7jxynm5y1jv6drrkmsi FOREIGN KEY(empresa_id) REFERENCES empresa(id);               
ALTER TABLE fatura ADD CONSTRAINT FKf4w8785d20fswn0d409898sqx FOREIGN KEY(compra_id) REFERENCES compra(id);      
ALTER TABLE estabelecimento ADD CONSTRAINT FKau7lwq27l21rj11tk4bmd7641 FOREIGN KEY(empresa_id) REFERENCES empresa(id);           
ALTER TABLE estoque ADD CONSTRAINT FKo04rd8n41ma0og2p07cuty1vr FOREIGN KEY(estabelecimento_id) REFERENCES estabelecimento(id);   
ALTER TABLE compra ADD CONSTRAINT FKsb016ju051sruhp97e2essyig FOREIGN KEY(empresa_id) REFERENCES empresa(id);    
ALTER TABLE compra ADD CONSTRAINT FKni21w35sfgo033m8l93ki11ab FOREIGN KEY(cliente_id) REFERENCES cliente(id);    
ALTER TABLE produto ADD CONSTRAINT FKfkvuskq2opt55th4cxhcmcpug FOREIGN KEY(estado_id) REFERENCES estado(id);     
ALTER TABLE devolucao ADD CONSTRAINT FKewqrd7upqdq8q1git70fscdwm FOREIGN KEY(empresa_id) REFERENCES empresa(id); 
SET FOREIGN_KEY_CHECKS = 1;
