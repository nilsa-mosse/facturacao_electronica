package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.MovimentoStock;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.MovimentoStockRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    @Autowired
    private MovimentoStockRepository movimentoStockRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Transactional
    public MovimentoStock registrarMovimento(Long produtoId, Double quantidade, String tipo, String motivo, 
                                            String referencia, String origem, String nomeDoc, byte[] blobDoc,
                                            Double precoCusto) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        double estoqueAtual = produto.getQuantidadeEstoque() != null ? produto.getQuantidadeEstoque() : 0.0;

        if ("ENTRA".equals(tipo)) {
            produto.setQuantidadeEstoque(estoqueAtual + quantidade);
        } else if ("SAIDA".equals(tipo)) {
            produto.setQuantidadeEstoque(estoqueAtual - quantidade);
        } else if ("AJUSTE".equals(tipo)) {
            produto.setQuantidadeEstoque(quantidade);
        }

        MovimentoStock movimento = new MovimentoStock();
        movimento.setProduto(produto);
        movimento.setQuantidade(quantidade);
        movimento.setTipo(tipo);
        movimento.setMotivo(motivo);
        movimento.setDocumentoReferencia(referencia);
        movimento.setDataMovimento(LocalDateTime.now());
        movimento.setOrigem(origem);
        movimento.setPrecoCusto(precoCusto);
        movimento.setNomeDocumento(nomeDoc);
        movimento.setDocumentoBlob(blobDoc);

        produtoRepository.save(produto);
        return movimentoStockRepository.save(movimento);
    }

    public List<MovimentoStock> listarTodos() {
        return movimentoStockRepository.findAllByOrderByDataMovimentoDesc();
    }

    public List<Produto> buscarProdutosComStockBaixo() {
        return produtoRepository.findProdutosComStockBaixo();
    }

    public List<Produto> listarTodosProdutos() {
        return produtoRepository.findAll();
    }

    public MovimentoStock buscarPorId(Long id) {
        return movimentoStockRepository.findById(id).orElse(null);
    }
}
