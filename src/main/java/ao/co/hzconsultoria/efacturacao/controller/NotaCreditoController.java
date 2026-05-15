package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.NotaCredito;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import ao.co.hzconsultoria.efacturacao.service.NotaCreditoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/notas")
public class NotaCreditoController {

    @Autowired
    private NotaCreditoService notaCreditoService;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/listar")
    public String listarNotas(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        // NCs do novo fluxo AGT (Fatura com tipo NC)
        List<Fatura> faturasNC = faturaRepository.findByTipoDocumentoAndEmpresa_IdOrderByDataEmissaoDesc("NC", empresaId);
        
        // Mapa para guardar o número da factura original de cada NC
        java.util.Map<Long, String> faturasOrigem = new java.util.HashMap<>();
        for (Fatura nc : faturasNC) {
            if (nc.getCompra() != null) {
                List<Fatura> faturasDaCompra = faturaRepository.findByCompra(nc.getCompra());
                for (Fatura f : faturasDaCompra) {
                    // Ignora as próprias notas de crédito para encontrar a factura FT, FR ou FP original
                    if (!"NC".equals(f.getTipoDocumento())) {
                        faturasOrigem.put(nc.getId(), f.getNumeroFatura());
                        break;
                    }
                }
            }
        }
        
        // NCs do fluxo legado
        List<NotaCredito> notasLegadas = notaCreditoService.listarTodas();
        model.addAttribute("faturasNC", faturasNC);
        model.addAttribute("faturasOrigem", faturasOrigem);
        model.addAttribute("notasLegadas", notasLegadas);
        return "listarNotas";
    }

    @GetMapping("/selecionar-fatura")
    public String seleccionarFactura(Model model) {
        model.addAttribute("facturas", compraRepository.findAll());
        return "selecionarFacturaNota";
    }

    @GetMapping("/nova/{facturaId}")
    public String novaNota(@PathVariable Long facturaId, Model model) {
        NotaCredito nota = notaCreditoService.prepararNotaDeFactura(facturaId);
        model.addAttribute("nota", nota);
        return "novaNota";
    }

    @PostMapping("/salvar")
    public String salvarNota(@ModelAttribute NotaCredito nota, RedirectAttributes redirectAttributes) {
        try {
            notaCreditoService.salvar(nota);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Nota de Crédito emitida com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao emitir nota: " + e.getMessage());
        }
        return "redirect:/notas/listar";
    }

    // --- NOVO FLUXO: DEVOLUÇÃO / NC ---

    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.DevolucaoService devolucaoService;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository produtoRepository;

    @GetMapping("/novo")
    public String novoFormulario(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        java.util.List<ao.co.hzconsultoria.efacturacao.model.Produto> produtos = produtoRepository.findByEmpresa_Id(empresaId);
        
        model.addAttribute("devolucao", new ao.co.hzconsultoria.efacturacao.model.Devolucao());
        model.addAttribute("produtos", produtos);
        return "novaNotaDevolucao";
    }

    @PostMapping("/salvar-devolucao")
    public String salvarDevolucao(@ModelAttribute ao.co.hzconsultoria.efacturacao.model.Devolucao devolucao, RedirectAttributes redirectAttributes) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Long usuarioId = SecurityUtils.getCurrentUserId();
        
        try {
            if (devolucao.getItens() != null) {
                devolucao.getItens().removeIf(item -> item.getProduto() == null || item.getProduto().getId() == null);
            }

            if (devolucao.getItens() == null || devolucao.getItens().isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Adicione pelo menos um produto para devolver.");
                return "redirect:/notas/novo";
            }

            devolucaoService.registrarDevolucao(devolucao, empresaId, usuarioId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Nota de Crédito registada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao emitir Nota de Crédito: " + e.getMessage());
            return "redirect:/notas/novo";
        }
        
        return "redirect:/notas/listar";
    }
}
