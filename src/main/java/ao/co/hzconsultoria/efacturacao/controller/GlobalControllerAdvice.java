package ao.co.hzconsultoria.efacturacao.controller;


import ao.co.hzconsultoria.efacturacao.service.ConfiguracaoSistemaService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Autowired;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository empresaRepository;

    @Autowired
    private ConfiguracaoSistemaService cfgService;

    @ModelAttribute
    public void addAttributes(Model model) {
        String tema = "light";
        try {
            ao.co.hzconsultoria.efacturacao.model.Sistema sistemaConfig = cfgService.getSistema();
            if (sistemaConfig != null && sistemaConfig.getTema() != null) {
                String temp = sistemaConfig.getTema();
                if (temp.equalsIgnoreCase("escuro") || temp.equalsIgnoreCase("dark")) {
                    tema = "dark";
                } else {
                    tema = "light";
                }
            }
        } catch (Exception ignored) {
            // BD pode estar indisponível durante arranque inicial
        }
        model.addAttribute("globalTema", tema);

        // Adicionar Empresa atual ao modelo
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = false;
        
        if (auth != null) {
            isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
            
            if (auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
                ao.co.hzconsultoria.efacturacao.security.CustomUserDetails userDetails = (ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal();
                Long empresaId = userDetails.getEmpresaId();
                if (empresaId != null) {
                    ao.co.hzconsultoria.efacturacao.model.Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
                    model.addAttribute("currentEmpresa", empresa);
                }
            }
        }
        model.addAttribute("isSuperAdmin", isSuperAdmin);
    }
}