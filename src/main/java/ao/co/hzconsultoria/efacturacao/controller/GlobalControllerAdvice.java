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
        // Pré-inicializar todas as permissões como falso por padrão para evitar erros de null no Thymeleaf (SpringEL)
        ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.forEach((modulo, itens) -> {
            model.addAttribute("modulo_" + modulo, false);
            itens.forEach(item -> model.addAttribute("modulo_" + modulo + "_" + item.getChave(), false));
        });

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
            
            if (isSuperAdmin) {
                // SuperAdmin tem acesso a tudo por padrão
                ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.forEach((modulo, itens) -> {
                    model.addAttribute("modulo_" + modulo, true);
                    itens.forEach(item -> model.addAttribute("modulo_" + modulo + "_" + item.getChave(), true));
                });
            } else if (isAdmin) {
                // Admin tem acesso a tudo EXCEPTO o Painel Global (SaaS)
                ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.forEach((modulo, itens) -> {
                    if (!modulo.equals("PAINEL_GLOBAL")) {
                        model.addAttribute("modulo_" + modulo, true);
                        itens.forEach(item -> model.addAttribute("modulo_" + modulo + "_" + item.getChave(), true));
                    }
                });
            } else if (auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
                ao.co.hzconsultoria.efacturacao.security.CustomUserDetails userDetails = (ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal();
                
                // Buscar permissões ao nível de Módulo configuradas na BD
                java.util.List<ao.co.hzconsultoria.efacturacao.model.PermissaoModulo> permissoesDb = permissaoRepo.findByUsuario_Id(userDetails.getId());
                
                if (permissoesDb.isEmpty()) {
                    // Sem registos na BD (Utilizador acabado de criar) -> Usar regras padrão
                    String userRole = userDetails.getRole();
                    ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.forEach((modulo, itens) -> {
                        boolean ativo = false;
                        if ("SUPERADMIN".equals(userRole)) {
                            ativo = true;
                        } else if ("ADMIN".equals(userRole)) {
                            ativo = !modulo.equals("PAINEL_GLOBAL");
                        } else if ("GESTOR".equals(userRole)) {
                            ativo = modulo.equals("DASHBOARD") || modulo.equals("VENDAS") || 
                                    modulo.equals("STOCK") || modulo.equals("ENTIDADES") || 
                                    modulo.equals("FACTURACAO") || modulo.equals("FINANCEIRO");
                        } else {
                            ativo = modulo.equals("VENDAS");
                        }
                        
                        if (ativo) {
                            model.addAttribute("modulo_" + modulo, true);
                            itens.forEach(item -> model.addAttribute("modulo_" + modulo + "_" + item.getChave(), true));
                        }
                    });
                } else {
                    // Utilizador já tem configuração feita no Controlo de Acesso
                    java.util.Set<String> modulosAtivos = new java.util.HashSet<>();
                    for (ao.co.hzconsultoria.efacturacao.model.PermissaoModulo p : permissoesDb) {
                        if (p.isAtivo()) {
                            modulosAtivos.add(p.getModulo());
                        }
                    }
                    
                    ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.forEach((modulo, itens) -> {
                        if (modulosAtivos.contains(modulo)) {
                            model.addAttribute("modulo_" + modulo, true);
                            itens.forEach(item -> model.addAttribute("modulo_" + modulo + "_" + item.getChave(), true));
                        }
                    });
                }
            }
        }
    }
}