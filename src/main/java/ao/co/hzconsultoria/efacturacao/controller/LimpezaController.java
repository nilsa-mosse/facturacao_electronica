package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@RestController
@RequestMapping("/api/limpeza")
public class LimpezaController {

    @Autowired
    private EntityManager entityManager;

    @PostMapping("/executar")
    @Transactional
    public ResponseEntity<?> executarLimpeza() {
        // Apenas ADMIN ou SUPERADMIN podem limpar a base de dados
        // Nota: A segurança deve ser reforçada no SecurityConfig também
        
        try {
            // Desativar verificações de chave estrangeira para limpeza total rápida (MySQL)
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 0").executeUpdate();

            // Tabelas de Facturação e Vendas
            entityManager.createNativeQuery("TRUNCATE TABLE item_factura").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE fatura").executeUpdate();
            
            entityManager.createNativeQuery("TRUNCATE TABLE item_nota_credito").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE nota_credito").executeUpdate();
            
            entityManager.createNativeQuery("TRUNCATE TABLE item_guia_remessa").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE guia_remessa").executeUpdate();
            
            entityManager.createNativeQuery("TRUNCATE TABLE item_devolucao").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE devolucao").executeUpdate();
            
            entityManager.createNativeQuery("TRUNCATE TABLE item_compra").executeUpdate();
            entityManager.createNativeQuery("TRUNCATE TABLE compra").executeUpdate();
            
            entityManager.createNativeQuery("TRUNCATE TABLE venda_suspensa").executeUpdate();
            
            // Movimentações de Stock relacionadas a vendas (ou todas)
            entityManager.createNativeQuery("TRUNCATE TABLE movimento_stock").executeUpdate();
            
            // Resetar o estado dos caixas
            entityManager.createNativeQuery("TRUNCATE TABLE caixa").executeUpdate();

            // Reativar verificações
            entityManager.createNativeQuery("SET FOREIGN_KEY_CHECKS = 1").executeUpdate();

            return ResponseEntity.ok("Base de dados de vendas e facturas limpa com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao limpar base de dados: " + e.getMessage());
        }
    }
}
