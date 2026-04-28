package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.service.DashboardService;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@Controller
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;
    
    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        
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
        model.addAttribute("lucroBrutoMensal", dashboardService.getLucroBrutoMensal(empresaId));

        // Dados para os novos gráficos
        model.addAttribute("receitaVsDespesa", dashboardService.getReceitaVsDespesaData(empresaId));
        model.addAttribute("comparacaoPeriodos", dashboardService.getComparacaoPeriodosData(empresaId));
        model.addAttribute("vendasPorLocalizacao", dashboardService.getVendasPorLocalizacao(empresaId));
        model.addAttribute("horariosPico", dashboardService.getHorariosPicoVendas(empresaId));

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
        
        double totalVendas = vendas.stream()
            .mapToDouble(v -> v.getTotal() != null ? v.getTotal() : 0.0)
            .sum();
            
        double totalIva = 0.0;
        java.util.Map<Long, String> faturasMap = new java.util.HashMap<>();
        
        for (Compra v : vendas) {
            if (v.getItens() != null) {
                totalIva += v.getItens().stream()
                    .mapToDouble(i -> i.getIva() != null ? i.getIva() : 0.0)
                    .sum();
            }
            
            // Buscar o número da fatura associada a esta compra
            List<ao.co.hzconsultoria.efacturacao.model.Fatura> faturas = faturaRepository.findByCompra(v);
            if (!faturas.isEmpty()) {
                faturasMap.put(v.getId(), faturas.get(0).getNumeroFatura());
            }
        }

        model.addAttribute("vendasDoDia", vendas);
        model.addAttribute("totalVendasDia", totalVendas);
        model.addAttribute("totalIvaDia", totalIva);
        model.addAttribute("faturasMap", faturasMap);
        return "vendasDia";
    }

    @GetMapping("/dashboard/receita-mensal")
    public String receitaMensal(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        List<Compra> vendas = dashboardService.getComprasDoMes(empresaId);
        
        double totalVendas = vendas.stream()
            .mapToDouble(v -> v.getTotal() != null ? v.getTotal() : 0.0)
            .sum();
            
        double totalIva = 0.0;
        java.util.Map<Long, String> faturasMap = new java.util.HashMap<>();
        
        for (Compra v : vendas) {
            if (v.getItens() != null) {
                totalIva += v.getItens().stream()
                    .mapToDouble(i -> i.getIva() != null ? i.getIva() : 0.0)
                    .sum();
            }
            
            // Buscar o número da fatura associada a esta compra
            List<ao.co.hzconsultoria.efacturacao.model.Fatura> faturas = faturaRepository.findByCompra(v);
            if (!faturas.isEmpty()) {
                faturasMap.put(v.getId(), faturas.get(0).getNumeroFatura());
            }
        }

        model.addAttribute("vendasDoMes", vendas);
        model.addAttribute("totalVendasMes", totalVendas);
        model.addAttribute("totalIvaMes", totalIva);
        model.addAttribute("faturasMap", faturasMap);
        return "receitaMensal";
    }
}