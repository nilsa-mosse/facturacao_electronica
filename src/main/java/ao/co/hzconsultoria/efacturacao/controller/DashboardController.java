package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // ...outros indicadores...
        model.addAttribute("produtosMaisVendidos", dashboardService.getProdutosMaisVendidos(10));
        model.addAttribute("vendasUltimos30Dias", dashboardService.getVendasUltimos30Dias());
        model.addAttribute("produtosEstoqueBaixo", dashboardService.getProdutosEstoqueBaixo(5));

        // 1ª Linha
        model.addAttribute("vendasHoje", dashboardService.getReceitaDia());
        model.addAttribute("vendasMes", dashboardService.getReceitaMensal());
        model.addAttribute("totalIva", dashboardService.getTotalIvaMes());
        model.addAttribute("totalFacturas", dashboardService.getTotalFacturasMes());
        
        // 2ª Linha
        model.addAttribute("totalPagamentos", dashboardService.getTotalPagamentosMes());
        model.addAttribute("totalClientes", dashboardService.getTotalClientes());
        model.addAttribute("totalPendentes", dashboardService.getTotalPendentes());
        model.addAttribute("lucroMensal", dashboardService.getLucroMensal());

        // Dados para os novos gráficos
        model.addAttribute("receitaVsDespesa", dashboardService.getReceitaVsDespesaData());
        model.addAttribute("comparacaoPeriodos", dashboardService.getComparacaoPeriodosData());
        model.addAttribute("vendasPorLocalizacao", dashboardService.getVendasPorLocalizacao());
        model.addAttribute("horariosPico", dashboardService.getHorariosPicoVendas());

        // ...outros atributos...
        return "dashboard";
    }

    @GetMapping("/dashboard/estatisticas")
    public String estatisticas(Model model) {
        // KPI Cards
        model.addAttribute("vendasTotais", dashboardService.getVendasTotaisMes());
        model.addAttribute("receitaTotal", String.format("%.2f Kz", dashboardService.getReceitaMensal()));
        model.addAttribute("clientesAtivosCount", dashboardService.getTotalClientes());
        model.addAttribute("produtosVendidosCount", dashboardService.getProdutosVendidosCount());

        // Top Tables
        model.addAttribute("produtosMaisVendidos", dashboardService.getProdutosMaisVendidos(10));
        model.addAttribute("clientesTopCompras", dashboardService.getClientesTopCompras(10));

        // Comparativos Mês Atual vs Anterior
        model.addAttribute("vendasMesAtual", dashboardService.getVendasTotaisMes());
        model.addAttribute("vendasMesAnterior", dashboardService.getVendasMesAnterior());
        model.addAttribute("variacaoVendas", dashboardService.getVariacaoVendas());
        model.addAttribute("receitaMesAtual", String.format("%.2f Kz", dashboardService.getReceitaMensal()));
        model.addAttribute("receitaMesAnterior", String.format("%.2f Kz", dashboardService.getReceitaMesAnterior()));
        model.addAttribute("variacaoReceita", dashboardService.getVariacaoReceita());

        // Dados para Gráficos (serializados como JSON para Chart.js)
        model.addAttribute("graficoMesesLabels", dashboardService.getUltimos12MesesLabels());
        model.addAttribute("graficoVendasData", dashboardService.getVendasPorMes());
        model.addAttribute("graficoReceitaData", dashboardService.getReceitaPorMes());
        model.addAttribute("graficoClientesData", dashboardService.getNovoClientesPorMes());

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
        model.addAttribute("produtosEstoqueBaixo", dashboardService.getProdutosEstoqueBaixo(5));
        return "estoqueBaixo";
    }

    @GetMapping("/dashboard/vendas-dia")
    public String vendasDia(Model model) {
        model.addAttribute("vendasDoDia", dashboardService.getComprasDoDia());
        return "vendasDia";
    }

    @GetMapping("/dashboard/receita-mensal")
    public String receitaMensal(Model model) {
        model.addAttribute("vendasDoMes", dashboardService.getComprasDoMes());
        return "receitaMensal";
    }
}