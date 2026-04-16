-- Script de criação das tabelas para o sistema de faturação eletrónica

CREATE TABLE categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE produto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    preco DECIMAL(10,2) NOT NULL,
    quantidade_estoque INT NOT NULL,
    imagem VARCHAR(255),
    imagem_blob LONGBLOB,
    codigo_barra VARCHAR(50),
    categoria_id BIGINT,
    iva_percentual DECIMAL(5,2) NULL,
    FOREIGN KEY (categoria_id) REFERENCES categoria(id)
);

CREATE TABLE cliente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    nif VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    telefone VARCHAR(20),
    endereco VARCHAR(255)
);

CREATE TABLE venda (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT,
    total DECIMAL(10,2) NOT NULL,
    data_venda DATETIME NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id)
);

CREATE TABLE item_carrinho (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id BIGINT,
    produto_id BIGINT,
    quantidade INT NOT NULL,
    preco_total DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (venda_id) REFERENCES venda(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id)
);

CREATE TABLE fatura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id BIGINT,
    numero_fatura VARCHAR(50) NOT NULL,
    data_emissao DATETIME NOT NULL,
    enviada_agt BOOLEAN DEFAULT FALSE,
    total DECIMAL(15,2),
    iva DECIMAL(15,2),
    hash VARCHAR(255),
    codigo_agt VARCHAR(100),
    FOREIGN KEY (venda_id) REFERENCES venda(id)
);

CREATE TABLE item_factura (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    factura_id BIGINT,
    produto VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL,
    preco DECIMAL(15,2) NOT NULL,
    FOREIGN KEY (factura_id) REFERENCES fatura(id)
);

-- Tabela de usuários para autenticação
CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Criação da tabela de clientes
CREATE TABLE IF NOT EXISTS clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    address TEXT NOT NULL
);

-- Tabelas para estabelecimentos e estoques

CREATE TABLE IF NOT EXISTS estabelecimento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    endereco VARCHAR(255),
    telefone VARCHAR(50),
    tipo VARCHAR(50),
    visivel BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS estoque (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id BIGINT,
    estabelecimento_id BIGINT,
    quantidade DECIMAL(10,2) DEFAULT 0,
    updated_at DATETIME,
    FOREIGN KEY (produto_id) REFERENCES produto(id),
    FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento(id)
);

-- Dados fake para categoria
INSERT INTO categoria (nome) VALUES
('Informática'),
('Escritório e Papelaria'),
('Electrodomésticos'),
('Segurança Electrônica'),
('Smartphones e Tablets'),
('Energia'),
('Imagem e Som'),
('Jogos, Consolas e Desporto');

