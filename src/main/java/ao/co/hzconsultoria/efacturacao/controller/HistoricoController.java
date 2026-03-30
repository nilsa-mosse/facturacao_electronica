package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import ao.co.hzconsultoria.efacturacao.service.VendaService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HistoricoController {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private VendaService vendaService;

    @GetMapping("/historico-vendas")
    public String historicoVendas(
            Model model,
            @RequestParam(value = "dataInicio", required = false) String dataInicio,
            @RequestParam(value = "dataFim", required = false) String dataFim,
            @RequestParam(value = "pesquisa", required = false) String pesquisa,
            @RequestParam(value = "tipo", required = false, defaultValue = "emitidas") String tipo,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page
    ) {
        int pageSize = 10;
        List<Compra> todasAsCompras = compraRepository.findAll();
        
        // Estatísticas para os cards
        long totalCount = todasAsCompras.size();
        long emitidasCount = todasAsCompras.stream().filter(c -> "EMITIDA".equals(c.getStatus())).count();
        long canceladasCount = todasAsCompras.stream().filter(c -> "CANCELADA".equals(c.getStatus())).count();

        List<Compra> filtradas = todasAsCompras;

        // Filtro por tipo (Status)
        if (!"todos".equalsIgnoreCase(tipo)) {
            String statusFiltro = tipo.equalsIgnoreCase("canceladas") ? "CANCELADA" : "EMITIDA";
            filtradas = filtradas.stream()
                .filter(c -> statusFiltro.equals(c.getStatus()))
                .collect(Collectors.toList());
        }

        // Filtro por data
        if (dataInicio != null && !dataInicio.isEmpty()) {
            LocalDate inicio = LocalDate.parse(dataInicio);
            filtradas = filtradas.stream().filter(c -> c.getDataCompra() != null && !c.getDataCompra().toLocalDate().isBefore(inicio)).collect(Collectors.toList());
        }
        if (dataFim != null && !dataFim.isEmpty()) {
            LocalDate fim = LocalDate.parse(dataFim);
            filtradas = filtradas.stream().filter(c -> c.getDataCompra() != null && !c.getDataCompra().toLocalDate().isAfter(fim)).collect(Collectors.toList());
        }
        // Filtro por pesquisa
        if (pesquisa != null && !pesquisa.isEmpty()) {
            String pesq = pesquisa.toLowerCase();
            filtradas = filtradas.stream().filter(c ->
                String.valueOf(c.getId()).contains(pesq) ||
                (c.getItens() != null && c.getItens().stream().anyMatch(i -> i.getNomeProduto() != null && i.getNomeProduto().toLowerCase().contains(pesq)))
            ).collect(Collectors.toList());
        }
        // Paginação manual
        int totalPages = (int) Math.ceil((double) filtradas.size() / pageSize);
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtradas.size());
        List<Compra> compras = filtradas.subList(fromIndex, toIndex);

        model.addAttribute("compras", compras);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("emitidasCount", emitidasCount);
        model.addAttribute("canceladasCount", canceladasCount);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("pesquisa", pesquisa);
        model.addAttribute("tipo", tipo);
        return "historicoVendas";
    }

    @GetMapping("/historico-vendas/cancelar/{id}")
    public String cancelarVenda(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (vendaService.cancelarVenda(id)) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura anulada com sucesso.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao anular factura.");
        }
        return "redirect:/historico-vendas?tipo=todos";
    }
}