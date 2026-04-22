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

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.PermissaoModuloRepository permissaoRepo;

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
                model.addAttribute("usuario_NOME", userDetails.getNome());
                Long empresaId = userDetails.getEmpresaId();
                if (empresaId != null) {
                    ao.co.hzconsultoria.efacturacao.model.Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
                    model.addAttribute("currentEmpresa", empresa);
                }
            }
        }
        model.addAttribute("isSuperAdmin", isSuperAdmin);

        // Injetar permissões de acesso
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String role = "";
            if (auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
                role = ((ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal()).getRole();
            }
            
            // Inicializar todos como falso por padrão
            model.addAttribute("modulo_DASHBOARD", false);
            model.addAttribute("modulo_VENDAS", false);
            model.addAttribute("modulo_STOCK", false);
            model.addAttribute("modulo_ENTIDADES", false);
            model.addAttribute("modulo_FACTURACAO", false);
            model.addAttribute("modulo_FINANCEIRO", false);
            model.addAttribute("modulo_ADMINISTRACAO", false);
            
            // SUPERADMIN tem acesso a tudo por padrão
            if (isSuperAdmin) {
                model.addAttribute("modulo_DASHBOARD", true);
                model.addAttribute("modulo_VENDAS", true);
                model.addAttribute("modulo_STOCK", true);
                model.addAttribute("modulo_ENTIDADES", true);
                model.addAttribute("modulo_FACTURACAO", true);
                model.addAttribute("modulo_FINANCEIRO", true);
                model.addAttribute("modulo_ADMINISTRACAO", true);
            } else {
                Long userId = ((ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal()).getId();
                java.util.List<ao.co.hzconsultoria.efacturacao.model.PermissaoModulo> permissoes = permissaoRepo.findByUsuario_Id(userId);
                for (ao.co.hzconsultoria.efacturacao.model.PermissaoModulo p : permissoes) {
                    model.addAttribute("modulo_" + p.getModulo(), p.isAtivo());
                }
            }
        }
    }
}