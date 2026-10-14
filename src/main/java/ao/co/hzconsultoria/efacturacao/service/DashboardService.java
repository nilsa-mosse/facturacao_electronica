package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import ao.co.hzconsultoria.efacturacao.repository.DevolucaoRepository;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.repository.DespesaRepository;
import ao.co.hzconsultoria.efacturacao.model.Cliente;
import ao.co.hzconsultoria.efacturacao.dto.RecebimentoDashboardDTO.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.io.File;

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
    @Autowired
    private DevolucaoRepository devolucaoRepository;

    public List<ProdutoMaisVendidoDTO> getProdutosMaisVendidos(Long empresaId, int limite) {
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
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
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
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
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
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
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        double receita = 0.0;
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null && compra.getDataCompra().toLocalDate().equals(hoje) && !"CANCELADA".equalsIgnoreCase(compra.getStatus())) {
                double total = (compra.getTotal() != null ? compra.getTotal() : 0.0);
                receita += total;
            }
        }
        
        // Subtrair devoluções do dia
        List<Devolucao> devolucoes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        for (Devolucao d : devolucoes) {
            if (d.getDataDevolucao() != null && d.getDataDevolucao().toLocalDate().equals(hoje)) {
                double totalDevolucao = (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0);
                receita -= totalDevolucao;
            }
        }
        
        return receita;
    }

    public double getTotalIvaMes(Long empresaId) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);
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
        
        // Subtrair IVA de devoluções (Notas de Crédito)
        List<Devolucao> devolucoes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        for (Devolucao d : devolucoes) {
            if (d.getDataDevolucao() != null) {
                java.time.LocalDate data = d.getDataDevolucao().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual) {
                    totalIva -= (d.getIva() != null ? d.getIva() : 0.0);
                }
            }
        }
        
        return totalIva;
    }

    public long getTotalFacturasMes(Long empresaId) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);
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
        return (empresaId == null) ? clienteRepository.count() : clienteRepository.findByEmpresa_Id(empresaId).size();
    }

    public long getTotalPendentes(Long empresaId) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);
        return faturas.stream().filter(f -> "PENDENTE".equalsIgnoreCase(f.getStatus())).count();
    }

    public double getLucroMensal(Long empresaId) {
        double receita = getReceitaMensal(empresaId);
        double despesas = getDespesasMensais(empresaId);
        // O lucro líquido mensal já considera receita líquida (sem IVA) - despesas
        return receita - despesas;
    }

    public double getLucroBrutoMensal(Long empresaId) {
        List<Compra> vendasDoMes = getComprasDoMes(empresaId);
        double receitaMensal = 0.0;
        double cogs = 0.0;
        
        for (Compra compra : vendasDoMes) {
            if (!"CANCELADA".equalsIgnoreCase(compra.getStatus())) {
                double total = (compra.getTotal() != null ? compra.getTotal() : 0.0);
                receitaMensal += total;
                
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
        }

        double totalDevolucoes = 0.0;
        List<Devolucao> devolucoesMes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        for (Devolucao d : devolucoesMes) {
            if (d.getDataDevolucao() != null && d.getDataDevolucao().toLocalDate().getMonthValue() == hoje.getMonthValue()) {
                totalDevolucoes += (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0);
            }
        }

        return receitaMensal - cogs - totalDevolucoes;
    }

    public double getLucroTotal(Long empresaId) {
        List<Compra> todasCompras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        double receitaTotal = todasCompras.stream()
                .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getStatus()))
                .mapToDouble(c -> (c.getTotal() != null ? c.getTotal() : 0.0))
                .sum();
        
        double totalDevolucoes = (empresaId == null) ? devolucaoRepository.findAll().stream().mapToDouble(d -> (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0)).sum()
                                                   : devolucaoRepository.findByEmpresa_Id(empresaId).stream().mapToDouble(d -> (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0)).sum();
        
        double despesasTotais = despesaRepository.findAll().stream()
                .filter(d -> d.getEmpresa() == null || (empresaId != null && d.getEmpresa().getId().equals(empresaId)))
                .mapToDouble(d -> d.getValor() != null ? d.getValor() : 0.0)
                .sum();
        return receitaTotal - totalDevolucoes - despesasTotais;
    }

    public long getTotalMovimentos(Long empresaId) {
        long vendasCount = (empresaId == null) ? compraRepository.count() : compraRepository.findByEmpresa_Id(empresaId).size();
        long despesasCount = despesaRepository.findAll().stream()
                .filter(d -> d.getEmpresa() == null || (empresaId != null && d.getEmpresa().getId().equals(empresaId)))
                .count();
        return vendasCount + despesasCount;
    }


    public double getDespesasMensais(Long empresaId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicio = hoje.withDayOfMonth(1);
        java.time.LocalDate fim = hoje.withDayOfMonth(hoje.lengthOfMonth());
        List<Despesa> todasDespesas = despesaRepository.findAll().stream()
                .filter(d -> d.getEmpresa() == null || (empresaId != null && d.getEmpresa().getId().equals(empresaId)))
                .collect(Collectors.toList());
        List<Despesa> despesas = todasDespesas.stream()
                .filter(d -> d.getDataDespesa() != null
                    && !d.getDataDespesa().isBefore(inicio)
                    && !d.getDataDespesa().isAfter(fim))
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

            // Receitas (Vendas Brutas - Devoluções Brutas)
            List<Compra> comprasMes = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
            double receitaMesVal = comprasMes.stream()
                .filter(c -> c.getDataCompra() != null && !"CANCELADA".equalsIgnoreCase(c.getStatus()) && !c.getDataCompra().toLocalDate().isBefore(inicio) && !c.getDataCompra().toLocalDate().isAfter(fim))
                .mapToDouble(c -> (c.getTotal() != null ? c.getTotal() : 0.0)).sum();
            
            List<Devolucao> devolucoesMesChart = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
            double devolucoesMesVal = devolucoesMesChart.stream()
                .filter(d -> d.getDataDevolucao() != null && !d.getDataDevolucao().toLocalDate().isBefore(inicio) && !d.getDataDevolucao().toLocalDate().isAfter(fim))
                .mapToDouble(d -> (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0)).sum();
            
            receitas.add(receitaMesVal - devolucoesMesVal);

            // Despesas
            List<Despesa> despesasMes = despesaRepository.findAll().stream()
                .filter(d -> (d.getEmpresa() == null || (empresaId != null && d.getEmpresa().getId().equals(empresaId)))
                    && d.getDataDespesa() != null
                    && !d.getDataDespesa().isBefore(inicio)
                    && !d.getDataDespesa().isAfter(fim))
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

        List<Compra> comprasDaEmpresa = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);

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
        List<Cliente> clientes = (empresaId == null) ? clienteRepository.findAll() : clienteRepository.findByEmpresa_Id(empresaId);
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

        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        for (Compra c : compras) {
            if (c.getDataCompra() != null) {
                int hora = c.getDataCompra().getHour();
                pico.put(hora, pico.get(hora) + 1);
            }
        }
        return pico;
    }

    public double getReceitaMensal(Long empresaId) {
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        double receita = 0.0;
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null && !"CANCELADA".equalsIgnoreCase(compra.getStatus())) {
                java.time.LocalDate data = compra.getDataCompra().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual) {
                    double total = (compra.getTotal() != null ? compra.getTotal() : 0.0);
                    receita += total;
                }
            }
        }
        
        // Subtrair devoluções do mês
        List<Devolucao> devolucoes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        for (Devolucao d : devolucoes) {
            if (d.getDataDevolucao() != null) {
                java.time.LocalDate data = d.getDataDevolucao().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual) {
                    double totalDevolucao = (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0);
                    receita -= totalDevolucao;
                }
            }
        }
        
        return receita;
    }

    public List<Produto> getProdutosEstoqueBaixo(Long empresaId, int limiteEstoque) {
        // Agora usamos a query optimizada do repositório que já trata o limite de risco (5) quando o mínimo é 0
        return produtoRepository.findProdutosComStockBaixo(empresaId);
    }

    public List<Compra> getComprasDoDia(Long empresaId) {
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
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
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
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
        long vendasCount = getComprasDoMes(empresaId).size();
        
        // Subtrair contagem de devoluções do mês
        long devolucoesCount = 0;
        List<Devolucao> devolucoes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        for (Devolucao d : devolucoes) {
            if (d.getDataDevolucao() != null && d.getDataDevolucao().toLocalDate().getMonthValue() == hoje.getMonthValue()) {
                devolucoesCount++;
            }
        }
        
        return vendasCount - devolucoesCount;
    }

    public long getProdutosVendidosCount(Long empresaId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        int mesAtual = hoje.getMonthValue();
        int anoAtual = hoje.getYear();
        long total = 0;
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        for (Compra compra : compras) {
            if (compra.getDataCompra() != null) {
                java.time.LocalDate data = compra.getDataCompra().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual && compra.getItens() != null) {
                    total += compra.getItens().stream().mapToLong(i -> i.getQuantidade() != null ? i.getQuantidade() : 0).sum();
                }
            }
        }
        
        // Subtrair quantidades devolvidas
        List<Devolucao> devolucoes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        for (Devolucao d : devolucoes) {
            if (d.getDataDevolucao() != null) {
                java.time.LocalDate data = d.getDataDevolucao().toLocalDate();
                if (data.getMonthValue() == mesAtual && data.getYear() == anoAtual && d.getItens() != null) {
                    total -= d.getItens().stream().mapToLong(i -> i.getQuantidade() != null ? (long)i.getQuantidade() : 0L).sum();
                }
            }
        }
        
        return total;
    }

    public long getVendasMesAnterior(Long empresaId) {
        java.time.LocalDate mesAnterior = java.time.LocalDate.now().minusMonths(1);
        int mes = mesAnterior.getMonthValue();
        int ano = mesAnterior.getYear();
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        long vendas = compras.stream()
            .filter(c -> c.getDataCompra() != null
                && c.getDataCompra().toLocalDate().getMonthValue() == mes
                && c.getDataCompra().toLocalDate().getYear() == ano)
            .count();
            
        List<Devolucao> devolucoes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        long devs = devolucoes.stream()
            .filter(d -> d.getDataDevolucao() != null
                && d.getDataDevolucao().toLocalDate().getMonthValue() == mes
                && d.getDataDevolucao().toLocalDate().getYear() == ano)
            .count();
            
        return vendas - devs;
    }

    public double getReceitaMesAnterior(Long empresaId) {
        java.time.LocalDate mesAnterior = java.time.LocalDate.now().minusMonths(1);
        int mes = mesAnterior.getMonthValue();
        int ano = mesAnterior.getYear();
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        double receita = compras.stream()
            .filter(c -> c.getDataCompra() != null && !"CANCELADA".equalsIgnoreCase(c.getStatus())
                && c.getDataCompra().toLocalDate().getMonthValue() == mes
                && c.getDataCompra().toLocalDate().getYear() == ano)
            .mapToDouble(c -> (c.getTotal() != null ? c.getTotal() : 0.0))
            .sum();
            
        List<Devolucao> devolucoes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        double devs = devolucoes.stream()
            .filter(d -> d.getDataDevolucao() != null
                && d.getDataDevolucao().toLocalDate().getMonthValue() == mes
                && d.getDataDevolucao().toLocalDate().getYear() == ano)
            .mapToDouble(d -> (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0))
            .sum();
            
        return receita - devs;
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
        List<Compra> compras = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        for (Compra compra : compras) {
            lista.add(new ClienteTopComprasDTO(compra.getId(), "Venda #" + compra.getId(), 1));
        }
        return lista.stream().limit(limite).collect(Collectors.toList());
    }

    public List<Long> getVendasPorMes(Long empresaId) {
        java.time.LocalDate hoje = java.time.LocalDate.now();
        List<Long> resultado = new ArrayList<>();
        List<Compra> comprasDaEmpresa = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
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
        List<Compra> comprasDaEmpresa = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);
        for (int i = 11; i >= 0; i--) {
            java.time.LocalDate mes = hoje.minusMonths(i);
            int m = mes.getMonthValue();
            int a = mes.getYear();
            double receita = comprasDaEmpresa.stream()
                .filter(c -> c.getDataCompra() != null && !"CANCELADA".equalsIgnoreCase(c.getStatus())
                    && c.getDataCompra().toLocalDate().getMonthValue() == m
                    && c.getDataCompra().toLocalDate().getYear() == a)
                .mapToDouble(c -> (c.getTotal() != null ? c.getTotal() : 0.0))
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

    // =========================================================================
    // MÉTODOS DE RECEBIMENTOS E CONTAS A RECEBER
    // =========================================================================

    public LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        try {
            return new java.sql.Date(date.getTime()).toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isDateInPeriod(Date date, LocalDate inicio, LocalDate fim) {
        if (date == null) return false;
        LocalDate ld = toLocalDate(date);
        if (ld == null) return false;
        if (inicio != null && ld.isBefore(inicio)) return false;
        if (fim != null && ld.isAfter(fim)) return false;
        return true;
    }

    public boolean isSameDay(Date date, LocalDate target) {
        if (date == null || target == null) return false;
        LocalDate ld = toLocalDate(date);
        return target.equals(ld);
    }

    public String normalizarMetodo(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "CASH";
        String upper = raw.trim().toUpperCase();
        if (upper.contains("CASH") || upper.contains("DINHEIRO") || upper.contains("NUMER") || upper.contains("ESPÉCIE") || upper.contains("ESPECIE")) {
            return "CASH";
        }
        if (upper.contains("TPA") || upper.contains("MULTI") || upper.contains("CART") || upper.contains("CARD")) {
            return "TPA";
        }
        if (upper.contains("TRANS") || upper.contains("BANC") || upper.contains("DEP") || upper.contains("IBAN")) {
            return "TRANSFERENCIA";
        }
        return "OUTRO";
    }

    public RecebimentosResumoDTO getRecebimentosResumo(Long empresaId, LocalDate inicio, LocalDate fim) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        RecebimentosResumoDTO resumo = new RecebimentosResumoDTO();

        double somaRecebidoMes = 0.0;
        long countRecibosMes = 0;

        double somaRecebidoHoje = 0.0;
        long countRecibosHoje = 0;

        double somaRecebidoPeriodo = 0.0;
        long countRecibosPeriodo = 0;

        double somaEmAberto = 0.0;
        double somaVencido = 0.0;
        long countPendentes = 0;
        long countVencidas = 0;

        double totalFaturadoCredito = 0.0;
        double totalAmortizadoCredito = 0.0;

        for (Fatura f : faturas) {
            String status = f.getStatus() != null ? f.getStatus().toUpperCase() : "";
            if ("ANULADA".equals(status) || "CANCELADA".equals(status)) {
                continue;
            }

            String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().toUpperCase() : "";

            // 1. Recebimentos efectivados: Recibo Autónomo (RC) ou Factura-Recibo (FR)
            if ("RC".equals(tipo) || "FR".equals(tipo)) {
                double val = f.getTotal() != null ? f.getTotal() : 0.0;

                if (isDateInPeriod(f.getDataEmissao(), inicioMes, fimMes)) {
                    somaRecebidoMes += val;
                    countRecibosMes++;
                }

                if (isSameDay(f.getDataEmissao(), hoje)) {
                    somaRecebidoHoje += val;
                    countRecibosHoje++;
                }

                if (isDateInPeriod(f.getDataEmissao(), inicio, fim)) {
                    somaRecebidoPeriodo += val;
                    countRecibosPeriodo++;
                }
            }

            // 2. Contas a Receber: Facturas a Crédito (FT)
            if ("FT".equals(tipo) || tipo.isEmpty()) {
                double total = f.getTotal() != null ? f.getTotal() : 0.0;
                double pago = f.getValorPago() != null ? f.getValorPago() : 0.0;
                double aberto = f.getValorEmAberto() != null ? f.getValorEmAberto() : Math.max(0.0, total - pago);

                totalFaturadoCredito += total;
                totalAmortizadoCredito += pago;

                if (aberto > 0.01) {
                    somaEmAberto += aberto;
                    countPendentes++;

                    LocalDate dtVenc = toLocalDate(f.getDataVencimento());
                    if (dtVenc != null && dtVenc.isBefore(hoje)) {
                        somaVencido += aberto;
                        countVencidas++;
                    }
                }
            }
        }

        resumo.setTotalRecebidoMes(somaRecebidoMes);
        resumo.setQtdRecibosMes(countRecibosMes);

        resumo.setTotalRecebidoHoje(somaRecebidoHoje);
        resumo.setQtdRecibosHoje(countRecibosHoje);

        resumo.setTotalRecebidoPeriodo(somaRecebidoPeriodo);
        resumo.setQtdRecibosPeriodo(countRecibosPeriodo);

        resumo.setTotalEmAberto(somaEmAberto);
        resumo.setTotalVencido(somaVencido);
        resumo.setTotalAVencer(Math.max(0.0, somaEmAberto - somaVencido));
        resumo.setQtdContasPendentes(countPendentes);
        resumo.setQtdContasVencidas(countVencidas);

        if (totalFaturadoCredito > 0) {
            resumo.setTaxaCobranca((totalAmortizadoCredito / totalFaturadoCredito) * 100.0);
        } else {
            resumo.setTaxaCobranca(100.0);
        }

        if (countRecibosPeriodo > 0) {
            resumo.setTicketMedio(somaRecebidoPeriodo / countRecibosPeriodo);
        } else {
            resumo.setTicketMedio(0.0);
        }

        return resumo;
    }

    public List<RecebimentoItemDTO> getHistoricoRecebimentos(Long empresaId, LocalDate inicio, LocalDate fim) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);
        SimpleDateFormat sdfData = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat sdfHora = new SimpleDateFormat("HH:mm");

        List<RecebimentoItemDTO> resultado = new ArrayList<>();

        for (Fatura f : faturas) {
            String status = f.getStatus() != null ? f.getStatus().toUpperCase() : "";
            if ("ANULADA".equals(status) || "CANCELADA".equals(status)) {
                continue;
            }

            String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().toUpperCase() : "";
            if (!"RC".equals(tipo) && !"FR".equals(tipo)) {
                continue;
            }

            if (!isDateInPeriod(f.getDataEmissao(), inicio, fim)) {
                continue;
            }

            RecebimentoItemDTO item = new RecebimentoItemDTO();
            item.setId(f.getId());
            item.setNumeroDocumento(f.getNumeroFatura() != null ? f.getNumeroFatura() : ("#" + f.getId()));
            item.setTipoDocumento(tipo);
            item.setDataEmissao(f.getDataEmissao());
            item.setDataFormatada(f.getDataEmissao() != null ? sdfData.format(f.getDataEmissao()) : "-");
            item.setHoraFormatada(f.getDataEmissao() != null ? sdfHora.format(f.getDataEmissao()) : "-");

            // FT de Referência
            if (f.getFaturaReferencia() != null && f.getFaturaReferencia().getNumeroFatura() != null) {
                item.setFaturaReferenciaNumero(f.getFaturaReferencia().getNumeroFatura());
            } else if (f.getObservacoes() != null && !f.getObservacoes().trim().isEmpty()) {
                item.setFaturaReferenciaNumero(f.getObservacoes());
            } else {
                item.setFaturaReferenciaNumero("-");
            }

            // Cliente
            Compra compra = f.getCompra();
            if (compra != null && compra.getCliente() != null && compra.getCliente().getNome() != null) {
                item.setNomeCliente(compra.getCliente().getNome());
                item.setNifCliente(compra.getCliente().getNif() != null ? compra.getCliente().getNif() : "-");
            } else if (compra != null && compra.getNomeCliente() != null && !compra.getNomeCliente().trim().isEmpty()) {
                item.setNomeCliente(compra.getNomeCliente());
                item.setNifCliente(compra.getNifCliente() != null ? compra.getNifCliente() : "-");
            } else {
                item.setNomeCliente("Consumidor Final");
                item.setNifCliente("999999999");
            }

            // Forma de Pagamento
            String metodo = f.getFormaPagamento();
            if (metodo == null && compra != null) {
                metodo = compra.getFormaPagamento();
            }
            item.setFormaPagamento(normalizarMetodo(metodo));

            item.setValor(f.getTotal() != null ? f.getTotal() : 0.0);
            item.setStatus(f.getStatus() != null ? f.getStatus() : "EMITIDA");

            // Utilizador
            if (compra != null && compra.getUsuario() != null) {
                item.setUsuarioNome(compra.getUsuario().getNome());
            } else {
                item.setUsuarioNome("Sistema");
            }

            // PDF
            String nomeDoc = f.getNumeroFatura();
            if (nomeDoc != null) {
                File pdf = new File("./uploads/faturas/" + nomeDoc + ".pdf");
                item.setTemPdf(pdf.exists());
                item.setUrlPdf("/uploads/faturas/" + nomeDoc + ".pdf");
            } else {
                item.setTemPdf(false);
                item.setUrlPdf(null);
            }

            resultado.add(item);
        }

        resultado.sort((a, b) -> {
            if (a.getDataEmissao() == null || b.getDataEmissao() == null) return 0;
            return b.getDataEmissao().compareTo(a.getDataEmissao());
        });

        return resultado;
    }

    public List<ContaReceberDTO> getContasAReceber(Long empresaId) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);
        LocalDate hoje = LocalDate.now();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        List<ContaReceberDTO> resultado = new ArrayList<>();

        for (Fatura f : faturas) {
            String status = f.getStatus() != null ? f.getStatus().toUpperCase() : "";
            if ("ANULADA".equals(status) || "CANCELADA".equals(status)) {
                continue;
            }

            String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().toUpperCase() : "";
            if (!"FT".equals(tipo) && !tipo.isEmpty()) {
                continue;
            }

            double total = f.getTotal() != null ? f.getTotal() : 0.0;
            double pago = f.getValorPago() != null ? f.getValorPago() : 0.0;
            double aberto = f.getValorEmAberto() != null ? f.getValorEmAberto() : Math.max(0.0, total - pago);

            if (aberto <= 0.01) {
                continue; // Totalmente liquidada
            }

            ContaReceberDTO dto = new ContaReceberDTO();
            dto.setId(f.getId());
            dto.setCompraId(f.getCompra() != null ? f.getCompra().getId() : null);
            dto.setNumeroFatura(f.getNumeroFatura() != null ? f.getNumeroFatura() : ("FT #" + f.getId()));
            dto.setDataEmissao(f.getDataEmissao());
            dto.setDataVencimento(f.getDataVencimento());
            dto.setDataEmissaoFormatada(f.getDataEmissao() != null ? sdf.format(f.getDataEmissao()) : "-");
            dto.setDataVencimentoFormatada(f.getDataVencimento() != null ? sdf.format(f.getDataVencimento()) : "Sem Prazo");

            Compra compra = f.getCompra();
            if (compra != null && compra.getCliente() != null && compra.getCliente().getNome() != null) {
                dto.setNomeCliente(compra.getCliente().getNome());
                dto.setNifCliente(compra.getCliente().getNif() != null ? compra.getCliente().getNif() : "-");
                dto.setTelefoneCliente(compra.getCliente().getTelefone() != null ? compra.getCliente().getTelefone() : "-");
                dto.setEmailCliente(compra.getCliente().getEmail() != null ? compra.getCliente().getEmail() : "-");
            } else if (compra != null && compra.getNomeCliente() != null && !compra.getNomeCliente().trim().isEmpty()) {
                dto.setNomeCliente(compra.getNomeCliente());
                dto.setNifCliente(compra.getNifCliente() != null ? compra.getNifCliente() : "-");
                dto.setTelefoneCliente(compra.getTelefoneCliente() != null ? compra.getTelefoneCliente() : "-");
                dto.setEmailCliente(compra.getEmailCliente() != null ? compra.getEmailCliente() : "-");
            } else {
                dto.setNomeCliente("Cliente Não Identificado");
                dto.setNifCliente("-");
                dto.setTelefoneCliente("-");
                dto.setEmailCliente("-");
            }

            dto.setTotal(total);
            dto.setValorPago(pago);
            dto.setValorEmAberto(aberto);
            dto.setPercentualPago(total > 0 ? (pago / total) * 100.0 : 0.0);
            dto.setStatusFatura(f.getStatus() != null ? f.getStatus() : "EMITIDA");

            LocalDate dtVenc = toLocalDate(f.getDataVencimento());
            if (dtVenc != null) {
                if (dtVenc.isBefore(hoje)) {
                    dto.setStatusVencimento("VENCIDA");
                    dto.setDiasAtraso(ChronoUnit.DAYS.between(dtVenc, hoje));
                } else if (dtVenc.isEqual(hoje)) {
                    dto.setStatusVencimento("VENCE_HOJE");
                    dto.setDiasAtraso(0);
                } else {
                    dto.setStatusVencimento("NO_PRAZO");
                    dto.setDiasAtraso(-ChronoUnit.DAYS.between(hoje, dtVenc)); // Dias restantes
                }
            } else {
                dto.setStatusVencimento("SEM_DATA");
                dto.setDiasAtraso(0);
            }

            resultado.add(dto);
        }

        // Ordenar primeiro as mais vencidas, depois as que vencem hoje, depois as no prazo
        resultado.sort((a, b) -> {
            if ("VENCIDA".equals(a.getStatusVencimento()) && !"VENCIDA".equals(b.getStatusVencimento())) return -1;
            if (!"VENCIDA".equals(a.getStatusVencimento()) && "VENCIDA".equals(b.getStatusVencimento())) return 1;
            return Long.compare(b.getDiasAtraso(), a.getDiasAtraso());
        });

        return resultado;
    }

    public AgingContasReceberDTO getAgingContasReceber(List<ContaReceberDTO> contas) {
        AgingContasReceberDTO aging = new AgingContasReceberDTO();
        for (ContaReceberDTO c : contas) {
            double aberto = c.getValorEmAberto() != null ? c.getValorEmAberto() : 0.0;
            long dias = c.getDiasAtraso();
            String status = c.getStatusVencimento();

            if ("VENCIDA".equals(status)) {
                if (dias <= 30) {
                    aging.setVencido1a30(aging.getVencido1a30() + aberto);
                    aging.setQtdVencido1a30(aging.getQtdVencido1a30() + 1);
                } else if (dias <= 60) {
                    aging.setVencido31a60(aging.getVencido31a60() + aberto);
                    aging.setQtdVencido31a60(aging.getQtdVencido31a60() + 1);
                } else if (dias <= 90) {
                    aging.setVencido61a90(aging.getVencido61a90() + aberto);
                    aging.setQtdVencido61a90(aging.getQtdVencido61a90() + 1);
                } else {
                    aging.setVencidoMais90(aging.getVencidoMais90() + aberto);
                    aging.setQtdVencidoMais90(aging.getQtdVencidoMais90() + 1);
                }
            } else {
                aging.setNoPrazo(aging.getNoPrazo() + aberto);
                aging.setQtdNoPrazo(aging.getQtdNoPrazo() + 1);
            }
        }
        return aging;
    }

    public List<MetodoPagamentoResumoDTO> getDistribuicaoMetodosRecebimento(Long empresaId, LocalDate inicio, LocalDate fim) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);

        MetodoPagamentoResumoDTO cashDTO = new MetodoPagamentoResumoDTO("CASH", "Numerário / Dinheiro", "#10b981", "fa-money-bill-wave");
        MetodoPagamentoResumoDTO tpaDTO = new MetodoPagamentoResumoDTO("TPA", "Multicaixa / TPA", "#3b82f6", "fa-credit-card");
        MetodoPagamentoResumoDTO transfDTO = new MetodoPagamentoResumoDTO("TRANSFERENCIA", "Transferência Bancária", "#8b5cf6", "fa-university");
        MetodoPagamentoResumoDTO outroDTO = new MetodoPagamentoResumoDTO("OUTRO", "Outros Meios", "#f59e0b", "fa-wallet");

        double totalGeral = 0.0;

        for (Fatura f : faturas) {
            String status = f.getStatus() != null ? f.getStatus().toUpperCase() : "";
            if ("ANULADA".equals(status) || "CANCELADA".equals(status)) {
                continue;
            }

            String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().toUpperCase() : "";
            if (!"RC".equals(tipo) && !"FR".equals(tipo)) {
                continue;
            }

            if (isDateInPeriod(f.getDataEmissao(), inicio, fim)) {
                double val = f.getTotal() != null ? f.getTotal() : 0.0;
                totalGeral += val;

                String metodo = f.getFormaPagamento();
                if (metodo == null && f.getCompra() != null) {
                    metodo = f.getCompra().getFormaPagamento();
                }
                String norm = normalizarMetodo(metodo);

                switch (norm) {
                    case "CASH":
                        cashDTO.setTotal(cashDTO.getTotal() + val);
                        cashDTO.setQuantidade(cashDTO.getQuantidade() + 1);
                        break;
                    case "TPA":
                        tpaDTO.setTotal(tpaDTO.getTotal() + val);
                        tpaDTO.setQuantidade(tpaDTO.getQuantidade() + 1);
                        break;
                    case "TRANSFERENCIA":
                        transfDTO.setTotal(transfDTO.getTotal() + val);
                        transfDTO.setQuantidade(transfDTO.getQuantidade() + 1);
                        break;
                    default:
                        outroDTO.setTotal(outroDTO.getTotal() + val);
                        outroDTO.setQuantidade(outroDTO.getQuantidade() + 1);
                        break;
                }
            }
        }

        if (totalGeral > 0) {
            cashDTO.setPercentual((cashDTO.getTotal() / totalGeral) * 100.0);
            tpaDTO.setPercentual((tpaDTO.getTotal() / totalGeral) * 100.0);
            transfDTO.setPercentual((transfDTO.getTotal() / totalGeral) * 100.0);
            outroDTO.setPercentual((outroDTO.getTotal() / totalGeral) * 100.0);
        }

        List<MetodoPagamentoResumoDTO> lista = new ArrayList<>();
        lista.add(cashDTO);
        lista.add(tpaDTO);
        lista.add(transfDTO);
        if (outroDTO.getQuantidade() > 0) {
            lista.add(outroDTO);
        }
        return lista;
    }

    public Map<String, Object> getEvolucaoRecebimentosDiarios(Long empresaId, int dias) {
        List<Fatura> faturas = (empresaId == null) ? faturaRepository.findAll() : faturaRepository.findByEmpresa_Id(empresaId);
        LocalDate hoje = LocalDate.now();

        List<String> labels = new ArrayList<>();
        List<Double> totais = new ArrayList<>();
        List<Double> totaisRC = new ArrayList<>();
        List<Double> totaisFR = new ArrayList<>();

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        for (int i = dias - 1; i >= 0; i--) {
            LocalDate dia = hoje.minusDays(i);
            labels.add(dia.format(fmt));

            double somaRC = 0.0;
            double somaFR = 0.0;

            for (Fatura f : faturas) {
                String status = f.getStatus() != null ? f.getStatus().toUpperCase() : "";
                if ("ANULADA".equals(status) || "CANCELADA".equals(status)) {
                    continue;
                }

                if (isSameDay(f.getDataEmissao(), dia)) {
                    double val = f.getTotal() != null ? f.getTotal() : 0.0;
                    String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().toUpperCase() : "";
                    if ("RC".equals(tipo)) {
                        somaRC += val;
                    } else if ("FR".equals(tipo)) {
                        somaFR += val;
                    }
                }
            }

            totaisRC.add(somaRC);
            totaisFR.add(somaFR);
            totais.add(somaRC + somaFR);
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("labels", labels);
        resultado.put("totais", totais);
        resultado.put("totaisRC", totaisRC);
        resultado.put("totaisFR", totaisFR);
        return resultado;
    }

    public List<ClienteDevedorDTO> getTopClientesDevedores(List<ContaReceberDTO> contas, int limite) {
        Map<String, ClienteDevedorDTO> mapa = new HashMap<>();
        double totalGeralDivida = 0.0;

        for (ContaReceberDTO c : contas) {
            double aberto = c.getValorEmAberto() != null ? c.getValorEmAberto() : 0.0;
            totalGeralDivida += aberto;

            String key = c.getNomeCliente() != null ? c.getNomeCliente() : "Consumidor Final";
            ClienteDevedorDTO dto = mapa.get(key);
            if (dto == null) {
                dto = new ClienteDevedorDTO();
                dto.setNome(key);
                dto.setNif(c.getNifCliente());
                dto.setTelefone(c.getTelefoneCliente());
                dto.setEmail(c.getEmailCliente());
                mapa.put(key, dto);
            }
            dto.setTotalEmDivida(dto.getTotalEmDivida() + aberto);
            dto.setTotalFaturas(dto.getTotalFaturas() + 1);
            if ("VENCIDA".equals(c.getStatusVencimento())) {
                dto.setFaturasVencidas(dto.getFaturasVencidas() + 1);
            }
        }

        final double totalFinal = totalGeralDivida;
        List<ClienteDevedorDTO> lista = mapa.values().stream()
                .sorted(Comparator.comparingDouble(ClienteDevedorDTO::getTotalEmDivida).reversed())
                .limit(limite)
                .collect(Collectors.toList());

        for (ClienteDevedorDTO dto : lista) {
            if (totalFinal > 0) {
                dto.setPercentualDoTotal((dto.getTotalEmDivida() / totalFinal) * 100.0);
            }
        }
        return lista;
    }

    public List<ClienteRecebimentoDTO> getTopClientesRecebimentos(List<RecebimentoItemDTO> recebimentos, int limite) {
        Map<String, ClienteRecebimentoDTO> mapa = new HashMap<>();
        for (RecebimentoItemDTO r : recebimentos) {
            double val = r.getValor() != null ? r.getValor() : 0.0;
            String key = r.getNomeCliente() != null ? r.getNomeCliente() : "Consumidor Final";
            ClienteRecebimentoDTO dto = mapa.get(key);
            if (dto == null) {
                dto = new ClienteRecebimentoDTO();
                dto.setNome(key);
                dto.setNif(r.getNifCliente());
                mapa.put(key, dto);
            }
            dto.setTotalPago(dto.getTotalPago() + val);
            dto.setTotalRecibos(dto.getTotalRecibos() + 1);
        }

        return mapa.values().stream()
                .sorted(Comparator.comparingDouble(ClienteRecebimentoDTO::getTotalPago).reversed())
                .limit(limite)
                .collect(Collectors.toList());
    }
}