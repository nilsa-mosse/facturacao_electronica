package ao.co.hzconsultoria.efacturacao.security;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private ConfiguracaoEmpresaRepository configuracaoEmpresaRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // 1. Verificar se a alteração de senha é obrigatória
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails ud = (CustomUserDetails) authentication.getPrincipal();
            if (ud.getUser() != null && ud.getUser().isForcarAlteracaoSenha()) {
                response.sendRedirect("/alterar-senha-obrigatorio");
                return;
            }
        }

        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        boolean isGestor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR"));

        // 2. SuperAdmin não tem empresa associada — vai directo ao dashboard
        if (isSuperAdmin) {
            response.sendRedirect("/dashboard");
            return;
        }

        // 3. Para Admin e Gestor: verificar se o Setup Wizard foi concluído
        if (isAdmin || isGestor) {
            if (authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails ud = (CustomUserDetails) authentication.getPrincipal();
                Long empresaId = ud.getEmpresaId();
                if (empresaId != null) {
                    try {
                        Optional<ConfiguracaoEmpresa> optConfig =
                                configuracaoEmpresaRepository.findByEmpresa_Id(empresaId);
                        boolean setupCompleto = optConfig.map(ConfiguracaoEmpresa::isSetupCompleto).orElse(false);
                        if (!setupCompleto) {
                            response.sendRedirect("/configuracoes/setup-wizard");
                            return;
                        }
                    } catch (Exception ignored) {
                        // Em caso de erro de BD, deixar prosseguir para o dashboard
                    }
                }
            }
            response.sendRedirect("/dashboard");
            return;
        }

        // 4. Operadores e outros utilizadores
        response.sendRedirect("/home");
    }
}
