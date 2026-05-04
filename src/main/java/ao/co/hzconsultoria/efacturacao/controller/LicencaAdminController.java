package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.LicencaGerada;
import ao.co.hzconsultoria.efacturacao.repository.LicencaGeradaRepository;
import ao.co.hzconsultoria.efacturacao.service.LicencaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/superadmin/licenca")
public class LicencaAdminController {

    @Autowired
    private LicencaService licencaService;
    
    @Autowired
    private LicencaGeradaRepository licencaGeradaRepository;

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
                        @RequestParam(value = "clienteNome", required = false, defaultValue = "") String clienteNome,
                        @RequestParam(value = "observacoes", required = false, defaultValue = "") String observacoes,
                        Model model) {
        
        // Validação da camada extra de segurança (PIN)
        if (!MASTER_PIN.equals(pin)) {
            model.addAttribute("erro", "PIN de Segurança Inválido! Acesso bloqueado.");
            return "geradorLicenca";
        }

        try {
            String fullDate = dataExpiracao + " " + horaExpiracao + ":00";
            String chave = licencaService.gerarChave(machineId, fullDate);
            
            // Gravar o registo na base de dados
            LicencaGerada novaLicenca = new LicencaGerada();
            novaLicenca.setMachineId(machineId);
            novaLicenca.setChaveGerada(chave);
            novaLicenca.setClienteNome(clienteNome);
            novaLicenca.setObservacoes(observacoes);
            novaLicenca.setDataEmissao(LocalDateTime.now());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            novaLicenca.setDataExpiracao(LocalDateTime.parse(fullDate, formatter));
            
            licencaGeradaRepository.save(novaLicenca);

            model.addAttribute("chaveGerada", chave);
            model.addAttribute("machineId", machineId);
            model.addAttribute("dataExpiracao", dataExpiracao);
            model.addAttribute("horaExpiracao", horaExpiracao);
            model.addAttribute("sucesso", "Chave gerada e registada com sucesso!");
        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao gerar chave: " + e.getMessage());
        }
        return "geradorLicenca";
    }
    
    @GetMapping("/gestao")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public String gestaoLicencas(Model model) {
        List<LicencaGerada> licencas = licencaGeradaRepository.findAll();
        // Ordenar da mais recente para a mais antiga (assumindo que o ID é sequencial)
        licencas.sort((l1, l2) -> l2.getId().compareTo(l1.getId()));
        
        model.addAttribute("licencas", licencas);
        return "gestaoLicencas";
    }
    
    @GetMapping("/alternar-estado/{id}")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public String alternarEstado(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        LicencaGerada licenca = licencaGeradaRepository.findById(id).orElse(null);
        if (licenca != null) {
            licenca.setAtiva(!licenca.isAtiva());
            licencaGeradaRepository.save(licenca);
            redirectAttributes.addFlashAttribute("sucesso", "Estado da licença alterado com sucesso!");
        } else {
            redirectAttributes.addFlashAttribute("erro", "Licença não encontrada!");
        }
        return "redirect:/superadmin/licenca/gestao";
    }
}
