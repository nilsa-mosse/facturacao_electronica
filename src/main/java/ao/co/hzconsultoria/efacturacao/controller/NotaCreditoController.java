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
        // NCs e NDs do novo fluxo AGT (Faturas com tipo NC ou ND)
        List<Fatura> faturasNC = faturaRepository.findByTipoDocumentoAndEmpresa_IdOrderByDataEmissaoDesc("NC", empresaId);
        List<Fatura> faturasND = faturaRepository.findByTipoDocumentoAndEmpresa_IdOrderByDataEmissaoDesc("ND", empresaId);
        
        List<Fatura> faturasNotas = new java.util.ArrayList<>();
        faturasNotas.addAll(faturasNC);
        faturasNotas.addAll(faturasND);
        faturasNotas.sort((f1, f2) -> {
            if (f1.getDataEmissao() == null || f2.getDataEmissao() == null) return 0;
            return f2.getDataEmissao().compareTo(f1.getDataEmissao());
        });
        
        // Mapa para guardar o número da factura original de cada NC/ND
        java.util.Map<Long, String> faturasOrigem = new java.util.HashMap<>();
        for (Fatura nc : faturasNotas) {
            if (nc.getCompra() != null) {
                List<Fatura> faturasDaCompra = faturaRepository.findByCompra(nc.getCompra());
                for (Fatura f : faturasDaCompra) {
                    // Ignora as próprias notas de crédito/débito para encontrar a factura original
                    if (!"NC".equals(f.getTipoDocumento()) && !"ND".equals(f.getTipoDocumento())) {
                        faturasOrigem.put(nc.getId(), f.getNumeroFatura());
                        break;
                    }
                }
            }
        }
        
        // NCs do fluxo legado
        List<NotaCredito> notasLegadas = notaCreditoService.listarTodas();
        model.addAttribute("faturasNC", faturasNotas);
        model.addAttribute("faturasOrigem", faturasOrigem);
        model.addAttribute("notasLegadas", notasLegadas);
        
        // Estatísticas para os cards
        model.addAttribute("totalNC", (long) faturasNC.size());
        model.addAttribute("totalND", (long) faturasND.size());
        
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
        
        String tipoNota = devolucao.getTipoNota();
        String label = "NC".equals(tipoNota) ? "Nota de Crédito" : "Nota de Débito";
        
        try {
            if (devolucao.getItens() != null) {
                devolucao.getItens().removeIf(item -> item.getProduto() == null || item.getProduto().getId() == null);
            }
 
            if (devolucao.getItens() == null || devolucao.getItens().isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Adicione pelo menos um produto para a nota.");
                return "redirect:/notas/novo";
            }
 
            devolucaoService.registrarDevolucao(devolucao, empresaId, usuarioId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", label + " registada com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao emitir " + label + ": " + e.getMessage());
            return "redirect:/notas/novo";
        }
        
        return "redirect:/notas/listar";
    }
}
