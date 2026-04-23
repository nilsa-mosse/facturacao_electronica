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
        // Expor papel Admin para uso nos templates de menu
        if (auth != null && auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
            String roleAtual = ((ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal()).getRole();
            model.addAttribute("isAdmin", "ADMIN".equals(roleAtual));
        } else {
            model.addAttribute("isAdmin", false);
        }

        // Injetar permissões de acesso
        if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String role = "";
            if (auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
                role = ((ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal()).getRole();
            }
            
            // Injetar permissões de acesso granulares (RBAC)
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (isSuperAdmin || isAdmin) {
                // Admin e SuperAdmin têm acesso a tudo por padrão
                ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.forEach((modulo, itens) -> {
                    model.addAttribute("modulo_" + modulo, true);
                    itens.forEach(item -> model.addAttribute("modulo_" + modulo + "_" + item.getChave(), true));
                });
            } else if (auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
                ao.co.hzconsultoria.efacturacao.security.CustomUserDetails userDetails = (ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal();
                java.util.Set<String> perms = userDetails.getPermissions();
                
                ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.forEach((modulo, itens) -> {
                    boolean hasModulo = false;
                    for (ao.co.hzconsultoria.efacturacao.config.ModuloItens.ItemDef item : itens) {
                        String key = modulo + "_" + item.getChave();
                        if (perms != null && perms.contains(key)) {
                            model.addAttribute("modulo_" + key, true);
                            hasModulo = true;
                        }
                    }
                    if (hasModulo) {
                        model.addAttribute("modulo_" + modulo, true);
                    }
                });
            }
        }
    }
}