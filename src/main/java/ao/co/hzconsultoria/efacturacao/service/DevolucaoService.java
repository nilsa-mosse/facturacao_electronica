package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DevolucaoService {

    @Autowired
    private DevolucaoRepository devolucaoRepository;
    
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private FaturaRepository faturaRepository;

    public List<Devolucao> listarTodas(Long empresaId) {
        return devolucaoRepository.findByEmpresa_Id(empresaId);
    }

    @Transactional
    public Devolucao registrarDevolucao(Devolucao devolucao, Long empresaId, Long usuarioId) {
        devolucao.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
        devolucao.setUsuario(userRepository.findById(usuarioId).orElse(null));
        devolucao.setDataDevolucao(LocalDateTime.now());
        
        String tipoNota = devolucao.getTipoNota();

        List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findByEmpresa_Id(empresaId);
        if (estabelecimentos.isEmpty()) {
            throw new RuntimeException("Nenhum estabelecimento encontrado para a empresa.");
        }
        Estabelecimento principal = estabelecimentos.get(0);

        double total = 0;
        double totalIva = 0;
        if (devolucao.getItens() != null) {
            for (ItemDevolucao item : devolucao.getItens()) {
                item.setDevolucao(devolucao);
                
                // Buscar dados reais do produto para o IVA
                Produto pFull = produtoRepository.findById(item.getProduto().getId()).orElse(item.getProduto());
                double percIva = pFull.getIvaPercentual() != null ? pFull.getIvaPercentual() : 0.0;
                double valorIva = (item.getPreco() * item.getQuantidade()) * (percIva / 100);
                
                item.setIvaPercentual(percIva);
                item.setIvaValor(valorIva);
                item.setSubtotal(item.getPreco() * item.getQuantidade());
                
                total += item.getSubtotal();
                totalIva += valorIva;

                // Ajustar Estoque
                Estoque estoque = estoqueRepository.findByProdutoAndEstabelecimento(item.getProduto(), principal)
                        .orElseGet(() -> {
                            Estoque novoEstoque = new Estoque();
                            novoEstoque.setProduto(item.getProduto());
                            novoEstoque.setEstabelecimento(principal);
                            novoEstoque.setQuantidade(0.0);
                            return novoEstoque;
                        });
                
                double qtdAjuste = item.getQuantidade();
                if ("ND".equals(tipoNota)) {
                    estoque.setQuantidade(estoque.getQuantidade() - qtdAjuste);
                } else {
                    estoque.setQuantidade(estoque.getQuantidade() + qtdAjuste);
                }
                estoque.setUpdatedAt(LocalDateTime.now());
                estoqueRepository.save(estoque);

                // ACTUALIZAR TAMBÉM O RESUMO NO PRODUTO (BUSCANDO COMPLETO PARA EVITAR SOBREPOSIÇÃO DE NULLS)
                if (item.getProduto() != null && item.getProduto().getId() != null) {
                    Produto p = produtoRepository.findById(item.getProduto().getId()).orElse(null);
                    if (p != null) {
                        double qAtual = p.getQuantidadeEstoque() != null ? p.getQuantidadeEstoque() : 0.0;
                        if ("ND".equals(tipoNota)) {
                            p.setQuantidadeEstoque(qAtual - qtdAjuste);
                        } else {
                            p.setQuantidadeEstoque(qAtual + qtdAjuste);
                        }
                        produtoRepository.save(p);
                    }
                }
            }
        }
        
        devolucao.setTotal(total);
        devolucao.setIva(totalIva);
        Devolucao salva = devolucaoRepository.save(devolucao);

        // GERAR NOTA DE CRÉDITO OU NOTA DE DÉBITO (DOCUMENTO FISCAL)
        if (salva.getFatura() != null) {
            Fatura nc = faturaService.emitirNota(salva, tipoNota);
            salva.setNotaCredito(nc);
            devolucaoRepository.save(salva);
            
            // AGORA SIM, com o vínculo salvo no banco, geramos o PDF
            faturaService.gerarPdfFatura(nc);
        }

        return salva;
    }

    public Devolucao buscarPorId(Long id, Long empresaId) {
        Devolucao devolucao = devolucaoRepository.findById(id).orElse(null);
        if (devolucao != null && devolucao.getEmpresa().getId().equals(empresaId)) {
            return devolucao;
        }
        return null;
    }
}
