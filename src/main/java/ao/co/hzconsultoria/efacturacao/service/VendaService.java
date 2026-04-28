package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Caixa;
import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VendaService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.UserRepository userRepository;

    @Autowired
    private StockService stockService;

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private NotaCreditoService notaCreditoService;

    @Autowired
    private CaixaService caixaService;

    public Compra finalizarVenda(List<Carrinho> carrinho) {
        Compra compra = new Compra();
        compra.setDataCompra(LocalDateTime.now());
        compra.setTotal(carrinho.get(0).getItens().stream().mapToDouble(item -> item.getPreco() * item.getQuantidade()).sum());

        compra.setItens(carrinho.get(0).getItens().stream().map(item -> {
            ItemCompra itemCompra = new ItemCompra();
            itemCompra.setNomeProduto(item.getNome());
            itemCompra.setQuantidade(item.getQuantidade());
            itemCompra.setPreco(item.getPreco());
            itemCompra.setSubtotal(item.getQuantidade() * item.getPreco());
            return itemCompra;
        }).collect(Collectors.toList()));

        return compraRepository.save(compra);
    }

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository empresaRepository;

    public Compra finalizarVenda(Compra compra) {
        return finalizarVenda(compra, "FT"); // Default to Factura
    }

    @org.springframework.transaction.annotation.Transactional
    public Compra finalizarVenda(Compra compra, String tipoDocumento) {
        if (compra == null) {
            throw new IllegalArgumentException("Compra cannot be null");
        }
        
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        ao.co.hzconsultoria.efacturacao.model.Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(null) : null;
        compra.setEmpresa(empresa);

        // Associar Utilizador Logado
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
            Long userId = ((ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal()).getId();
            userRepository.findById(userId).ifPresent(compra::setUsuario);
        }

        if (compra.getItens() == null || compra.getItens().isEmpty()) {
            throw new IllegalArgumentException("Compra must have at least one item");
        }
        compra.setTipoDocumento(tipoDocumento);
        compra.setDataCompra(LocalDateTime.now());
        double totalSemImposto = 0;
        double valorIva = 0;
        for (ItemCompra item : compra.getItens()) {
            Produto produto = null;
            if (item.getProdutoId() != null) {
                produto = produtoRepository.findById(item.getProdutoId()).orElse(null);
            }
            if (produto == null) {
                produto = produtoRepository.findByCodigoBarraAndEmpresa_Id(item.getNomeProduto(), empresaId);
            }

            double ivaPercentual = 0;
            if (produto != null && produto.getIvaPercentual() != null) {
                ivaPercentual = produto.getIvaPercentual();
            }
            double subtotal = item.getSubtotal();
            double itemIva = 0;
            if (ivaPercentual > 0) {
                itemIva = subtotal * (ivaPercentual / 100);
                valorIva += itemIva;
            }
            item.setIva(itemIva);
            item.setIvaPercentual(ivaPercentual);
            totalSemImposto += subtotal;
        }
        double totalFinal = totalSemImposto + valorIva;
        compra.setTotal(totalFinal);
        compra.setStatus("EMITIDA"); 
        
        if (compra.getItens() != null) {
            compra.getItens().forEach(item -> item.setCompra(compra));
        }

        String numeroFatura = "FT-" + System.currentTimeMillis();
        // Gerar hash e simular envio AGT omitidos para brevidade se desejar, mas vamos manter o fluxo
        Compra compraSalva = compraRepository.save(compra);

        for (ItemCompra item : compraSalva.getItens()) {
            Produto produto = null;
            if (item.getProdutoId() != null) {
                produto = produtoRepository.findById(item.getProdutoId()).orElse(null);
            }
            if (produto == null) {
                produto = produtoRepository.findByNomeStartingWithIgnoreCaseAndEmpresa_Id(item.getNomeProduto(), empresaId).stream().findFirst().orElse(null);
            }
            if (produto == null) {
                produto = produtoRepository.findByCodigoBarraAndEmpresa_Id(item.getNomeProduto(), empresaId);
            }

            if (produto == null) {
                System.err.println("AVISO CRÍTICO: Produto não encontrado para a venda! NomeProduto: " + item.getNomeProduto() + ", ProdutoId: " + item.getProdutoId());
            }

            if (produto != null && !"FP".equals(tipoDocumento)) {
                String descricaoMov = "Venda / Facturação (" + tipoDocumento + ") - " + compraSalva.getFormaPagamento();
                if (compraSalva.getReferenciaMulticaixa() != null && !compraSalva.getReferenciaMulticaixa().isEmpty()) {
                    descricaoMov += " (Ref: " + compraSalva.getReferenciaMulticaixa() + ")";
                }

                int qtd = item.getQuantidade() != null ? item.getQuantidade() : 1;
                
                stockService.registrarMovimento(
                    produto.getId(), 
                    (double) qtd, 
                    "SAIDA", 
                    descricaoMov, 
                    numeroFatura, 
                    compraSalva.getNomeCliente(), 
                    null, 
                    null, 
                    produto.getPreco()
                );
            } else {
                System.err.println("AVISO CRÍTICO: Produto ignorado na baixa de stock por ser nulo ou tratar-se de Proforma.");
            }
        }

        // Salvar fatura fiscal e gerar PDF
        faturaService.emitirDocumento(compraSalva, tipoDocumento);

        // Registrar no Caixa se for um documento que movimenta valores (FT/FR)
        if (!"FP".equals(tipoDocumento)) {
            try {
                Caixa caixaAberto = caixaService.getCaixaAbertoAtual();
                if (caixaAberto != null) {
                    Double multicaixa = compraSalva.getValorPagoMulticaixa() != null ? compraSalva.getValorPagoMulticaixa() : 0.0;
                    Double numerario = compraSalva.getTotal() - multicaixa;
                    caixaService.registarVendaNoCaixa(caixaAberto, numerario, multicaixa);
                }
            } catch (Exception e) {
                System.err.println("Erro ao registrar venda no caixa: " + e.getMessage());
            }
        }

        return compraSalva;
    }

    private String gerarHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash SHA-256", e);
        }
    }

    public List<Compra> finalizarVendas(List<Compra> compras) {
        for (Compra compra : compras) {
            finalizarVenda(compra); // Reuse the existing method for individual Compra
        }
        return compras;
    }

    public boolean cancelarVenda(Long id, String motivo) {
        return compraRepository.findById(id).map(compra -> {
            boolean wasEmitida = "EMITIDA".equals(compra.getStatus());
            compra.setStatus("CANCELADA");
            compra.setMotivoAnulacao(motivo);
            compraRepository.save(compra);
            
            // Se já tinha sido emitida, gera Nota de Crédito fiscal automaticamente
            if (wasEmitida) {
                notaCreditoService.gerarNotaCreditoAutomatica(compra, motivo);
            }
            return true;
        }).orElse(false);
    }

    public boolean restaurarVenda(Long id) {
        return compraRepository.findById(id).map(compra -> {
            compra.setStatus("EMITIDA");
            compraRepository.save(compra);
            return true;
        }).orElse(false);
    }
}