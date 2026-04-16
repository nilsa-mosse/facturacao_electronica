package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import ao.co.hzconsultoria.efacturacao.service.VendaService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

@Controller
public class HistoricoController {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

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
        long totalVendas = todasAsCompras.size();
        long emitidasCount = todasAsCompras.stream().filter(c -> "EMITIDA".equals(c.getStatus())).count();
        long substituidasCount = todasAsCompras.stream().filter(c -> "SUBSTITUIDA".equals(c.getStatus())).count();
        long canceladasCount = todasAsCompras.stream().filter(c -> "CANCELADA".equals(c.getStatus())).count();

        List<Compra> filtradas = todasAsCompras;

        // Filtro por tipo (Tipo de Documento ou Status)
        if (!"todos".equalsIgnoreCase(tipo)) {
            if ("CANCELADA".equalsIgnoreCase(tipo) || "canceladas".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                    .filter(c -> "CANCELADA".equals(c.getStatus()))
                    .collect(Collectors.toList());
            } else if ("SUBSTITUIDA".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                    .filter(c -> "SUBSTITUIDA".equals(c.getStatus()))
                    .collect(Collectors.toList());
            } else if ("EMITIDA".equalsIgnoreCase(tipo) || "emitidas".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                    .filter(c -> "EMITIDA".equals(c.getStatus()))
                    .collect(Collectors.toList());
            } else if ("FT".equalsIgnoreCase(tipo)) {
                // Se pedir explicitamente FT, mostramos as que não são Pro-formas, de preferência emitidas
                filtradas = filtradas.stream()
                    .filter(c -> !"FP".equals(c.getTipoDocumento()))
                    .collect(Collectors.toList());
            } else if ("FP".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                    .filter(c -> "FP".equals(c.getTipoDocumento()))
                    .collect(Collectors.toList());
            }
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
        model.addAttribute("totalVendas", totalVendas);
        model.addAttribute("emitidasCount", emitidasCount);
        model.addAttribute("substituidasCount", substituidasCount);
        model.addAttribute("canceladasCount", canceladasCount);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("pesquisa", pesquisa);
        model.addAttribute("tipo", tipo);
        return "historicoVendas";
    }

    @PostMapping("/historico-vendas/anular")
    public String anularVenda(
            @RequestParam(value = "id", required = false) Long id, 
            @RequestParam(value = "motivo", required = false) String motivo, 
            RedirectAttributes redirectAttributes) {
            
        if (id == null || motivo == null || motivo.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Dados inválidos para anulação.");
            return "redirect:/historico-vendas";
        }

        if (vendaService.cancelarVenda(id, motivo)) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura anulada com sucesso.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao anular factura.");
        }
        return "redirect:/historico-vendas?tipo=todos";
    }

    @GetMapping("/historico-vendas/rectificar/{id}")
    public String prepararRectificacao(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent()) {
            Compra original = compraOpt.get();
            Compra nova = new Compra();
            nova.setCliente(original.getCliente());
            nova.setFaturaReferencia(original);
            nova.setNomeCliente(original.getNomeCliente());
            nova.setNifCliente(original.getNifCliente());
            nova.setFormaPagamento(original.getFormaPagamento());
            
            // Clonar itens
            List<ItemCompra> novosItens = original.getItens().stream().map(i -> {
                ItemCompra ni = new ItemCompra();
                ni.setNomeProduto(i.getNomeProduto());
                ni.setQuantidade(i.getQuantidade());
                ni.setPreco(i.getPreco());
                ni.setSubtotal(i.getSubtotal());
                ni.setIva(i.getIva());
                return ni;
            }).collect(Collectors.toList());
            nova.setItens(novosItens);
            
            model.addAttribute("compra", nova);
            model.addAttribute("clientes", clienteRepository.findAll());
            
            return "rectificarFactura";
        }
        redirectAttributes.addFlashAttribute("mensagemErro", "Factura não encontrada.");
        return "redirect:/historico-vendas";
    }

    @PostMapping("/historico-vendas/salvar-rectificacao")
    public String salvarRectificacao(Compra compra, RedirectAttributes redirectAttributes) {
        try {
            // 1. Marcar a original como SUBSTITUIDA
            if (compra.getFaturaReferencia() != null) {
                Compra original = compraRepository.findById(compra.getFaturaReferencia().getId()).orElse(null);
                if (original != null) {
                    original.setStatus("SUBSTITUIDA");
                    compraRepository.save(original);
                }
            }
            
            // 2. Salvar a nova factura
            vendaService.finalizarVenda(compra, "FT");
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura rectificada com sucesso!");
            return "redirect:/historico-vendas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao rectificar factura: " + e.getMessage());
            return "redirect:/historico-vendas";
        }
    }

    @GetMapping("/historico-vendas/restaurar/{id}")
    public String restaurarVenda(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (vendaService.restaurarVenda(id)) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura restaurada com sucesso.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao restaurar factura.");
        }
        return "redirect:/historico-vendas?tipo=canceladas";
    }

    @GetMapping("/api/compras/fatura-url/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> obterFaturaUrl(@PathVariable Long id) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent()) {
            List<Fatura> faturas = faturaRepository.findAll().stream()
                .filter(f -> f.getCompra() != null && f.getCompra().getId().equals(id))
                .sorted((f1, f2) -> f2.getId().compareTo(f1.getId()))
                .collect(Collectors.toList());
            
            if (!faturas.isEmpty()) {
                Map<String, String> res = new HashMap<>();
                res.put("url", "/faturas/" + faturas.get(0).getNumeroFatura() + ".pdf");
                return ResponseEntity.ok(res);
            }
        }
        return ResponseEntity.notFound().build();
    }
}