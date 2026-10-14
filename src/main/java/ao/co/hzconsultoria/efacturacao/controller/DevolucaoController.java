package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.service.DevolucaoService;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/devolucoes")
public class DevolucaoController {

    @Autowired
    private DevolucaoService devolucaoService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping
    public String listarDevolucoes(
            Model model,
            @RequestParam(value = "pesquisa", required = false) String pesquisa,
            @RequestParam(value = "dataInicio", required = false) String dataInicio,
            @RequestParam(value = "dataFim", required = false) String dataFim,
            @RequestParam(value = "tipo", required = false, defaultValue = "todos") String tipo) {

        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        List<Devolucao> todas = devolucaoService.listarTodas(empresaId);

        // Estatísticas para os cards superiores
        long totalDevolucoes = todas.size();
        double totalValorDevolvido = todas.stream().mapToDouble(d -> d.getTotal() != null ? d.getTotal() : 0.0).sum();
        long totalNcEmitidas = todas.stream().filter(d -> d.getNotaCredito() != null).count();
        LocalDate hoje = LocalDate.now();
        long totalHoje = todas.stream()
                .filter(d -> d.getDataDevolucao() != null && d.getDataDevolucao().toLocalDate().isEqual(hoje))
                .count();

        List<Devolucao> filtradas = todas;

        // Filtro por tipo de documento gerado
        if ("COM_NC".equalsIgnoreCase(tipo)) {
            filtradas = filtradas.stream().filter(d -> d.getNotaCredito() != null).collect(Collectors.toList());
        } else if ("SEM_NC".equalsIgnoreCase(tipo)) {
            filtradas = filtradas.stream().filter(d -> d.getNotaCredito() == null).collect(Collectors.toList());
        }

        // Filtro por datas
        if (dataInicio != null && !dataInicio.isEmpty()) {
            try {
                LocalDate inicio = LocalDate.parse(dataInicio);
                filtradas = filtradas.stream()
                        .filter(d -> d.getDataDevolucao() != null && !d.getDataDevolucao().toLocalDate().isBefore(inicio))
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}
        }
        if (dataFim != null && !dataFim.isEmpty()) {
            try {
                LocalDate fim = LocalDate.parse(dataFim);
                filtradas = filtradas.stream()
                        .filter(d -> d.getDataDevolucao() != null && !d.getDataDevolucao().toLocalDate().isAfter(fim))
                        .collect(Collectors.toList());
            } catch (Exception ignored) {}
        }

        // Filtro por pesquisa (ID, Motivo, Operador, Fatura Ref, Nota de Crédito)
        if (pesquisa != null && !pesquisa.trim().isEmpty()) {
            String pLower = pesquisa.trim().toLowerCase();
            filtradas = filtradas.stream().filter(d ->
                    String.valueOf(d.getId()).contains(pLower) ||
                    (d.getMotivo() != null && d.getMotivo().toLowerCase().contains(pLower)) ||
                    (d.getUsuario() != null && d.getUsuario().getNome() != null && d.getUsuario().getNome().toLowerCase().contains(pLower)) ||
                    (d.getFatura() != null && d.getFatura().getNumeroFatura() != null && d.getFatura().getNumeroFatura().toLowerCase().contains(pLower)) ||
                    (d.getNotaCredito() != null && d.getNotaCredito().getNumeroFatura() != null && d.getNotaCredito().getNumeroFatura().toLowerCase().contains(pLower))
            ).collect(Collectors.toList());
        }

        // Ordenar as mais recentes primeiro
        filtradas.sort((d1, d2) -> {
            if (d1.getDataDevolucao() == null) return 1;
            if (d2.getDataDevolucao() == null) return -1;
            return d2.getDataDevolucao().compareTo(d1.getDataDevolucao());
        });

        model.addAttribute("devolucoes", filtradas);
        model.addAttribute("totalDevolucoes", totalDevolucoes);
        model.addAttribute("totalValorDevolvido", totalValorDevolvido);
        model.addAttribute("totalNcEmitidas", totalNcEmitidas);
        model.addAttribute("totalHoje", totalHoje);
        model.addAttribute("pesquisa", pesquisa);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("tipo", tipo);

        return "listarDevolucoes";
    }

    @GetMapping("/novo")
    public String novoFormulario(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        List<Produto> produtos = produtoRepository.findByEmpresa_Id(empresaId);
        
        model.addAttribute("devolucao", new Devolucao());
        model.addAttribute("produtos", produtos);
        return "cadastroDevolucao";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Devolucao devolucao, RedirectAttributes redirectAttributes) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Long usuarioId = SecurityUtils.getCurrentUserId();
        
        try {
            // Limpar fatura com ID nulo para evitar erro transiente no Hibernate
            if (devolucao.getFatura() != null && devolucao.getFatura().getId() == null) {
                devolucao.setFatura(null);
            }

            // Remover itens nulos (caso o JS envie algum vazio)
            if (devolucao.getItens() != null) {
                devolucao.getItens().removeIf(item -> item.getProduto() == null || item.getProduto().getId() == null);
            }

            if (devolucao.getItens() == null || devolucao.getItens().isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Adicione pelo menos um produto para devolver.");
                return "redirect:/devolucoes/novo";
            }

            devolucaoService.registrarDevolucao(devolucao, empresaId, usuarioId);
            redirectAttributes.addFlashAttribute("mensagem", "Devolução registrada com sucesso! Stock atualizado e Nota Fiscal processada.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao registrar devolução: " + e.getMessage());
            return "redirect:/devolucoes/novo";
        }
        
        return "redirect:/devolucoes";
    }
}