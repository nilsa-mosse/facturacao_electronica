package ao.co.hzconsultoria.efacturacao.service.ia;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Motor de IA Analítica & Preditiva NATIVO em Java (100% Gratuito e Offline).
 * Processa inteligência sobre os dados financeiros, clientes e produtos.
 */
@Service
public class IaAnaliticaService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FaturaRepository faturaRepository;

    // --- 1. Previsão de Vendas (Próximos 30 dias) ---
    public Map<String, Object> preverVendasProximoMes(Long empresaId) {
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        
        LocalDate hoje = LocalDate.now();
        Map<YearMonthKey, Double> vendasMensais = new TreeMap<>();
        
        // Agrupar vendas nos últimos 6 meses
        for (int i = 5; i >= 0; i--) {
            LocalDate m = hoje.minusMonths(i);
            vendasMensais.put(new YearMonthKey(m.getYear(), m.getMonthValue()), 0.0);
        }

        for (Compra c : compras) {
            if (c.getDataCompra() != null && !"CANCELADA".equalsIgnoreCase(c.getStatus())) {
                LocalDate data = c.getDataCompra().toLocalDate();
                YearMonthKey key = new YearMonthKey(data.getYear(), data.getMonthValue());
                if (vendasMensais.containsKey(key)) {
                    vendasMensais.put(key, vendasMensais.get(key) + (c.getTotal() != null ? c.getTotal() : 0.0));
                }
            }
        }

        List<Double> valores = new ArrayList<>(vendasMensais.values());
        double mediaHistorica = valores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // Tendência Linear Simples
        double tendencia = 0.0;
        if (valores.size() >= 2) {
            double ultimo = valores.get(valores.size() - 1);
            double penultimo = valores.get(valores.size() - 2);
            tendencia = (ultimo - penultimo) * 0.5;
        }

        double previsaoProximoMes = Math.max(0.0, (valores.isEmpty() ? 0 : valores.get(valores.size() - 1)) + tendencia);

        Map<String, Object> res = new HashMap<>();
        res.put("mediaMensalHistorica", mediaHistorica);
        res.put("previsaoProximoMes", previsaoProximoMes);
        res.put("tendencia", tendencia >= 0 ? "CRESCIMENTO (" + String.format("%.1f", (previsaoProximoMes - mediaHistorica)/Math.max(1, mediaHistorica)*100) + "%)" : "QUEDA");
        res.put("historico6Meses", valores);
        return res;
    }

    // --- 2. Previsão de Ruptura de Stock ---
    public List<Map<String, Object>> preverRupturaStock(Long empresaId) {
        List<Produto> produtos = (empresaId == null) ? produtoRepository.findAll() : produtoRepository.findByEmpresa_Id(empresaId);
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);

        LocalDate trintaDiasAtras = LocalDate.now().minusDays(30);

        Map<Long, Integer> vendasPorProduto = new HashMap<>();
        for (Compra c : compras) {
            if (c.getDataCompra() != null && c.getDataCompra().toLocalDate().isAfter(trintaDiasAtras) && !"CANCELADA".equalsIgnoreCase(c.getStatus())) {
                if (c.getItens() != null) {
                    for (ItemCompra item : c.getItens()) {
                        if (item.getProduto() != null) {
                            Long pid = item.getProduto().getId();
                            vendasPorProduto.put(pid, vendasPorProduto.getOrDefault(pid, 0) + (item.getQuantidade() != null ? item.getQuantidade() : 0));
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> listaRisco = new ArrayList<>();
        for (Produto p : produtos) {
            int qtdVendida30Dias = vendasPorProduto.getOrDefault(p.getId(), 0);
            double consumoDiario = (double) qtdVendida30Dias / 30.0;

            int stockAtual = (p.getQuantidadeEstoque() != null ? p.getQuantidadeEstoque().intValue() : 0);
            double minEstoque = (p.getEstoqueMinimo() != null ? p.getEstoqueMinimo() : 5.0);
            
            if (consumoDiario > 0 || stockAtual <= minEstoque) {
                int diasRestantes = (consumoDiario > 0) ? (int) Math.round(stockAtual / consumoDiario) : 0;
                
                if (diasRestantes <= 15 || stockAtual <= 5) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("produtoId", p.getId());
                    item.put("nomeProduto", p.getNome());
                    item.put("stockAtual", stockAtual);
                    item.put("consumoDiarioEstimado", String.format("%.1f", consumoDiario));
                    item.put("diasAteRuptura", diasRestantes);
                    item.put("nivelRisco", diasRestantes <= 5 ? "CRÍTICO" : (diasRestantes <= 10 ? "ALTO" : "MÉDIO"));
                    item.put("sugestaoRecompra", Math.max(20, (int)(consumoDiario * 30)));
                    listaRisco.add(item);
                }
            }
        }

        listaRisco.sort((a, b) -> Integer.compare((Integer) a.get("diasAteRuptura"), (Integer) b.get("diasAteRuptura")));
        return listaRisco;
    }

    // --- 3. Deteção de Clientes com Risco de Inadimplência ---
    public List<Map<String, Object>> detectarClientesRiscoInadimplencia(Long empresaId) {
        List<Cliente> clientes = (empresaId == null) ? clienteRepository.findAll() : clienteRepository.findByEmpresa_Id(empresaId);
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);

        List<Map<String, Object>> resultado = new ArrayList<>();
        LocalDate hoje = LocalDate.now();

        for (Cliente c : clientes) {
            List<Fatura> faturasCliente = faturas.stream()
                    .filter(f -> f.getCompra() != null && f.getCompra().getCliente() != null && f.getCompra().getCliente().getId().equals(c.getId()))
                    .collect(Collectors.toList());

            long faturasPendentes = faturasCliente.stream().filter(f -> "PENDENTE".equalsIgnoreCase(f.getStatus())).count();
            double valorTotalPendente = faturasCliente.stream()
                    .filter(f -> "PENDENTE".equalsIgnoreCase(f.getStatus()))
                    .mapToDouble(f -> f.getTotal() != null ? f.getTotal() : 0.0).sum();

            if (faturasPendentes > 0 || valorTotalPendente > 0) {
                long diasMaiorAtraso = 0;
                for (Fatura f : faturasCliente) {
                    if ("PENDENTE".equalsIgnoreCase(f.getStatus()) && f.getDataVencimento() != null) {
                        LocalDate venc = new java.sql.Date(f.getDataVencimento().getTime()).toLocalDate();
                        if (hoje.isAfter(venc)) {
                            long atraso = ChronoUnit.DAYS.between(venc, hoje);
                            if (atraso > diasMaiorAtraso) diasMaiorAtraso = atraso;
                        }
                    }
                }

                String nivelRisco = "BAIXO";
                if (diasMaiorAtraso > 60 || valorTotalPendente > 500000) nivelRisco = "ALTO (CRÍTICO)";
                else if (diasMaiorAtraso > 30 || faturasPendentes >= 3) nivelRisco = "MÉDIO";

                Map<String, Object> dto = new HashMap<>();
                dto.put("clienteId", c.getId());
                dto.put("nomeCliente", c.getNome());
                dto.put("nif", c.getNif() != null ? c.getNif() : "Consumidor Final");
                dto.put("faturasPendentes", faturasPendentes);
                dto.put("valorTotalPendente", valorTotalPendente);
                dto.put("diasMaiorAtraso", diasMaiorAtraso);
                dto.put("nivelRisco", nivelRisco);
                dto.put("recomendacao", diasMaiorAtraso > 30 ? "Bloquear vendas a crédito e emitir aviso de cobrança" : "Enviar lembrete amigável");

                resultado.add(dto);
            }
        }

        resultado.sort((a, b) -> Double.compare((Double) b.get("valorTotalPendente"), (Double) a.get("valorTotalPendente")));
        return resultado;
    }

    // --- 4. Sugestão de Preços Dinâmicos com base no Histórico ---
    public List<Map<String, Object>> sugerirPrecosProdutos(Long empresaId) {
        List<Produto> produtos = (empresaId == null) ? produtoRepository.findAll() : produtoRepository.findByEmpresa_Id(empresaId);
        List<Map<String, Object>> sugestoes = new ArrayList<>();

        for (Produto p : produtos) {
            double precoCompra = (p.getPrecoCompra() != null ? p.getPrecoCompra() : 0.0);
            double precoVendaAtual = p.getPreco();

            if (precoCompra > 0) {
                double margemAtual = ((precoVendaAtual - precoCompra) / precoCompra) * 100;
                
                double precoSugerido = precoCompra * 1.35;
                double margemSugerida = 35.0;

                Map<String, Object> item = new HashMap<>();
                item.put("produtoId", p.getId());
                item.put("nomeProduto", p.getNome());
                item.put("precoCompra", precoCompra);
                item.put("precoVendaAtual", precoVendaAtual);
                item.put("margemAtualFormatada", String.format("%.1f%%", margemAtual));
                item.put("precoSugerido", precoSugerido);
                item.put("margemSugeridaFormatada", String.format("%.1f%%", margemSugerida));
                item.put("diferenca", precoSugerido - precoVendaAtual);
                item.put("acaoRecomendada", margemAtual < 15 ? "Aumentar preço para garantir rentabilidade" : (margemAtual > 60 ? "Preço elevado - considerar promoção" : "Manter preço competitivo"));

                sugestoes.add(item);
            }
        }
        return sugestoes;
    }

    // --- 5. Sugestão Inteligente de Promoções ---
    public List<Map<String, Object>> sugerirPromocoes(Long empresaId) {
        List<Produto> produtos = (empresaId == null) ? produtoRepository.findAll() : produtoRepository.findByEmpresa_Id(empresaId);
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);

        LocalDate trintaDiasAtras = LocalDate.now().minusDays(30);

        Map<Long, Integer> vendas30Dias = new HashMap<>();
        for (Compra c : compras) {
            if (c.getDataCompra() != null && c.getDataCompra().toLocalDate().isAfter(trintaDiasAtras) && !"CANCELADA".equalsIgnoreCase(c.getStatus())) {
                if (c.getItens() != null) {
                    for (ItemCompra item : c.getItens()) {
                        if (item.getProduto() != null) {
                            Long pid = item.getProduto().getId();
                            vendas30Dias.put(pid, vendas30Dias.getOrDefault(pid, 0) + (item.getQuantidade() != null ? item.getQuantidade() : 0));
                        }
                    }
                }
            }
        }

        List<Map<String, Object>> promocoes = new ArrayList<>();
        for (Produto p : produtos) {
            int stock = (p.getQuantidadeEstoque() != null ? p.getQuantidadeEstoque().intValue() : 0);
            int vendas = vendas30Dias.getOrDefault(p.getId(), 0);

            if (stock >= 20 && vendas <= 3) {
                double precoAtual = p.getPreco();
                double precoComDesconto = precoAtual * 0.85;

                Map<String, Object> promo = new HashMap<>();
                promo.put("produtoId", p.getId());
                promo.put("nomeProduto", p.getNome());
                promo.put("stockParado", stock);
                promo.put("vendasUltimos30Dias", vendas);
                promo.put("precoAtual", precoAtual);
                promo.put("descontoSugerido", "15%");
                promo.put("precoPromocional", precoComDesconto);
                promo.put("motivo", "Stock estagnado com baixa rotação nos últimos 30 dias.");

                promocoes.add(promo);
            }
        }

        promocoes.sort((a, b) -> Integer.compare((Integer) b.get("stockParado"), (Integer) a.get("stockParado")));
        return promocoes;
    }

    private static class YearMonthKey implements Comparable<YearMonthKey> {
        int year, month;
        YearMonthKey(int year, int month) { this.year = year; this.month = month; }
        @Override
        public int compareTo(YearMonthKey o) {
            if (this.year != o.year) return Integer.compare(this.year, o.year);
            return Integer.compare(this.month, o.month);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof YearMonthKey)) return false;
            YearMonthKey that = (YearMonthKey) o;
            return year == that.year && month == that.month;
        }
        @Override
        public int hashCode() { return Objects.hash(year, month); }
    }
}
