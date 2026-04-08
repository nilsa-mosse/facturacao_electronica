package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.GuiaRemessa;
import ao.co.hzconsultoria.efacturacao.model.ItemGuiaRemessa;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import ao.co.hzconsultoria.efacturacao.service.GuiaRemessaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.File;
import java.util.ArrayList;

@Controller
@RequestMapping("/guias")
public class GuiaRemessaController {

    @Autowired
    private GuiaRemessaService guiaRemessaService;

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/listar")
    public String listarGuias(Model model) {
        java.util.List<GuiaRemessa> guias = guiaRemessaService.listarTodas();
        long activas = guias.stream().filter(g -> "ATIVA".equals(g.getStatus())).count();
        long substituidas = guias.stream().filter(g -> "SUBSTITUIDA".equals(g.getStatus())).count();
        
        model.addAttribute("guias", guias);
        model.addAttribute("totalGuias", guias.size());
        model.addAttribute("guiasActivas", activas);
        model.addAttribute("guiasSubstituidas", substituidas);
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
            if (guia.getGuiaReferencia() != null && guia.getGuiaReferencia().getId() != null) {
                System.out.println("DEBUG: Recebida guia com referência: ID " + guia.getGuiaReferencia().getId());
            }
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

    @GetMapping("/api/pdf-url/{id}")
    @ResponseBody
    public java.util.Map<String, String> obterPdfUrl(@PathVariable Long id) {
        GuiaRemessa guia = guiaRemessaService.buscarPorId(id);
        java.util.Map<String, String> res = new java.util.HashMap<>();
        if (guia != null) {
            // Garantir que o PDF existe (fallback para guias antigas)
            File pdfFile = new File("src/main/resources/static/guias/" + guia.getNumeroGuia() + ".pdf");
            if (!pdfFile.exists()) {
                guiaRemessaService.salvar(guia); // Isto dispara a geração do PDF
            }
            res.put("url", "/guias/" + guia.getNumeroGuia() + ".pdf");
        }
        return res;
    }

    @PostMapping("/anular")
    public String anular(@RequestParam("id") Long id, @RequestParam("motivo") String motivo, RedirectAttributes ra) {
        guiaRemessaService.anularGuia(id, motivo);
        ra.addFlashAttribute("mensagemSucesso", "Guia anulada com sucesso.");
        return "redirect:/guias/listar";
    }

    @GetMapping("/converter/{id}")
    public String converter(@PathVariable Long id, RedirectAttributes ra) {
        try {
            guiaRemessaService.converterParaFactura(id);
            ra.addFlashAttribute("mensagemSucesso", "Guia convertida em Factura com sucesso.");
        } catch (Exception e) {
            ra.addFlashAttribute("mensagemErro", "Erro ao converter guia: " + e.getMessage());
        }
        return "redirect:/guias/listar";
    }

    @GetMapping("/rectificar/{id}")
    public String rectificar(@PathVariable Long id, Model model) {
        GuiaRemessa original = guiaRemessaService.buscarPorId(id);
        GuiaRemessa nova = new GuiaRemessa();
        nova.setCliente(original.getCliente());
        nova.setLocalCarga(original.getLocalCarga());
        nova.setLocalDescarga(original.getLocalDescarga());
        nova.setMatriculaViatura(original.getMatriculaViatura());
        nova.setMotorista(original.getMotorista());
        nova.setGuiaReferencia(original);
        
        java.util.List<ItemGuiaRemessa> novosItens = new ArrayList<>();
        if (original.getItens() != null) {
            for (ItemGuiaRemessa item : original.getItens()) {
                ItemGuiaRemessa novoItem = new ItemGuiaRemessa();
                novoItem.setNomeProduto(item.getNomeProduto());
                novoItem.setQuantidade(item.getQuantidade());
                novoItem.setUnidadeMedida(item.getUnidadeMedida());
                novoItem.setGuiaRemessa(nova);
                novosItens.add(novoItem);
            }
        }
        nova.setItens(novosItens);
        
        model.addAttribute("clientes", clienteRepository.findAll());
        return "novaGuia";
    }

    @GetMapping("/tracking/{id}")
    @ResponseBody
    public GuiaRemessa obterTracking(@PathVariable Long id) {
        return guiaRemessaService.buscarPorId(id);
    }

    @PostMapping("/tracking/adicionar")
    public String adicionarTracking(@RequestParam Long id, @RequestParam String trackingStatus, 
                                   @RequestParam String local, @RequestParam String obs, RedirectAttributes ra) {
        guiaRemessaService.adicionarEventoTracking(id, trackingStatus, local, obs);
        ra.addFlashAttribute("mensagemSucesso", "Evento de rastreio registado com sucesso!");
        return "redirect:/guias/listar";
    }
}
