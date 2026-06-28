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
    private ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository configuracaoEmpresaRepository;

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

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long empresaId = null;
        if (auth != null && auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
            ao.co.hzconsultoria.efacturacao.security.CustomUserDetails userDetails = (ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal();
            empresaId = userDetails.getEmpresaId();
        }

        String tema = "light";
        String nomeSistema = "Kwanza ERP";
        String versaoSistema = "1.0.0";
        boolean exibirDatasValidade = true;
        boolean setupCompleto = true;
        try {
            if (empresaId != null) {
                java.util.Optional<ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa> optConfig = configuracaoEmpresaRepository.findByEmpresa_Id(empresaId);
                if (optConfig.isPresent()) {
                    ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa config = optConfig.get();
                    if (config.getSistemaNome() != null) nomeSistema = config.getSistemaNome();
                    if (config.getSistemaVersao() != null) versaoSistema = config.getSistemaVersao();
                    exibirDatasValidade = config.isExibirDatasValidade();
                    setupCompleto = config.isSetupCompleto();
                    String temp = config.getSistemaTema();
                    if (temp != null && (temp.equalsIgnoreCase("escuro") || temp.equalsIgnoreCase("dark"))) {
                        tema = "dark";
                    }
                }
            } else {
                ao.co.hzconsultoria.efacturacao.model.Sistema sistemaConfig = cfgService.getSistema();
                if (sistemaConfig != null) {
                    if (sistemaConfig.getNome() != null) nomeSistema = sistemaConfig.getNome();
                    if (sistemaConfig.getVersao() != null) versaoSistema = sistemaConfig.getVersao();
                    exibirDatasValidade = sistemaConfig.isExibirDatasValidade();
                    String temp = sistemaConfig.getTema();
                    if (temp != null && (temp.equalsIgnoreCase("escuro") || temp.equalsIgnoreCase("dark"))) {
                        tema = "dark";
                    }
                }
            }
        } catch (Exception ignored) {
            // BD pode estar indisponível durante arranque inicial
        }
        model.addAttribute("globalTema", tema);
        model.addAttribute("globalSistemaNome", nomeSistema);
        model.addAttribute("globalSistemaVersao", versaoSistema);
        model.addAttribute("globalSetupCompleto", setupCompleto);

        String sistemaLogotipo = "/img/logo.png";
        try {
            // Priority 1: use the current company logo
            if (empresaId != null) {
                ao.co.hzconsultoria.efacturacao.model.Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
                if (empresa != null && empresa.getLogotipo() != null && !empresa.getLogotipo().isEmpty()) {
                    sistemaLogotipo = empresa.getLogotipo();
                }
            }
            // Priority 2: fallback to company-specific system logo or global system logo if company logo is not set
            if ("/img/logo.png".equals(sistemaLogotipo)) {
                if (empresaId != null) {
                    java.util.Optional<ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa> optConfig = configuracaoEmpresaRepository.findByEmpresa_Id(empresaId);
                    if (optConfig.isPresent() && optConfig.get().getSistemaLogotipo() != null) {
                        sistemaLogotipo = optConfig.get().getSistemaLogotipo();
                    }
                }
                if ("/img/logo.png".equals(sistemaLogotipo)) {
                    ao.co.hzconsultoria.efacturacao.model.Sistema sistemaConfig = cfgService.getSistema();
                    if (sistemaConfig != null && sistemaConfig.getLogotipo() != null) {
                        sistemaLogotipo = sistemaConfig.getLogotipo();
                    }
                }
            }
        } catch (Exception ignored) {}
        model.addAttribute("globalSistemaLogotipo", sistemaLogotipo);
        model.addAttribute("globalExibirDatasValidade", exibirDatasValidade);

        // Adicionar Empresa atual ao modelo
        boolean isSuperAdmin = false;
        
        if (auth != null) {
            isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
            
            if (auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
                ao.co.hzconsultoria.efacturacao.security.CustomUserDetails userDetails = (ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal();
                model.addAttribute("usuario_NOME", userDetails.getNome());
                empresaId = userDetails.getEmpresaId();
                if (empresaId != null) {
                    ao.co.hzconsultoria.efacturacao.model.Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
                    model.addAttribute("currentEmpresa", empresa);
                }
            }
        }
        model.addAttribute("isSuperAdmin", isSuperAdmin);
        // Expor papel Admin e Operador para uso nos templates de menu
        if (auth != null && auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
            String roleAtual = ((ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal()).getRole();
            model.addAttribute("isAdmin", "ADMIN".equals(roleAtual) || "GESTOR".equals(roleAtual));
            model.addAttribute("isOperador", "OPERADOR".equals(roleAtual));
        } else {
            model.addAttribute("isAdmin", false);
            model.addAttribute("isOperador", false);
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