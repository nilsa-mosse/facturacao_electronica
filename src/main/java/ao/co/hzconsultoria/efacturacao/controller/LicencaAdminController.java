package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.service.LicencaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/superadmin/licenca")
public class LicencaAdminController {

    @Autowired
    private LicencaService licencaService;

    // PIN de segurança mestre para gerar licenças (Camada extra de proteção)
    private static final String MASTER_PIN = "998877"; 

    @GetMapping("/gerador")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public String gerador(Model model) {
        return "geradorLicenca";
    }

    @GetMapping("/gerar")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public String gerarGet() {
        return "redirect:/superadmin/licenca/gerador";
    }

    @PostMapping("/gerar")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public String gerar(@RequestParam("machineId") String machineId,
                        @RequestParam("dataExpiracao") String dataExpiracao,
                        @RequestParam("horaExpiracao") String horaExpiracao,
                        @RequestParam("pin") String pin,
                        Model model) {
        
        // Validação da camada extra de segurança (PIN)
        if (!MASTER_PIN.equals(pin)) {
            model.addAttribute("erro", "PIN de Segurança Inválido! Acesso bloqueado.");
            return "geradorLicenca";
        }

        try {
            String fullDate = dataExpiracao + " " + horaExpiracao + ":00";
            String chave = licencaService.gerarChave(machineId, fullDate);
            
            model.addAttribute("chaveGerada", chave);
            model.addAttribute("machineId", machineId);
            model.addAttribute("dataExpiracao", dataExpiracao);
            model.addAttribute("horaExpiracao", horaExpiracao);
            model.addAttribute("sucesso", "Chave gerada com sucesso!");
        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao gerar chave: " + e.getMessage());
        }
        return "geradorLicenca";
    }
}
