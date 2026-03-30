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

    public Compra finalizarVenda(Compra compra) {
        if (compra == null) {
            throw new IllegalArgumentException("Compra cannot be null");
        }
        if (compra.getItens() == null || compra.getItens().isEmpty()) {
            throw new IllegalArgumentException("Compra must have at least one item");
        }
        compra.setDataCompra(LocalDateTime.now());
        double totalSemImposto = 0;
        double valorIva = 0;
        for (ItemCompra item : compra.getItens()) {
            Produto produto = produtoRepository.findByCodigoBarra(item.getNomeProduto());
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
        // Gerar hash SHA256(numeroFatura+data+totalFinal)
        String numeroFatura = "FT-" + System.currentTimeMillis();
        String data = compra.getDataCompra().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String hash = gerarHash(numeroFatura + data + totalFinal);
        // Simular envio para AGT e validação
        String codigoAgt = "AGT" + System.currentTimeMillis();
        String statusAgt = "VALIDADA";
        // Salvar compra
        Compra compraSalva = compraRepository.save(compra);
        // Salvar fatura
        // ... código para salvar fatura com todos os campos ...
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

    public boolean cancelarVenda(Long id) {
        return compraRepository.findById(id).map(compra -> {
            compra.setStatus("CANCELADA");
            compraRepository.save(compra);
            return true;
        }).orElse(false);
    }
}