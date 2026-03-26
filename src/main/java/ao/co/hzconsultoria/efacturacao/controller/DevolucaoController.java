package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.service.DevolucaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/devolucoes")
public class DevolucaoController {
    @Autowired
    private DevolucaoService devolucaoService;

    @GetMapping
    public String listarDevolucoes(
            Model model,
            @RequestParam(value = "dataInicio", required = false) String dataInicio,
            @RequestParam(value = "dataFim", required = false) String dataFim,
            @RequestParam(value = "pesquisa", required = false) String pesquisa,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page
    ) {
        int pageSize = 10;
        List<Devolucao> todas = devolucaoService.listarDevolucoes();
        // Filtro por data
        if (dataInicio != null && !dataInicio.isEmpty()) {
            LocalDate inicio = LocalDate.parse(dataInicio);
            todas = todas.stream().filter(d -> d.getDataDevolucao() != null && !d.getDataDevolucao().toLocalDate().isBefore(inicio)).collect(Collectors.toList());
        }
        if (dataFim != null && !dataFim.isEmpty()) {
            LocalDate fim = LocalDate.parse(dataFim);
            todas = todas.stream().filter(d -> d.getDataDevolucao() != null && !d.getDataDevolucao().toLocalDate().isAfter(fim)).collect(Collectors.toList());
        }
        // Filtro por pesquisa
        if (pesquisa != null && !pesquisa.isEmpty()) {
            String pesq = pesquisa.toLowerCase();
            todas = todas.stream().filter(d ->
                String.valueOf(d.getId()).contains(pesq) ||
                (d.getItens() != null && d.getItens().stream().anyMatch(i -> i.getNomeProduto() != null && i.getNomeProduto().toLowerCase().contains(pesq)))
            ).collect(Collectors.toList());
        }
        // Paginação manual
        int totalPages = (int) Math.ceil((double) todas.size() / pageSize);
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, todas.size());
        List<Devolucao> devolucoes = todas.subList(fromIndex, toIndex);
        model.addAttribute("devolucoes", devolucoes);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("pesquisa", pesquisa);
        return "devolucoes";
    }

    @PostMapping("/registrar")
    @ResponseBody
    public Devolucao registrarDevolucao(@RequestBody Devolucao devolucao) {
        return devolucaoService.registrarDevolucao(devolucao);
    }
}