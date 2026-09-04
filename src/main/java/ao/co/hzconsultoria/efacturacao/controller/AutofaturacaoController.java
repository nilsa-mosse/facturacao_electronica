package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.repository.FornecedorRepository;
import ao.co.hzconsultoria.efacturacao.service.FaturaService;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/autofaturacao")
public class AutofaturacaoController {

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @GetMapping({"", "/", "/listar"})
    public String listar(Model model) {
        Long empId = SecurityUtils.getCurrentEmpresaId();
        List<Fatura> autofaturas = faturaRepository.findByTipoDocumentoAndEmpresa_IdOrderByDataEmissaoDesc("AF", empId);
        
        double totalGeral = autofaturas.stream().mapToDouble(f -> f.getTotal() != null ? f.getTotal() : 0.0).sum();
        double totalRetencoes = autofaturas.stream().mapToDouble(f -> f.getValorRetencao() != null ? f.getValorRetencao() : 0.0).sum();

        model.addAttribute("autofaturas", autofaturas);
        model.addAttribute("totalCount", autofaturas.size());
        model.addAttribute("totalGeral", totalGeral);
        model.addAttribute("totalRetencoes", totalRetencoes);
        return "autofaturacao/index";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("compra", new Compra());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("fornecedores", fornecedorRepository.findAll());
        return "autofaturacao/nova";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Compra compra,
                         @RequestParam(value = "percentualRetencao", defaultValue = "6.5") Double percentualRetencao,
                         @RequestParam(value = "observacoes", required = false) String observacoes,
                         RedirectAttributes ra) {
        try {
            compra.setDataCompra(LocalDateTime.now());
            compra.setTipoDocumento("AF");
            compra.setStatus("PAGA");
            
            // Vincular itens
            if (compra.getItens() != null) {
                for (ItemCompra item : compra.getItens()) {
                    item.setCompra(compra);
                    if (item.getSubtotal() == null && item.getPreco() != null && item.getQuantidade() != null) {
                        item.setSubtotal(item.getPreco() * item.getQuantidade());
                    }
                }
            } else {
                compra.setItens(new ArrayList<>());
            }

            // Calcular totais
            double total = compra.getItens().stream().mapToDouble(i -> i.getSubtotal() != null ? i.getSubtotal() : 0.0).sum();
            compra.setTotal(total);

            Compra compraSalva = compraRepository.save(compra);
            Fatura af = faturaService.emitirAutofaturacao(compraSalva, percentualRetencao, observacoes);

            ra.addFlashAttribute("mensagemSucesso", "Fatura de Autofaturação (" + af.getNumeroFatura() + ") emitida e comunicada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("mensagemErro", "Erro ao emitir Autofaturação: " + e.getMessage());
        }
        return "redirect:/autofaturacao";
    }
}
