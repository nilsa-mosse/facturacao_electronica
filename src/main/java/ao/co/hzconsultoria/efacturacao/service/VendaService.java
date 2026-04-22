package ao.co.hzconsultoria.efacturacao.service;

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
    private StockService stockService;

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private NotaCreditoService notaCreditoService;

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

    public Compra finalizarVenda(Compra compra, String tipoDocumento) {
        if (compra == null) {
            throw new IllegalArgumentException("Compra cannot be null");
        }
        
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        ao.co.hzconsultoria.efacturacao.model.Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(null) : null;
        compra.setEmpresa(empresa);

        if (compra.getItens() == null || compra.getItens().isEmpty()) {
            throw new IllegalArgumentException("Compra must have at least one item");
        }
        compra.setTipoDocumento(tipoDocumento);
        compra.setDataCompra(LocalDateTime.now());
        double totalSemImposto = 0;
        double valorIva = 0;
        for (ItemCompra item : compra.getItens()) {
            Produto produto = produtoRepository.findByCodigoBarraAndEmpresa_Id(item.getNomeProduto(), empresaId);
            double ivaPercentual = 0;
            if (produto != null && produto.getIvaPercentual() != null) {
                ivaPercentual = produto.getIvaPercentual();
            }
            double subtotal = item.getSubtotal();
            if (ivaPercentual > 0) {
                valorIva += subtotal * (ivaPercentual / 100);
            }
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

            if (produto != null) {
                String descricaoMov = "Venda / Facturação (" + tipoDocumento + ") - " + compraSalva.getFormaPagamento();
                if (compraSalva.getReferenciaMulticaixa() != null && !compraSalva.getReferenciaMulticaixa().isEmpty()) {
                    descricaoMov += " (Ref: " + compraSalva.getReferenciaMulticaixa() + ")";
                }

                stockService.registrarMovimento(
                    produto.getId(), 
                    item.getQuantidade().doubleValue(), 
                    "SAIDA", 
                    descricaoMov, 
                    numeroFatura, 
                    compraSalva.getNomeCliente(), 
                    null, 
                    null, 
                    produto.getPreco()
                );
            }
        }

        // Salvar fatura fiscal e gerar PDF
        faturaService.emitirDocumento(compraSalva, tipoDocumento);

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