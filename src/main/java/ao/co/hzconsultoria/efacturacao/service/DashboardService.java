package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.repository.DespesaRepository;
import ao.co.hzconsultoria.efacturacao.model.Cliente;
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
    @Autowired
    private FaturaRepository faturaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private DespesaRepository despesaRepository;

    public List<ProdutoMaisVendidoDTO> getProdutosMaisVendidos(Long empresaId, int limite) {
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
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

    public Map<String, Integer> getVendasUltimos30Dias(Long empresaId) 
    {
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
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

    public int getVendasDia(Long empresaId) {
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int total = 0;
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null && compra.getDataCompra().toLocalDate().equals(hoje)) {
                total++;
            }
        }
        return total;
    }

    public double getReceitaDia(Long empresaId) {
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        double receita = 0.0;
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null && compra.getDataCompra().toLocalDate().equals(hoje)) {
                receita += (compra.getTotal() != null ? compra.getTotal() : 0.0);
            }
        }
        return receita;
    }

    public double getTotalIvaMes(Long empresaId) {
        List<Fatura> faturas = faturaRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        double totalIva = 0.0;
        for (Fatura fatura : faturas) {
            if (fatura.getDataEmissao() != null) {
                java.time.LocalDate data = new java.sql.Date(fatura.getDataEmissao().getTime()).toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual) {
                    totalIva += (fatura.getIva() != null ? fatura.getIva() : 0.0);
                }
            }
        }
        return totalIva;
    }

    public long getTotalFacturasMes(Long empresaId) {
        List<Fatura> faturas = faturaRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        long contagem = 0;
        for (Fatura fatura : faturas) {
            if (fatura.getDataEmissao() != null) {
                java.time.LocalDate data = new java.sql.Date(fatura.getDataEmissao().getTime()).toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual) {
                    contagem++;
                }
            }
        }
        return contagem;
    }

    public long getTotalPagamentosMes(Long empresaId) {
        return getComprasDoMes(empresaId).size();
    }

    public long getTotalClientes(Long empresaId) {
        return clienteRepository.findByEmpresa_Id(empresaId).size();
    }

    public long getTotalPendentes(Long empresaId) {
        List<Fatura> faturas = faturaRepository.findByEmpresa_Id(empresaId);
        return faturas.stream().filter(f -> "PENDENTE".equalsIgnoreCase(f.getStatus())).count();
    }

    public double getLucroMensal(Long empresaId) {
        double receita = getReceitaMensal(empresaId);
        double despesas = getDespesasMensais(empresaId);
        return receita - despesas;
    }

    public double getLucroBrutoMensal(Long empresaId) {
        List<Compra> vendasDoMes = getComprasDoMes(empresaId);
        double receitaMensal = 0.0;
        double cogs = 0.0;
        
        for (Compra compra : vendasDoMes) {
            receitaMensal += (compra.getTotal() != null ? compra.getTotal() : 0.0);
            if (compra.getItens() != null) {
                for (ItemCompra item : compra.getItens()) {
                    double precoCompra = 0.0;
                    if (item.getProduto() != null && item.getProduto().getPrecoCompra() != null) {
                        precoCompra = item.getProduto().getPrecoCompra();
                    }
                    cogs += (precoCompra * (item.getQuantidade() != null ? item.getQuantidade() : 0));
                }
            }
        }
        return receitaMensal - cogs;
    }

    public double getLucroTotal(Long empresaId) {
        double receitaTotal = compraRepository.findByEmpresa_Id(empresaId).stream()
                .mapToDouble(c -> c.getTotal() != null ? c.getTotal() : 0.0)
                .sum();
        double despesasTotais = despesaRepository.findByEmpresa_Id(empresaId).stream()
                .mapToDouble(d -> d.getValor() != null ? d.getValor() : 0.0)
                .sum();
        return receitaTotal - despesasTotais;
    }

    public double getDespesasMensais(Long empresaId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicio = hoje.withDayOfMonth(1);
        java.time.LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());
        List<Despesa> despesas = despesaRepository.findByDataDespesaBetween(inicio, fim).stream()
                .filter(d -> d.getEmpresa() != null && d.getEmpresa().getId().equals(empresaId))
                .collect(Collectors.toList());
        return despesas.stream().mapToDouble(d -> d.getValor() != null ? d.getValor() : 0.0).sum();
    }

    public Map<String, Object> getReceitaVsDespesaData(Long empresaId) {
        Map<String, Object> data = new HashMap<>();
        List<String> labels = new ArrayList<>();
        List<Double> receitas = new ArrayList<>();
        List<Double> despesasValues = new ArrayList<>();

        java.time.LocalDate hoje = java.time.LocalDate.now();
        for (int i = 5; i >= 0; i--) {
            java.time.LocalDate mes = hoje.minusMonths(i);
            String label = mes.getMonth().name() + " " + mes.getYear();
            labels.add(label);

            java.time.LocalDate inicio = mes.withDayOfMonth(1);
            java.time.LocalDate fim = mes.withDayOfMonth(mes.lengthOfMonth());

            // Receitas
            List<Compra> comprasMes = compraRepository.findByEmpresa_Id(empresaId).stream()
                .filter(c -> c.getDataCompra() != null && !c.getDataCompra().toLocalDate().isBefore(inicio) && !c.getDataCompra().toLocalDate().isAfter(fim))
                .collect(Collectors.toList());
            receitas.add(comprasMes.stream().mapToDouble(c -> c.getTotal() != null ? c.getTotal() : 0.0).sum());

            // Despesas
            List<Despesa> despesasMes = despesaRepository.findByDataDespesaBetween(inicio, fim).stream()
                .filter(d -> d.getEmpresa() != null && d.getEmpresa().getId().equals(empresaId))
                .collect(Collectors.toList());
            despesasValues.add(despesasMes.stream().mapToDouble(d -> d.getValor() != null ? d.getValor() : 0.0).sum());
        }

        data.put("labels", labels);
        data.put("receitas", receitas);
        data.put("despesas", despesasValues);
        return data;
    }

    public Map<String, Object> getComparacaoPeriodosData(Long empresaId) {
        Map<String, Object> data = new HashMap<>();
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate mesPassado = hoje.minusMonths(1);

        List<Integer> atual = new ArrayList<>();
        List<Integer> passado = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        List<Compra> comprasDaEmpresa = compraRepository.findByEmpresa_Id(empresaId);

        for (int i = 1; i <= 31; i++) {
            labels.add(String.valueOf(i));
            final int dia = i;
            
            long countAtual = comprasDaEmpresa.stream()
                .filter(c -> c.getDataCompra() != null && c.getDataCompra().toLocalDate().getMonthValue() == hoje.getMonthValue() && c.getDataCompra().toLocalDate().getDayOfMonth() == dia)
                .count();
            atual.add((int) countAtual);

            long countPassado = comprasDaEmpresa.stream()
                .filter(c -> c.getDataCompra() != null && c.getDataCompra().toLocalDate().getMonthValue() == mesPassado.getMonthValue() && c.getDataCompra().toLocalDate().getDayOfMonth() == dia)
                .count();
            passado.add((int) countPassado);
        }

        data.put("labels", labels);
        data.put("atual", atual);
        data.put("passado", passado);
        return data;
    }

    public Map<String, Long> getVendasPorLocalizacao(Long empresaId) {
        List<Cliente> clientes = clienteRepository.findByEmpresa_Id(empresaId);
        Map<String, Long> localizacoes = clientes.stream()
            .filter(c -> c.getEndereco() != null && !c.getEndereco().isEmpty())
            .map(c -> {
                String end = c.getEndereco().split(",")[0].split(" ")[0].trim();
                return end.substring(0, 1).toUpperCase() + end.substring(1).toLowerCase();
            })
            .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return localizacoes;
    }

    public Map<Integer, Long> getHorariosPicoVendas(Long empresaId) {
        Map<Integer, Long> pico = new TreeMap<>();
        for (int i = 0; i < 24; i++) pico.put(i, 0L);

        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
        for (Compra c : compras) {
            if (c.getDataCompra() != null) {
                int hora = c.getDataCompra().getHour();
                pico.put(hora, pico.get(hora) + 1);
            }
        }
        return pico;
    }

    public double getReceitaMensal(Long empresaId) {
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
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

    public List<Produto> getProdutosEstoqueBaixo(Long empresaId, int limiteEstoque) {
        List<Produto> produtos = produtoRepository.findByEmpresa_Id(empresaId);
        List<Produto> estoqueBaixo = new ArrayList<>();
        for (Produto produto : produtos) {
            if (produto.getQuantidadeEstoque() <= (produto.getEstoqueMinimo() != null ? produto.getEstoqueMinimo() : limiteEstoque)) {
                estoqueBaixo.add(produto);
            }
        }
        return estoqueBaixo;
    }

    public List<Compra> getComprasDoDia(Long empresaId) {
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        List<Compra> vendasDoDia = new ArrayList<>();
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null && compra.getDataCompra().toLocalDate().equals(hoje)) {
                vendasDoDia.add(compra);
            }
        }
        return vendasDoDia;
    }

    public List<Compra> getComprasDoMes(Long empresaId) {
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
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

    public long getVendasTotaisMes(Long empresaId) {
        return getComprasDoMes(empresaId).size();
    }

    public long getProdutosVendidosCount(Long empresaId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        long total = 0;
        for (Compra compra : compraRepository.findByEmpresa_Id(empresaId)) {
            if (compra.getDataCompra() != null) {
                java.time.LocalDate data = compra.getDataCompra().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual && compra.getItens() != null) {
                    total += compra.getItens().stream().mapToLong(i -> i.getQuantidade() != null ? i.getQuantidade() : 0).sum();
                }
            }
        }
        return total;
    }

    public long getVendasMesAnterior(Long empresaId) {
        java.time.LocalDate mesAnterior = java.time.LocalDate.now().minusMonths(1);
        int mes = mesAnterior.getMonthValue();
        int ano = mesAnterior.getYear();
        return compraRepository.findByEmpresa_Id(empresaId).stream()
            .filter(c -> c.getDataCompra() != null
                && c.getDataCompra().toLocalDate().getMonthValue() == mes
                && c.getDataCompra().toLocalDate().getYear() == ano)
            .count();
    }

    public double getReceitaMesAnterior(Long empresaId) {
        java.time.LocalDate mesAnterior = java.time.LocalDate.now().minusMonths(1);
        int mes = mesAnterior.getMonthValue();
        int ano = mesAnterior.getYear();
        return compraRepository.findByEmpresa_Id(empresaId).stream()
            .filter(c -> c.getDataCompra() != null
                && c.getDataCompra().toLocalDate().getMonthValue() == mes
                && c.getDataCompra().toLocalDate().getYear() == ano)
            .mapToDouble(c -> c.getTotal() != null ? c.getTotal() : 0.0)
            .sum();
    }

    public String getVariacaoVendas(Long empresaId) {
        long atual = getVendasTotaisMes(empresaId);
        long anterior = getVendasMesAnterior(empresaId);
        if (anterior == 0) return "+0%";
        double variacao = ((double)(atual - anterior) / anterior) * 100;
        return String.format("%+.1f%%", variacao);
    }

    public String getVariacaoReceita(Long empresaId) {
        double atual = getReceitaMensal(empresaId);
        double anterior = getReceitaMesAnterior(empresaId);
        if (anterior == 0) return "+0%";
        double variacao = ((atual - anterior) / anterior) * 100;
        return String.format("%+.1f%%", variacao);
    }

    public List<ClienteTopComprasDTO> getClientesTopCompras(Long empresaId, int limite) {
        List<ClienteTopComprasDTO> lista = new ArrayList<>();
        List<Compra> compras = compraRepository.findByEmpresa_Id(empresaId);
        for (Compra compra : compras) {
            lista.add(new ClienteTopComprasDTO(compra.getId(), "Venda #" + compra.getId(), 1));
        }
        return lista.stream().limit(limite).collect(Collectors.toList());
    }

    public List<Long> getVendasPorMes(Long empresaId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        List<Long> resultado = new ArrayList<>();
        List<Compra> comprasDaEmpresa = compraRepository.findByEmpresa_Id(empresaId);
        for (int i = 11; i >= 0; i--) {
            java.time.LocalDate mes = hoje.minusMonths(i);
            int m = mes.getMonthValue();
            int a = mes.getYear();
            long count = comprasDaEmpresa.stream()
                .filter(c -> c.getDataCompra() != null
                    && c.getDataCompra().toLocalDate().getMonthValue() == m
                    && c.getDataCompra().toLocalDate().getYear() == a)
                .count();
            resultado.add(count);
        }
        return resultado;
    }

    public List<Double> getReceitaPorMes(Long empresaId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        List<Double> resultado = new ArrayList<>();
        List<Compra> comprasDaEmpresa = compraRepository.findByEmpresa_Id(empresaId);
        for (int i = 11; i >= 0; i--) {
            java.time.LocalDate mes = hoje.minusMonths(i);
            int m = mes.getMonthValue();
            int a = mes.getYear();
            double receita = comprasDaEmpresa.stream()
                .filter(c -> c.getDataCompra() != null
                    && c.getDataCompra().toLocalDate().getMonthValue() == m
                    && c.getDataCompra().toLocalDate().getYear() == a)
                .mapToDouble(c -> c.getTotal() != null ? c.getTotal() : 0.0)
                .sum();
            resultado.add(receita);
        }
        return resultado;
    }

    public List<Long> getNovoClientesPorMes(Long empresaId) {
        List<Long> resultado = new ArrayList<>();
        for (int i = 0; i < 12; i++) resultado.add(0L);
        return resultado;
    }

    public List<String> getUltimos12MesesLabels() {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        List<String> labels = new ArrayList<>();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MMM/yy", java.util.Locale.forLanguageTag("pt"));
        for (int i = 11; i >= 0; i--) {
            labels.add(hoje.minusMonths(i).format(fmt));
        }
        return labels;
    }

    public static class ClienteTopComprasDTO {
        private Long id;
        private String nome;
        private int totalCompras;
        public ClienteTopComprasDTO(Long id, String nome, int totalCompras) {
            this.id = id; this.nome = nome; this.totalCompras = totalCompras;
        }
        public Long getId() { return id; }
        public String getNome() { return nome; }
        public int getTotalCompras() { return totalCompras; }
        public void setTotalCompras(int totalCompras) { this.totalCompras = totalCompras; }
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