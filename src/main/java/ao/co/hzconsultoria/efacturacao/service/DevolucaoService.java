package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.i18n.LocaleContextHolder;

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
        if (empresaId != null) {
            devolucao.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
        }
        if (usuarioId != null) {
            devolucao.setUsuario(userRepository.findById(usuarioId).orElse(null));
        }
        devolucao.setDataDevolucao(LocalDateTime.now());

        // Resolver a fatura completa se foi enviado apenas o ID
        if (devolucao.getFatura() != null && devolucao.getFatura().getId() != null) {
            devolucao.setFatura(faturaRepository.findById(devolucao.getFatura().getId()).orElse(null));
        } else {
            devolucao.setFatura(null);
        }

        String tipoNota = devolucao.getTipoNota();

        List<Estabelecimento> estabelecimentos = (empresaId != null)
                ? estabelecimentoRepository.findByEmpresa_Id(empresaId)
                : null;
        Estabelecimento principal = null;
        if (estabelecimentos != null && !estabelecimentos.isEmpty()) {
            principal = estabelecimentos.get(0);
        } else {
            principal = estabelecimentoRepository.findAll().stream().findFirst().orElse(null);
        }

        double total = 0;
        double totalIva = 0;
        if (devolucao.getItens() != null) {
            for (ItemDevolucao item : devolucao.getItens()) {
                if (item.getProduto() == null || item.getProduto().getId() == null) {
                    continue;
                }
                item.setDevolucao(devolucao);

                // Buscar dados reais do produto para o IVA
                Produto pFull = produtoRepository.findById(item.getProduto().getId()).orElse(item.getProduto());
                double percIva = (item.getIvaPercentual() != null) ? item.getIvaPercentual()
                        : (pFull.getIvaPercentual() != null ? pFull.getIvaPercentual() : 0.0);
                double preco = (item.getPreco() != null) ? item.getPreco()
                        : pFull.getPreco();
                int qtd = (item.getQuantidade() != null && item.getQuantidade() > 0) ? item.getQuantidade() : 1;

                item.setPreco(preco);
                item.setQuantidade(qtd);
                item.setIvaPercentual(percIva);

                double subtotal = preco * qtd;
                double valorIva = subtotal * (percIva / 100);

                item.setSubtotal(subtotal);
                item.setIvaValor(valorIva);

                total += subtotal;
                totalIva += valorIva;

                // Ajustar Estoque apenas se houver estabelecimento
                if (principal != null) {
                    final Estabelecimento estabFinal = principal;
                    Estoque estoque = estoqueRepository.findByProdutoAndEstabelecimento(item.getProduto(), estabFinal)
                            .orElseGet(() -> {
                                Estoque novoEstoque = new Estoque();
                                novoEstoque.setProduto(item.getProduto());
                                novoEstoque.setEstabelecimento(estabFinal);
                                novoEstoque.setQuantidade(0.0);
                                return novoEstoque;
                            });

                    double qtdAjuste = qtd;
                    if ("ND".equals(tipoNota)) {
                        estoque.setQuantidade(estoque.getQuantidade() - qtdAjuste);
                    } else {
                        estoque.setQuantidade(estoque.getQuantidade() + qtdAjuste);
                    }
                    estoque.setUpdatedAt(LocalDateTime.now());
                    estoqueRepository.save(estoque);
                }

                // ACTUALIZAR TAMBÉM O RESUMO NO PRODUTO
                Produto p = produtoRepository.findById(item.getProduto().getId()).orElse(null);
                if (p != null) {
                    double qAtual = p.getQuantidadeEstoque() != null ? p.getQuantidadeEstoque() : 0.0;
                    if ("ND".equals(tipoNota)) {
                        p.setQuantidadeEstoque(qAtual - qtd);
                    } else {
                        p.setQuantidadeEstoque(qAtual + qtd);
                    }
                    produtoRepository.save(p);
                }
            }
        }

        devolucao.setTotal(total);
        devolucao.setIva(totalIva);
        Devolucao salva = devolucaoRepository.save(devolucao);

        // GERAR NOTA DE CRÉDITO OU NOTA DE DÉBITO (DOCUMENTO FISCAL)
        if (salva.getFatura() != null) {
            try {
                Fatura nc = faturaService.emitirNota(salva, tipoNota);
                salva.setNotaCredito(nc);
                devolucaoRepository.save(salva);

                // Gerar o PDF
                faturaService.gerarPdfFatura(nc, LocaleContextHolder.getLocale());
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(DevolucaoService.class)
                        .error("Erro ao emitir documento fiscal para devolução {}: {}", salva.getId(), ex.getMessage(),
                                ex);
            }
        }

        return salva;
    }

    public Devolucao buscarPorId(Long id, Long empresaId) {
        Devolucao devolucao = devolucaoRepository.findById(id).orElse(null);
        if (devolucao != null) {
            if (empresaId == null
                    || (devolucao.getEmpresa() != null && devolucao.getEmpresa().getId().equals(empresaId))) {
                return devolucao;
            }
        }
        return null;
    }
}
