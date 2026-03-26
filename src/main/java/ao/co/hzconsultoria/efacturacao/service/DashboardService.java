package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    public List<ProdutoMaisVendidoDTO> getProdutosMaisVendidos(int limite) {
        List<Compra> compras = compraRepository.findAll();
        Map<String, ProdutoMaisVendidoDTO> mapa = new HashMap<>();
        for (Compra compra : compras) {
            if (compra.getItens() != null) {
                for (ItemCompra item : compra.getItens()) {
                    String nome = item.getNomeProduto();
                    if (nome == null) continue;
                    ProdutoMaisVendidoDTO dto = mapa.getOrDefault(nome, new ProdutoMaisVendidoDTO(nome, 0));
                    dto.setQuantidadeVendida(dto.getQuantidadeVendida() + (item.getQuantidade() != null ? item.getQuantidade() : 0));
                    mapa.put(nome, dto);
                }
            }
        }
        return mapa.values().stream()
                .sorted(Comparator.comparingInt(ProdutoMaisVendidoDTO::getQuantidadeVendida).reversed())
                .limit(limite)
                .collect(Collectors.toList());
    }

    public Map<String, Integer> getVendasUltimos30Dias() 
    {
        List<Compra> compras = compraRepository.findAll();
        Map<String, Integer> vendasPorDia = new LinkedHashMap<>();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            java.time.LocalDate dia = hoje.minusDays(i);
            vendasPorDia.put(dia.toString(), 0);
        }
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null) {
                java.time.LocalDate data = compra.getDataCompra().toLocalDate();
                String dataStr = data.toString();
                if (vendasPorDia.containsKey(dataStr)) {
                    vendasPorDia.put(dataStr, vendasPorDia.get(dataStr) + 1);
                }
            }
        }
        return vendasPorDia;
    }

    public int getVendasDia() {
        List<Compra> compras = compraRepository.findAll();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int total = 0;
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null && compra.getDataCompra().toLocalDate().equals(hoje)) {
                total++;
            }
        }
        return total;
    }

    public double getReceitaMensal() {
        List<Compra> compras = compraRepository.findAll();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        double receita = 0.0;
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null) {
                java.time.LocalDate data = compra.getDataCompra().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual) {
                    receita += (compra.getTotal() != null ? compra.getTotal() : 0.0);
                }
            }
        }
        return receita;
    }

    public List<Produto> getProdutosEstoqueBaixo(int limiteEstoque) {
        List<Produto> produtos = produtoRepository.findAll();
        List<Produto> estoqueBaixo = new ArrayList<>();
        for (Produto produto : produtos) {
            if (produto.getQuantidadeEstoque() <= limiteEstoque) {
                estoqueBaixo.add(produto);
            }
        }
        return estoqueBaixo;
    }

    public List<Compra> getComprasDoDia() {
        List<Compra> compras = compraRepository.findAll();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        List<Compra> vendasDoDia = new ArrayList<>();
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null && compra.getDataCompra().toLocalDate().equals(hoje)) {
                vendasDoDia.add(compra);
            }
        }
        return vendasDoDia;
    }

    public List<Compra> getComprasDoMes() {
        List<Compra> compras = compraRepository.findAll();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        List<Compra> vendasDoMes = new ArrayList<>();
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null) {
                java.time.LocalDate data = compra.getDataCompra().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual) {
                    vendasDoMes.add(compra);
                }
            }
        }
        return vendasDoMes;
    }

    public static class ProdutoMaisVendidoDTO {
        private String nome;
        private int quantidadeVendida;
        public ProdutoMaisVendidoDTO(String nome, int quantidadeVendida) {
            this.nome = nome;
            this.quantidadeVendida = quantidadeVendida;
        }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public int getQuantidadeVendida() { return quantidadeVendida; }
        public void setQuantidadeVendida(int quantidadeVendida) { this.quantidadeVendida = quantidadeVendida; }
    }
}