-- Dados fake para produto
INSERT INTO produto (nome, descricao, preco, quantidade_estoque, imagem, codigo_barra, categoria_id) VALUES
-- Informática
('Notebook Dell', 'Notebook Dell Inspiron 15', 350000.00, 10, 'https://via.placeholder.com/60?text=Dell', 'INF0001', 1),
('Mouse Logitech', 'Mouse sem fio Logitech', 5000.00, 50, 'https://via.placeholder.com/60?text=Mosue', 'INF0002', 1),
('Teclado Microsoft', 'Teclado ergonômico Microsoft', 8000.00, 30, 'https://via.placeholder.com/60?text=Teclado', 'INF0003', 1),
('Monitor LG', 'Monitor LG 24" Full HD', 45000.00, 20, 'https://via.placeholder.com/60?text=Monitor', 'INF0004', 1),
('Impressora HP', 'Impressora multifuncional HP', 60000.00, 15, 'https://via.placeholder.com/60?text=Impressora', 'INF0005', 1),
-- Escritório e Papelaria
('Cadeira Escritório', 'Cadeira ergonômica para escritório', 25000.00, 25, 'https://via.placeholder.com/60?text=Cadeira', 'ESC0001', 2),
('Mesa Escritório', 'Mesa de escritório com gavetas', 40000.00, 10, 'https://via.placeholder.com/60?text=Mesa', 'ESC0002', 2),
('Papel A4', 'Resma de papel A4 500 folhas', 2000.00, 100, 'https://via.placeholder.com/60?text=Papel', 'ESC0003', 2),
('Caneta Bic', 'Caneta esferográfica azul', 500.00, 200, 'https://via.placeholder.com/60?text=Caneta', 'ESC0004', 2),
('Agenda 2026', 'Agenda anual capa dura', 3500.00, 40, 'https://via.placeholder.com/60?text=Agenda', 'ESC0005', 2),
-- Electrodomésticos
('Frigorífico Samsung', 'Frigorífico Samsung 300L', 120000.00, 8, 'https://via.placeholder.com/60?text=Frigorifico', 'ELE0001', 3),
('Microondas LG', 'Microondas LG 30L', 25000.00, 12, 'https://via.placeholder.com/60?text=Microondas', 'ELE0002', 3),
('Máquina Lavar Bosch', 'Máquina de lavar Bosch 8kg', 90000.00, 6, 'https://via.placeholder.com/60?text=Lavadora', 'ELE0003', 3),
('Liquidificador Philips', 'Liquidificador Philips 700W', 8000.00, 18, 'https://via.placeholder.com/60?text=Liquidificador', 'ELE0004', 3),
('Aspirador Electrolux', 'Aspirador de pó Electrolux', 15000.00, 10, 'https://via.placeholder.com/60?text=Aspirador', 'ELE0005', 3),
-- Segurança Electrônica
('Câmera IP', 'Câmera IP Full HD', 12000.00, 20, 'https://via.placeholder.com/60?text=Camera', 'SEG0001', 4),
('Alarme Residencial', 'Sistema de alarme residencial', 25000.00, 8, 'https://via.placeholder.com/60?text=Alarme', 'SEG0002', 4),
('Sensor Movimento', 'Sensor de movimento infravermelho', 6000.00, 30, 'https://via.placeholder.com/60?text=Sensor', 'SEG0003', 4),
('Gravador DVR', 'Gravador digital DVR 4 canais', 18000.00, 5, 'https://via.placeholder.com/60?text=DVR', 'SEG0004', 4),
('Fechadura Digital', 'Fechadura digital com senha', 22000.00, 12, 'https://via.placeholder.com/60?text=Fechadura', 'SEG0005', 4),
-- Smartphones e Tablets
('Smartphone Samsung', 'Samsung Galaxy S22', 250000.00, 15, 'https://via.placeholder.com/60?text=Samsung', 'SMT0001', 5),
('Tablet Apple', 'Apple iPad 10.2"', 300000.00, 10, 'https://via.placeholder.com/60?text=iPad', 'SMT0002', 5),
('Smartphone Xiaomi', 'Xiaomi Redmi Note 11', 120000.00, 20, 'https://via.placeholder.com/60?text=Xiaomi', 'SMT0003', 5),
('Tablet Samsung', 'Samsung Galaxy Tab A7', 140000.00, 8, 'https://via.placeholder.com/60?text=TabA7', 'SMT0004', 5),
('Smartphone Apple', 'Apple iPhone 14', 350000.00, 5, 'https://via.placeholder.com/60?text=iPhone', 'SMT0005', 5),
-- Energia
('Painel Solar', 'Painel solar 250W', 50000.00, 12, 'https://via.placeholder.com/60?text=Solar', 'ENE0001', 6),
('Gerador Honda', 'Gerador Honda 5kVA', 120000.00, 4, 'https://via.placeholder.com/60?text=Gerador', 'ENE0002', 6),
('Bateria Estacionária', 'Bateria estacionária 150Ah', 25000.00, 10, 'https://via.placeholder.com/60?text=Bateria', 'ENE0003', 6),
('Inversor Solar', 'Inversor solar 3kW', 35000.00, 6, 'https://via.placeholder.com/60?text=Inversor', 'ENE0004', 6),
('Luminária LED', 'Luminária LED 50W', 4000.00, 30, 'https://via.placeholder.com/60?text=LED', 'ENE0005', 6),
-- Imagem e Som
('TV LG', 'Smart TV LG 55" UHD', 350000.00, 7, 'https://via.placeholder.com/60?text=TV', 'IMS0001', 7),
('Home Theater Sony', 'Home Theater Sony 5.1', 90000.00, 5, 'https://via.placeholder.com/60?text=HT', 'IMS0002', 7),
('Caixa JBL', 'Caixa de som JBL Bluetooth', 15000.00, 20, 'https://via.placeholder.com/60?text=JBL', 'IMS0003', 7),
('Projetor Epson', 'Projetor Epson XGA', 80000.00, 3, 'https://via.placeholder.com/60?text=Projetor', 'IMS0004', 7),
('Fone Sony', 'Fone de ouvido Sony', 6000.00, 25, 'https://via.placeholder.com/60?text=Fone', 'IMS0005', 7),
-- Jogos, Consolas e Desporto
('PlayStation 5', 'Console PlayStation 5', 400000.00, 4, 'https://via.placeholder.com/60?text=PS5', 'JOG0001', 8),
('Xbox Series X', 'Console Xbox Series X', 380000.00, 3, 'https://via.placeholder.com/60?text=Xbox', 'JOG0002', 8),
('Nintendo Switch', 'Console Nintendo Switch', 250000.00, 6, 'https://via.placeholder.com/60?text=Switch', 'JOG0003', 8),
('Bola Adidas', 'Bola de futebol Adidas', 5000.00, 40, 'https://via.placeholder.com/60?text=Bola', 'JOG0004', 8),
('Raquete Wilson', 'Raquete de tênis Wilson', 8000.00, 10, 'https://via.placeholder.com/60?text=Raquete', 'JOG0005', 8);

