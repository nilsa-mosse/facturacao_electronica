-- Script DDL para a tabela Compra
CREATE TABLE Compra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_compra DATETIME NOT NULL,
    total DECIMAL(10, 2) NOT NULL
);

-- Script DDL para a tabela ItemCompra
CREATE TABLE ItemCompra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_produto VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL,
    preco DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    compra_id BIGINT NOT NULL,
    FOREIGN KEY (compra_id) REFERENCES Compra(id) ON DELETE CASCADE
);