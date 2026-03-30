package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.NotaCredito;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.service.NotaCreditoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notas")
public class NotaCreditoController {

    @Autowired
    private NotaCreditoService notaCreditoService;

    @Autowired
    private CompraRepository compraRepository;

    @GetMapping("/listar")
    public String listarNotas(Model model) {
        model.addAttribute("notas", notaCreditoService.listarTodas());
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
}
