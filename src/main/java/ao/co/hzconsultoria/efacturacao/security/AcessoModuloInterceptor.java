package ao.co.hzconsultoria.efacturacao.security;

import ao.co.hzconsultoria.efacturacao.model.PermissaoModulo;
import ao.co.hzconsultoria.efacturacao.repository.PermissaoModuloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

@Component
public class AcessoModuloInterceptor implements HandlerInterceptor {

    @Autowired
    private PermissaoModuloRepository permissaoRepo;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        
        // Ignorar recursos estáticos e login/logout
        if (uri.startsWith("/assets/") || uri.startsWith("/plugins/") || uri.startsWith("/css/") || 
            uri.startsWith("/js/") || uri.startsWith("/images/") || uri.equals("/login") || uri.equals("/logout") || uri.equals("/error")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return true; // Deixar o Spring Security lidar com o redirecionamento para o login
        }

        // SuperAdmin tem acesso a tudo
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
        if (isSuperAdmin) {
            return true;
        }

        String modulo = identificarModuloPorUri(uri);
        if (modulo == null) {
            return true; // Se não for um módulo mapeado, permitir acesso (ex: perfil do usuário)
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Bloqueio estrito para Admin: Não entra no Painel Global
        if (isAdmin && "PAINEL_GLOBAL".equals(modulo)) {
            response.sendRedirect("/dashboard?error=access_denied&module=PAINEL_GLOBAL");
            return false;
        }

        // O acesso aos módulos será validado mais abaixo através da Tabela de Permissões (RBAC)

        Long userId = null;
        if (auth.getPrincipal() instanceof CustomUserDetails) {
            userId = ((CustomUserDetails) auth.getPrincipal()).getId();
        }

        if (userId != null) {
            // Validar acesso através da Tabela de Permissões para módulos permitidos ao Role
            Optional<PermissaoModulo> perm = permissaoRepo.findByModuloAndUsuario_Id(modulo, userId);
            if (perm.isPresent() && !perm.get().isAtivo()) {
                String redirectPath = isAdmin ? "/dashboard" : "/home";
                response.sendRedirect(redirectPath + "?error=access_denied&module=" + modulo);
                return false;
            }
        }

        return true;
    }

    private String identificarModuloPorUri(String uri) {
        if (uri.startsWith("/dashboard")) return "DASHBOARD";
        if (uri.startsWith("/pos") || uri.startsWith("/historico-vendas") || uri.startsWith("/devolucoes") || uri.startsWith("/finalizarVenda") || uri.startsWith("/home")) return "VENDAS";
        if (uri.startsWith("/produtos") || uri.startsWith("/cadastroProduto") || uri.startsWith("/inventario") || uri.startsWith("/stock") || uri.startsWith("/api/categorias") || uri.startsWith("/categorias")) return "STOCK";
        if (uri.startsWith("/clientes") || uri.startsWith("/fornecedores")) return "ENTIDADES";
        if (uri.startsWith("/guias") || uri.startsWith("/notas") || uri.startsWith("/factura-eletronica") || uri.startsWith("/configuracoes/saft")) return "FACTURACAO";
        if (uri.startsWith("/despesas") || uri.startsWith("/financeiro")) return "FINANCEIRO";
        if (uri.startsWith("/superadmin")) return "PAINEL_GLOBAL";
        
        // Controlo de Acesso é permitido a qualquer utilizador administrativo
        if (uri.startsWith("/configuracoes/controlo-acesso")) return null;
        
        // Restantes configurações (Empresa, Utilizadores, AGT, etc.) requerem módulo ADMINISTRACAO
        if (uri.startsWith("/configuracoes")) return "ADMINISTRACAO";
        
        return null;
    }
}
