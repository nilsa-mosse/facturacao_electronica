package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Executa todos os dias às 00:00 para verificar produtos que expiram em menos de 1 mês.
     * Define o estado 'emPromocao' como verdadeiro para esses produtos.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void verificarPromocaoPorExpiracao() {
        LocalDate limite = LocalDate.now().plusMonths(1);
        List<Produto> produtosParaPromocao = produtoRepository.findByDataExpiracaoBeforeAndEmPromocaoFalse(limite);
        
        for (Produto produto : produtosParaPromocao) {
            aplicarRegraPromocao(produto);
        }
        
        if (!produtosParaPromocao.isEmpty()) {
            produtoRepository.saveAll(produtosParaPromocao);
            System.out.println("Automated Promotion: " + produtosParaPromocao.size() + " produtos colocados em promoção.");
        }
    }

    public void aplicarRegraPromocao(Produto produto) {
        if (produto.getDataExpiracao() != null) {
            LocalDate limite = LocalDate.now().plusMonths(1);
            if (produto.getDataExpiracao().isBefore(limite)) {
                produto.setEmPromocao(true);
            }
        }
    }
}
