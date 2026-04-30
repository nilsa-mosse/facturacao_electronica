package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.MovimentoStock;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.model.Estoque;
import ao.co.hzconsultoria.efacturacao.model.Estabelecimento;
import ao.co.hzconsultoria.efacturacao.repository.MovimentoStockRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.repository.EstoqueRepository;
import ao.co.hzconsultoria.efacturacao.repository.EstabelecimentoRepository;
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

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.InventarioRepository inventarioRepository;

    @Transactional
    public MovimentoStock registrarMovimento(Long produtoId, Double quantidade, String tipo, String motivo, 
                                            String referencia, String origem, String nomeDoc, byte[] blobDoc,
                                            Double precoCusto) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        double estoqueAtual = produto.getQuantidadeEstoque() != null ? produto.getQuantidadeEstoque() : 0.0;
        double novaQuantidade = estoqueAtual;

        if ("ENTRA".equals(tipo)) {
            novaQuantidade = estoqueAtual + quantidade;
        } else if ("SAIDA".equals(tipo)) {
            novaQuantidade = estoqueAtual - quantidade;
        } else if ("AJUSTE".equals(tipo)) {
            novaQuantidade = quantidade;
        }

        produto.setQuantidadeEstoque(novaQuantidade);
        produtoRepository.save(produto);

        // Sincronizar com a tabela de Estoque (por estabelecimento)
        if (produto.getEmpresa() != null) {
            List<Estabelecimento> estabelecimentos = 
                estabelecimentoRepository.findByEmpresa_Id(produto.getEmpresa().getId());
            
            if (!estabelecimentos.isEmpty()) {
                Estabelecimento principal = estabelecimentos.get(0);
                Estoque estoque = estoqueRepository.findByProdutoAndEstabelecimento(produto, principal)
                        .orElse(new Estoque());
                
                estoque.setProduto(produto);
                estoque.setEstabelecimento(principal);
                estoque.setQuantidade(novaQuantidade);
                estoque.setUpdatedAt(LocalDateTime.now());
                estoqueRepository.save(estoque);
                System.out.println("SUCESSO: Stock na tabela Estoque actualizado para " + novaQuantidade + " no estabelecimento " + principal.getNome());
            } else {
                System.err.println("AVISO: Nenhum estabelecimento encontrado para a empresa. A tabela Estoque não foi actualizada.");
            }
        }

        System.out.println("SUCESSO: Stock na tabela Produto actualizado de " + estoqueAtual + " para " + novaQuantidade);

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

        return movimentoStockRepository.save(movimento);
    }

    public List<MovimentoStock> listarTodos() {
        return movimentoStockRepository.findAllByOrderByDataMovimentoDesc();
    }

    public List<Produto> buscarProdutosComStockBaixo() {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            return produtoRepository.findProdutosComStockBaixo(empresaId);
        } else {
            return java.util.Collections.emptyList();
        }
    }

    public List<Produto> listarTodosProdutos() {
        return produtoRepository.findAll();
    }

    public MovimentoStock buscarPorId(Long id) {
        return movimentoStockRepository.findById(id).orElse(null);
    }

    /**
     * Check if a product is marked in any active PARCIAL inventory for the current company.
     * Active states that block sales: RASCUNHO, EM_CONTAGEM, EM_REVISAO
     */
    public boolean isProdutoEmInventarioParcial(Long produtoId) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) return false;

        java.util.List<ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario> estados = new java.util.ArrayList<>();
        estados.add(ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario.RASCUNHO);
        estados.add(ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario.EM_CONTAGEM);
        estados.add(ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario.EM_REVISAO);

        java.util.List<ao.co.hzconsultoria.efacturacao.model.Inventario> inventarios = inventarioRepository.findByEmpresa_IdAndTipoAndEstadoIn(empresaId, ao.co.hzconsultoria.efacturacao.model.Inventario.TipoInventario.PARCIAL, estados);
        if (inventarios == null || inventarios.isEmpty()) return false;

        for (ao.co.hzconsultoria.efacturacao.model.Inventario inv : inventarios) {
            if (inv.getItens() == null) continue;
            for (ao.co.hzconsultoria.efacturacao.model.ItemInventario it : inv.getItens()) {
                if (it != null && it.getProduto() != null && it.getProduto().getId() != null && it.getProduto().getId().equals(produtoId)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Return a set of product IDs that are part of any active PARCIAL inventory for the current company.
     */
    public java.util.Set<Long> listarProdutosEmInventarioParcial() {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        java.util.Set<Long> result = new java.util.HashSet<>();
        if (empresaId == null) return result;

        java.util.List<ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario> estados = new java.util.ArrayList<>();
        estados.add(ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario.RASCUNHO);
        estados.add(ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario.EM_CONTAGEM);
        estados.add(ao.co.hzconsultoria.efacturacao.model.Inventario.EstadoInventario.EM_REVISAO);

        java.util.List<ao.co.hzconsultoria.efacturacao.model.Inventario> inventarios = inventarioRepository.findByEmpresa_IdAndTipoAndEstadoIn(empresaId, ao.co.hzconsultoria.efacturacao.model.Inventario.TipoInventario.PARCIAL, estados);
        if (inventarios == null || inventarios.isEmpty()) return result;

        for (ao.co.hzconsultoria.efacturacao.model.Inventario inv : inventarios) {
            if (inv.getItens() == null) continue;
            for (ao.co.hzconsultoria.efacturacao.model.ItemInventario it : inv.getItens()) {
                if (it != null && it.getProduto() != null && it.getProduto().getId() != null) {
                    result.add(it.getProduto().getId());
                }
            }
        }
        return result;
    }
}