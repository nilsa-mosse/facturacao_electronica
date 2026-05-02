package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import ao.co.hzconsultoria.efacturacao.service.LicencaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SiteController {

    @Autowired
    private LicencaService licencaService;

    @Autowired
    private ConfiguracaoSistemaRepository configRepo;

    @GetMapping("/licenca-expirada")
    public String licencaExpirada(Model model) {
        model.addAttribute("machineId", licencaService.getMachineId());
        return "licenca-expirada";
    }

    @PostMapping("/ativar-licenca")
    public String ativarLicenca(@RequestParam("chave") String chave, RedirectAttributes redirectAttributes) {
        if (licencaService.validarChave(chave)) {
            ConfiguracaoSistemaEntity config = configRepo.findById(1L).orElse(new ConfiguracaoSistemaEntity());
            config.setLicencaChaveAtivacao(chave);
            configRepo.save(config);
            
            redirectAttributes.addFlashAttribute("mensagem", "Sistema ativado com sucesso!");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("erro", "Chave de ativação inválida ou expirada para esta máquina.");
            return "redirect:/licenca-expirada";
        }
    }
}