package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.service.DashboardService;
import ao.co.hzconsultoria.efacturacao.service.CaixaService;
import ao.co.hzconsultoria.efacturacao.model.Caixa;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.repository.DespesaRepository;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.service.FaturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ao.co.hzconsultoria.efacturacao.dto.RecebimentoDashboardDTO.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private CaixaService caixaService;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.ConfiguracaoEmpresaService configuracaoEmpresaService;

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private DespesaRepository despesaRepository;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.DevolucaoRepository devolucaoRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        
        if (empresaId != null) {
            boolean isSetupCompleto = configuracaoEmpresaService.obterConfiguracao(empresaId).isSetupCompleto();
            if (!isSetupCompleto) {
                return "redirect:/configuracoes/setup-wizard";
            }
            model.addAttribute("isSetupCompleto", true);
        } else {
            model.addAttribute("isSetupCompleto", true);
        }

        model.addAttribute("produtosMaisVendidos", dashboardService.getProdutosMaisVendidos(empresaId, 10));
        model.addAttribute("vendasUltimos30Dias", dashboardService.getVendasUltimos30Dias(empresaId));
        model.addAttribute("produtosEstoqueBaixo", dashboardService.getProdutosEstoqueBaixo(empresaId, 5));

        // 1ª Linha
        model.addAttribute("vendasHoje", dashboardService.getReceitaDia(empresaId));
        model.addAttribute("vendasMes", dashboardService.getReceitaMensal(empresaId));
        model.addAttribute("totalIva", dashboardService.getTotalIvaMes(empresaId));
        model.addAttribute("totalFacturas", dashboardService.getTotalFacturasMes(empresaId));
        
        // 2ª Linha
        model.addAttribute("totalPagamentos", dashboardService.getTotalPagamentosMes(empresaId));
        model.addAttribute("totalClientes", dashboardService.getTotalClientes(empresaId));
        model.addAttribute("totalPendentes", dashboardService.getTotalPendentes(empresaId));
        model.addAttribute("totalLucro", dashboardService.getLucroTotal(empresaId));
        model.addAttribute("totalMovimentos", dashboardService.getTotalMovimentos(empresaId));
        model.addAttribute("lucroBrutoMensal", dashboardService.getLucroBrutoMensal(empresaId));
        model.addAttribute("lucroLiquidoMensal", dashboardService.getLucroMensal(empresaId));

        // Dados para os novos gráficos
        model.addAttribute("receitaVsDespesa", dashboardService.getReceitaVsDespesaData(empresaId));
        model.addAttribute("comparacaoPeriodos", dashboardService.getComparacaoPeriodosData(empresaId));
        model.addAttribute("vendasPorLocalizacao", dashboardService.getVendasPorLocalizacao(empresaId));
        model.addAttribute("horariosPico", dashboardService.getHorariosPicoVendas(empresaId));

        // Widget de Caixas Abertos para Admin/Gestor
        if (empresaId != null) {
            List<Caixa> caixasAbertos = caixaService.getCaixasAbertosPorEmpresa(empresaId);
            double totalFaturadoCaixas = caixasAbertos.stream().mapToDouble(c -> c.getTotalFaturado() != null ? c.getTotalFaturado() : 0.0).sum();
            model.addAttribute("caixasAbertos", caixasAbertos);
            model.addAttribute("qtdCaixasAbertos", caixasAbertos.size());
            model.addAttribute("totalFaturadoCaixas", totalFaturadoCaixas);
        } else {
            model.addAttribute("caixasAbertos", new java.util.ArrayList<>());
            model.addAttribute("qtdCaixasAbertos", 0);
            model.addAttribute("totalFaturadoCaixas", 0.0);
        }

        return "dashboard";
    }

    @GetMapping("/dashboard/estatisticas")
    public String estatisticas(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();

        // KPI Cards
        model.addAttribute("vendasTotais", dashboardService.getVendasTotaisMes(empresaId));
        model.addAttribute("receitaTotal", String.format("%.2f Kz", dashboardService.getReceitaMensal(empresaId)));
        model.addAttribute("clientesAtivosCount", dashboardService.getTotalClientes(empresaId));
        model.addAttribute("produtosVendidosCount", dashboardService.getProdutosVendidosCount(empresaId));

        // Top Tables
        model.addAttribute("produtosMaisVendidos", dashboardService.getProdutosMaisVendidos(empresaId, 10));
        model.addAttribute("clientesTopCompras", dashboardService.getClientesTopCompras(empresaId, 10));

        // Comparativos Mês Atual vs Anterior
        model.addAttribute("vendasMesAtual", dashboardService.getVendasTotaisMes(empresaId));
        model.addAttribute("vendasMesAnterior", dashboardService.getVendasMesAnterior(empresaId));
        model.addAttribute("variacaoVendas", dashboardService.getVariacaoVendas(empresaId));
        model.addAttribute("receitaMesAtual", String.format("%.2f Kz", dashboardService.getReceitaMensal(empresaId)));
        model.addAttribute("receitaMesAnterior", String.format("%.2f Kz", dashboardService.getReceitaMesAnterior(empresaId)));
        model.addAttribute("variacaoReceita", dashboardService.getVariacaoReceita(empresaId));

        // Dados para Gráficos
        model.addAttribute("graficoMesesLabels", dashboardService.getUltimos12MesesLabels());
        model.addAttribute("graficoVendasData", dashboardService.getVendasPorMes(empresaId));
        model.addAttribute("graficoReceitaData", dashboardService.getReceitaPorMes(empresaId));
        model.addAttribute("graficoClientesData", dashboardService.getNovoClientesPorMes(empresaId));

        return "dashboardEstatisticas";
    }

    @GetMapping("/dashboard/relatorios")
    public String relatorios(Model model) {
        return "dashboardRelatorios";
    }

    @GetMapping("/dashboard/configuracoes")
    public String configuracoes(Model model) {
        return "dashboardConfiguracoes";
    }

    @GetMapping("/dashboard/estoque-baixo")
    public String estoqueBaixo(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("produtosEstoqueBaixo", dashboardService.getProdutosEstoqueBaixo(empresaId, 5));
        return "estoqueBaixo";
    }

    @GetMapping("/dashboard/vendas-dia")
    public String vendasDia(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        List<Compra> vendas = dashboardService.getComprasDoDia(empresaId);
        
        double totalVendasGross = vendas.stream()
            .filter(v -> !"CANCELADA".equalsIgnoreCase(v.getStatus()))
            .mapToDouble(v -> v.getTotal() != null ? v.getTotal() : 0.0)
            .sum();
            
        double totalIva = 0.0;
        java.util.Map<Long, String> faturasMap = new java.util.HashMap<>();
        
        for (Compra v : vendas) {
            if (!"CANCELADA".equalsIgnoreCase(v.getStatus())) {
                if (v.getItens() != null) {
                    totalIva += v.getItens().stream()
                        .mapToDouble(item -> item.getIva() != null ? item.getIva() : 0.0)
                        .sum();
                }
            }
            
            // Buscar o número da fatura associada a esta compra
            List<ao.co.hzconsultoria.efacturacao.model.Fatura> faturas = faturaRepository.findByCompra(v);
            if (!faturas.isEmpty()) {
                faturasMap.put(v.getId(), faturas.get(0).getNumeroFatura());
            }
        }

        model.addAttribute("vendasDoDia", vendas);
        model.addAttribute("totalVendasDia", totalVendasGross - totalIva); // Venda Líquida
        model.addAttribute("totalIvaDia", totalIva);
        model.addAttribute("faturasMap", faturasMap);
        return "vendasDia";
    }

    @GetMapping("/dashboard/receita-mensal")
    public String receitaMensal(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        List<Compra> vendas = dashboardService.getComprasDoMes(empresaId);
        
        double totalVendasGross = vendas.stream()
            .filter(v -> !"CANCELADA".equalsIgnoreCase(v.getStatus()))
            .mapToDouble(v -> v.getTotal() != null ? v.getTotal() : 0.0)
            .sum();
            
        double totalIva = 0.0;
        java.util.Map<Long, String> faturasMap = new java.util.HashMap<>();
        
        for (Compra v : vendas) {
            if (!"CANCELADA".equalsIgnoreCase(v.getStatus())) {
                if (v.getItens() != null) {
                    totalIva += v.getItens().stream()
                        .mapToDouble(item -> item.getIva() != null ? item.getIva() : 0.0)
                        .sum();
                }
            }
            
            // Buscar o número da fatura associada a esta compra
            List<ao.co.hzconsultoria.efacturacao.model.Fatura> faturas = faturaRepository.findByCompra(v);
            if (!faturas.isEmpty()) {
                faturasMap.put(v.getId(), faturas.get(0).getNumeroFatura());
            }
        }

        model.addAttribute("vendasDoMes", vendas);
        model.addAttribute("totalVendasMes", totalVendasGross - totalIva); // Venda Líquida
        model.addAttribute("totalIvaMes", totalIva);
        model.addAttribute("faturasMap", faturasMap);
        return "receitaMensal";
    }

    @GetMapping("/dashboard/lucro-liquido-mensal")
    public String lucroLiquidoMensal(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();

        java.time.LocalDate hoje = java.time.LocalDate.now();
        java.time.LocalDate inicioMes = hoje.withDayOfMonth(1);
        java.time.LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());

        // Vendas do mês (Excluindo canceladas)
        List<Compra> vendasDoMes = dashboardService.getComprasDoMes(empresaId).stream()
            .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getStatus()))
            .collect(Collectors.toList());

        // Receita total mensal (Bruta das não canceladas)
        double receitaMensalBruta = vendasDoMes.stream()
            .mapToDouble(c -> c.getTotal() != null ? c.getTotal() : 0.0)
            .sum();
        
        // IVA total das vendas do mês
        double totalIvaVendas = vendasDoMes.stream()
            .mapToDouble(c -> c.getItens() != null ? c.getItens().stream().mapToDouble(item -> item.getIva() != null ? item.getIva() : 0.0).sum() : 0.0)
            .sum();

        // Devoluções do mês
        List<ao.co.hzconsultoria.efacturacao.model.Devolucao> devolucoesDoMes = (empresaId == null) ? devolucaoRepository.findAll() : devolucaoRepository.findByEmpresa_Id(empresaId);
        devolucoesDoMes = devolucoesDoMes.stream()
            .filter(d -> d.getDataDevolucao() != null && !d.getDataDevolucao().toLocalDate().isBefore(inicioMes) && !d.getDataDevolucao().toLocalDate().isAfter(fimMes))
            .collect(Collectors.toList());
        
        double totalDevolucoes = devolucoesDoMes.stream()
            .mapToDouble(d -> (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0))
            .sum();

        // Receita Bruta Real (Vendas Brutas - Devoluções Brutas)
        double receitaMensal = receitaMensalBruta - totalDevolucoes;

        // COGS (Custo dos Produtos Vendidos) - Baseado apenas nas vendas efectivas
        double cogs = 0.0;
        for (Compra compra : vendasDoMes) {
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

        // Despesas do mês
        List<Despesa> todasDespesas = despesaRepository.findAll().stream()
            .filter(d -> d.getEmpresa() == null || (empresaId != null && d.getEmpresa().getId().equals(empresaId)))
            .collect(Collectors.toList());
        List<Despesa> despesasDoMes = todasDespesas.stream()
            .filter(d -> d.getDataDespesa() != null
                && !d.getDataDespesa().isBefore(inicioMes)
                && !d.getDataDespesa().isAfter(fimMes))
            .collect(Collectors.toList());
        double totalDespesas = despesasDoMes.stream()
            .mapToDouble(d -> d.getValor() != null ? d.getValor() : 0.0)
            .sum();

        // Lucro Bruto = Receita - COGS
        double lucroBruto = receitaMensal - cogs;

        // Lucro Líquido = Receita - COGS - Despesas
        double lucroLiquido = lucroBruto - totalDespesas;

        // Margem de Lucro (%)
        double margemLucro = receitaMensal > 0 ? (lucroLiquido / receitaMensal) * 100 : 0;

        // Número de vendas
        long totalVendasCount = vendasDoMes.size();

        model.addAttribute("receitaMensal", receitaMensal);
        model.addAttribute("receitaMensalBruta", receitaMensalBruta); // Mostrar valor bruto real
        model.addAttribute("totalDevolucoes", totalDevolucoes);
        model.addAttribute("cogs", cogs);
        model.addAttribute("totalDespesas", totalDespesas);
        model.addAttribute("lucroBruto", lucroBruto);
        model.addAttribute("lucroLiquido", lucroLiquido);
        model.addAttribute("margemLucro", margemLucro);
        model.addAttribute("totalVendasCount", totalVendasCount);
        model.addAttribute("vendasDoMes", vendasDoMes);
        model.addAttribute("despesasDoMes", despesasDoMes);
        model.addAttribute("devolucoesDoMes", devolucoesDoMes);

        return "lucroMensal";
    }

    @GetMapping("/dashboard/recebimentos")
    public String recebimentos(
            @RequestParam(value = "periodo", defaultValue = "mes") String periodo,
            @RequestParam(value = "dataInicio", required = false) String dataInicioStr,
            @RequestParam(value = "dataFim", required = false) String dataFimStr,
            Model model) {

        try {
            Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();

            LocalDate hoje = LocalDate.now();
            LocalDate inicio;
            LocalDate fim = hoje;

            if ("hoje".equalsIgnoreCase(periodo)) {
                inicio = hoje;
                fim = hoje;
            } else if ("7dias".equalsIgnoreCase(periodo)) {
                inicio = hoje.minusDays(6);
                fim = hoje;
            } else if ("ano".equalsIgnoreCase(periodo)) {
                inicio = hoje.withDayOfYear(1);
                fim = hoje;
            } else if ("todos".equalsIgnoreCase(periodo)) {
                inicio = null;
                fim = null;
            } else if ("custom".equalsIgnoreCase(periodo) || (dataInicioStr != null && dataFimStr != null && !dataInicioStr.trim().isEmpty() && !dataFimStr.trim().isEmpty())) {
                try {
                    inicio = LocalDate.parse(dataInicioStr.trim());
                    fim = LocalDate.parse(dataFimStr.trim());
                    periodo = "custom";
                } catch (Exception e) {
                    inicio = hoje.withDayOfMonth(1);
                    fim = hoje.withDayOfMonth(hoje.lengthOfMonth());
                    periodo = "mes";
                }
            } else {
                inicio = hoje.withDayOfMonth(1);
                fim = hoje.withDayOfMonth(hoje.lengthOfMonth());
                periodo = "mes";
            }

            // 1. Resumo dos Indicadores e KPIs
            RecebimentosResumoDTO resumo = dashboardService.getRecebimentosResumo(empresaId, inicio, fim);
            model.addAttribute("resumo", resumo != null ? resumo : new RecebimentosResumoDTO());

            // 2. Histórico de Recebimentos (RC e FR)
            List<RecebimentoItemDTO> recebimentos = dashboardService.getHistoricoRecebimentos(empresaId, inicio, fim);
            model.addAttribute("recebimentos", recebimentos != null ? recebimentos : java.util.Collections.emptyList());

            // 3. Contas a Receber / Dívidas (FT pendentes)
            List<ContaReceberDTO> contasReceber = dashboardService.getContasAReceber(empresaId);
            model.addAttribute("contasReceber", contasReceber != null ? contasReceber : java.util.Collections.emptyList());

            // 4. Aging das Contas a Receber
            AgingContasReceberDTO aging = dashboardService.getAgingContasReceber(contasReceber != null ? contasReceber : java.util.Collections.emptyList());
            model.addAttribute("aging", aging != null ? aging : new AgingContasReceberDTO());

            // 5. Distribuição por Métodos de Pagamento
            List<MetodoPagamentoResumoDTO> metodos = dashboardService.getDistribuicaoMetodosRecebimento(empresaId, inicio, fim);
            if (metodos == null) metodos = java.util.Collections.emptyList();
            model.addAttribute("metodosPagamento", metodos);

            // 6. Evolução Diária dos Recebimentos (Últimos 30 dias para o gráfico temporal)
            Map<String, Object> evolucaoDiaria = dashboardService.getEvolucaoRecebimentosDiarios(empresaId, 30);
            if (evolucaoDiaria == null) evolucaoDiaria = new java.util.HashMap<>();
            model.addAttribute("evolucaoDiaria", evolucaoDiaria);

            // 7. Top Clientes Devedores
            List<ClienteDevedorDTO> topDevedores = dashboardService.getTopClientesDevedores(contasReceber != null ? contasReceber : java.util.Collections.emptyList(), 5);
            model.addAttribute("topDevedores", topDevedores != null ? topDevedores : java.util.Collections.emptyList());

            // 8. Top Clientes Pagadores
            List<ClienteRecebimentoDTO> topPagadores = dashboardService.getTopClientesRecebimentos(recebimentos != null ? recebimentos : java.util.Collections.emptyList(), 5);
            model.addAttribute("topPagadores", topPagadores != null ? topPagadores : java.util.Collections.emptyList());

            // 9. Listas diretas para Chart.js
            List<String> evolucaoLabels = evolucaoDiaria.containsKey("labels") ? (List<String>) evolucaoDiaria.get("labels") : java.util.Collections.emptyList();
            List<Double> evolucaoTotais = evolucaoDiaria.containsKey("totais") ? (List<Double>) evolucaoDiaria.get("totais") : java.util.Collections.emptyList();
            List<String> metodosLabels = new java.util.ArrayList<>();
            List<Double> metodosValores = new java.util.ArrayList<>();
            List<String> metodosCores = new java.util.ArrayList<>();
            for (MetodoPagamentoResumoDTO m : metodos) {
                metodosLabels.add(m.getLabel() != null ? m.getLabel() : m.getMetodo());
                metodosValores.add(m.getTotal() != null ? m.getTotal() : 0.0);
                metodosCores.add(m.getCor() != null ? m.getCor() : "#10b981");
            }

            model.addAttribute("evolucaoLabels", evolucaoLabels);
            model.addAttribute("evolucaoTotais", evolucaoTotais);
            model.addAttribute("metodosLabels", metodosLabels);
            model.addAttribute("metodosValores", metodosValores);
            model.addAttribute("metodosCores", metodosCores);

            // Metadados do filtro selecionado
            model.addAttribute("periodoSelecionado", periodo);
            model.addAttribute("dataInicioFiltro", inicio != null ? inicio.toString() : "");
            model.addAttribute("dataFimFiltro", fim != null ? fim.toString() : "");

        } catch (Exception ex) {
            System.err.println(">>> Erro ao carregar /dashboard/recebimentos: " + ex.getMessage());
            ex.printStackTrace();
            model.addAttribute("resumo", new RecebimentosResumoDTO());
            model.addAttribute("recebimentos", java.util.Collections.emptyList());
            model.addAttribute("contasReceber", java.util.Collections.emptyList());
            model.addAttribute("aging", new AgingContasReceberDTO());
            model.addAttribute("metodosPagamento", java.util.Collections.emptyList());
            model.addAttribute("topDevedores", java.util.Collections.emptyList());
            model.addAttribute("topPagadores", java.util.Collections.emptyList());
            model.addAttribute("evolucaoLabels", java.util.Collections.emptyList());
            model.addAttribute("evolucaoTotais", java.util.Collections.emptyList());
            model.addAttribute("metodosLabels", java.util.Collections.emptyList());
            model.addAttribute("metodosValores", java.util.Collections.emptyList());
            model.addAttribute("metodosCores", java.util.Collections.emptyList());
            model.addAttribute("evolucaoDiaria", new java.util.HashMap<>());
            model.addAttribute("periodoSelecionado", periodo);
            model.addAttribute("dataInicioFiltro", "");
            model.addAttribute("dataFimFiltro", "");
        }

        return "dashboardRecebimentos";
    }

    @GetMapping({"/dashboard/documentos-pendentes", "/documentos-pendentes"})
    public String documentosPendentes(
            @RequestParam(required = false, defaultValue = "agt") String aba,
            Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();

        // 1. Facturas da empresa
        List<Fatura> todasFaturas = (empresaId == null) 
            ? faturaRepository.findAll() 
            : faturaRepository.findByEmpresa_Id(empresaId);

        // Facturas Pendentes de Validação / Envio AGT
        List<Fatura> faturasPendentesAgt = todasFaturas.stream()
            .filter(f -> "PENDENTE".equalsIgnoreCase(f.getStatus()) 
                      || (Boolean.FALSE.equals(f.isEnviadaAGT()) && !"VALIDADA".equalsIgnoreCase(f.getStatus()) && !"CANCELADA".equalsIgnoreCase(f.getStatus()) && !"ANULADA".equalsIgnoreCase(f.getStatus())))
            .sorted((a, b) -> {
                if (a.getDataEmissao() == null || b.getDataEmissao() == null) return 0;
                return b.getDataEmissao().compareTo(a.getDataEmissao());
            })
            .collect(Collectors.toList());

        // Facturas com Falha de Envio AGT
        List<Fatura> faturasFalhasAgt = todasFaturas.stream()
            .filter(f -> "FALHA_ENVIO".equalsIgnoreCase(f.getStatus()) 
                      || (f.getCodigoAgt() != null && f.getCodigoAgt().startsWith("ERRO:")))
            .sorted((a, b) -> {
                if (a.getDataEmissao() == null || b.getDataEmissao() == null) return 0;
                return b.getDataEmissao().compareTo(a.getDataEmissao());
            })
            .collect(Collectors.toList());

        // Evitar duplicados
        faturasPendentesAgt.removeAll(faturasFalhasAgt);

        double totalValorPendenteAgt = faturasPendentesAgt.stream()
            .mapToDouble(f -> f.getTotal() != null ? f.getTotal() : 0.0)
            .sum();

        double totalValorFalhasAgt = faturasFalhasAgt.stream()
            .mapToDouble(f -> f.getTotal() != null ? f.getTotal() : 0.0)
            .sum();

        // 2. Facturas Proforma (FP) Pendentes de Conversão
        List<Compra> todasCompras = (empresaId == null)
            ? compraRepository.findAll()
            : compraRepository.findByEmpresa_Id(empresaId);

        List<Compra> proformasPendentes = todasCompras.stream()
            .filter(c -> "FP".equalsIgnoreCase(c.getTipoDocumento()))
            .filter(c -> !"CONVERTIDA".equalsIgnoreCase(c.getStatus()) 
                      && !"CANCELADA".equalsIgnoreCase(c.getStatus())
                      && !"REJEITADA".equalsIgnoreCase(c.getStatus())
                      && !"ANULADA".equalsIgnoreCase(c.getStatus()))
            .sorted((a, b) -> {
                if (a.getDataCompra() == null || b.getDataCompra() == null) return 0;
                return b.getDataCompra().compareTo(a.getDataCompra());
            })
            .collect(Collectors.toList());

        double totalValorProformas = proformasPendentes.stream()
            .mapToDouble(c -> c.getTotal() != null ? c.getTotal() : 0.0)
            .sum();

        // 3. Contas a Receber (FT Emitidas com Saldo em Aberto)
        List<ContaReceberDTO> contasReceber = dashboardService.getContasAReceber(empresaId);
        double totalSaldoEmAberto = contasReceber.stream()
            .mapToDouble(c -> c.getValorEmAberto() != null ? c.getValorEmAberto() : 0.0)
            .sum();

        long totalGeralPendentes = faturasPendentesAgt.size() + faturasFalhasAgt.size() + proformasPendentes.size();

        model.addAttribute("faturasPendentesAgt", faturasPendentesAgt);
        model.addAttribute("faturasFalhasAgt", faturasFalhasAgt);
        model.addAttribute("proformasPendentes", proformasPendentes);
        model.addAttribute("contasReceber", contasReceber);

        model.addAttribute("qtdPendentesAgt", faturasPendentesAgt.size());
        model.addAttribute("valorPendentesAgt", totalValorPendenteAgt);
        model.addAttribute("qtdFalhasAgt", faturasFalhasAgt.size());
        model.addAttribute("valorFalhasAgt", totalValorFalhasAgt);
        model.addAttribute("qtdProformas", proformasPendentes.size());
        model.addAttribute("valorProformas", totalValorProformas);
        model.addAttribute("qtdContasReceber", contasReceber.size());
        model.addAttribute("valorContasReceber", totalSaldoEmAberto);
        model.addAttribute("totalGeralPendentes", totalGeralPendentes);

        model.addAttribute("abaAtiva", aba);

        return "documentosPendentes";
    }

    @GetMapping("/dashboard/documentos-pendentes/reenviar/{id}")
    public String reenviarFaturaPendentes(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Fatura fatura = faturaService.reenviarFatura(id);
            if (fatura != null && fatura.isEnviadaAGT()) {
                redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura " + (fatura.getNumeroFatura() != null ? fatura.getNumeroFatura() : "") + " reenviada e validada com sucesso na AGT!");
            } else {
                redirectAttributes.addFlashAttribute("mensagemErro", "Falha ao reenviar factura para a AGT: " + (fatura != null && fatura.getCodigoAgt() != null ? fatura.getCodigoAgt() : "Aguarde nova tentativa."));
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao processar reenvio: " + e.getMessage());
        }
        return "redirect:/dashboard/documentos-pendentes?aba=agt";
    }

    @PostMapping("/dashboard/documentos-pendentes/reenviar-todos")
    public String reenviarTodosPendentes(RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        List<Fatura> todasFaturas = (empresaId == null) 
            ? faturaRepository.findAll() 
            : faturaRepository.findByEmpresa_Id(empresaId);

        List<Fatura> pendentes = todasFaturas.stream()
            .filter(f -> "PENDENTE".equalsIgnoreCase(f.getStatus()) 
                      || "FALHA_ENVIO".equalsIgnoreCase(f.getStatus()) 
                      || (Boolean.FALSE.equals(f.isEnviadaAGT()) && !"VALIDADA".equalsIgnoreCase(f.getStatus()) && !"CANCELADA".equalsIgnoreCase(f.getStatus()) && !"ANULADA".equalsIgnoreCase(f.getStatus())))
            .collect(Collectors.toList());

        int sucessos = 0;
        int falhas = 0;
        for (Fatura f : pendentes) {
            try {
                Fatura res = faturaService.reenviarFatura(f.getId());
                if (res != null && res.isEnviadaAGT()) sucessos++;
                else falhas++;
            } catch (Exception ex) {
                falhas++;
            }
        }

        if (sucessos > 0 && falhas == 0) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", sucessos + " documento(s) fiscal(is) validado(s) com sucesso na AGT!");
        } else if (sucessos > 0) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", sucessos + " validado(s) com sucesso. " + falhas + " documento(s) ainda com falha.");
        } else if (falhas > 0) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não foi possível validar os documentos na AGT (" + falhas + " falha(s)). Verifique a comunicação/credenciais com a AGT.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Nenhum documento pendente para reenviar.");
        }
        return "redirect:/dashboard/documentos-pendentes?aba=agt";
    }
}