-- Dados fake para cliente
INSERT INTO cliente (nome, nif, email, telefone, endereco) VALUES
('João Silva', '500000001', 'joao.silva@email.com', '923000001', 'Rua 1, Luanda'),
('Maria Santos', '500000002', 'maria.santos@email.com', '923000002', 'Rua 2, Luanda'),
('Empresa XYZ', '500000003', 'contato@xyz.com', '923000003', 'Av. Principal, Luanda');

-- Dados fake para venda
INSERT INTO venda (cliente_id, total, data_venda) VALUES
(1, 30.00, '2026-03-14 10:00:00'),
(2, 20.00, '2026-03-14 11:00:00');

-- Dados fake para item_carrinho
INSERT INTO item_carrinho (venda_id, produto_id, quantidade, preco_total) VALUES
(1, 1, 2, 20.00),
(1, 3, 1, 15.50),
(2, 2, 1, 20.00);

-- Dados fake para fatura
INSERT INTO fatura (venda_id, numero_fatura, data_emissao, enviada_agt, total, iva, hash, codigo_agt) VALUES
(1, 'FAT20260314001', '2026-03-14 10:05:00', TRUE, 1000.00, 140.00, 'HASH123', 'AGT001'),
(2, 'FAT20260314002', '2026-03-14 11:05:00', FALSE, 500.00, 70.00, 'HASH456', 'AGT002');

-- Usuários de exemplo (senha: 'admin123' e 'user123' criptografadas com BCrypt)
INSERT INTO usuario (login, senha, nome, role) VALUES
('admin', '$2a$10$7QJwK6QZK6QJwK6QZK6QJwK6QZK6QJwK6QJwK6QZK6QJwK6QZK6QJwK', 'Administrador', 'ADMIN'),
('user', '$2a$10$7QJwK6QZK6QJwK6QZK6QJwK6QZK6QJwK6QJwK6QZK6QJwK6QZK6QJwK', 'Usuário', 'USER');
-- Substitua as senhas por hashes reais gerados com BCrypt

-- Dados fake para a tabela de clientes
INSERT INTO clients (name, email, phone, address) VALUES
('John Doe', 'john.doe@example.com', '123456789', '123 Main St'),
('Jane Smith', 'jane.smith@example.com', '987654321', '456 Elm St'),
('Alice Johnson', 'alice.johnson@example.com', '555123456', '789 Oak St');

-- Dados fake para estabelecimentos e estoques de exemplo
INSERT INTO estabelecimento (nome, endereco, telefone, tipo, visivel) VALUES
('Loja Central', 'Av. Principal, Luanda', '923000100', 'LOJA', TRUE),
('Armazem Principal', 'Zona Industrial, Luanda', '923000200', 'ARMAZEM', TRUE);

-- Exemplo de estoque (vinculado a produtos já inseridos: produto_id 1 e 2 existem)
INSERT INTO estoque (produto_id, estabelecimento_id, quantidade, updated_at) VALUES
(1, 1, 5, NOW()),
(1, 2, 2, NOW()),
(2, 1, 0, NOW());