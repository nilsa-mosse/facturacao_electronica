package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.GuiaRemessa;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import ao.co.hzconsultoria.efacturacao.service.GuiaRemessaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/guias")
public class GuiaRemessaController {

    @Autowired
    private GuiaRemessaService guiaRemessaService;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/listar")
    public String listarGuias(Model model) {
        model.addAttribute("guias", guiaRemessaService.listarTodas());
        return "listarGuias";
    }

    @GetMapping("/nova")
    public String novaGuia(Model model) {
        model.addAttribute("guia", new GuiaRemessa());
        model.addAttribute("clientes", clienteRepository.findAll());
        return "novaGuia";
    }

    @PostMapping("/salvar")
    public String salvarGuia(@ModelAttribute GuiaRemessa guia, RedirectAttributes redirectAttributes) {
        try {
            guiaRemessaService.salvar(guia);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Guia de Remessa emitida com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao emitir guia: " + e.getMessage());
        }
        return "redirect:/guias/listar";
    }

    @GetMapping("/gerar-da-fatura/{faturaId}")
    public String gerarDaFactura(@PathVariable Long faturaId, RedirectAttributes redirectAttributes) {
        try {
            guiaRemessaService.gerarGuiaAPartirDeFatura(faturaId);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Guia gerada automaticamente com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao gerar guia: " + e.getMessage());
        }
        return "redirect:/guias/listar";
    }
}
