package ao.co.hzconsultoria.efacturacao.security;

import ao.co.hzconsultoria.efacturacao.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ForcePasswordChangeInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Ignorar recursos estáticos, endpoints de autenticação, recursos do tema e a própria página de alteração de senha
        if (uri.startsWith("/assets/") || uri.startsWith("/plugins/") || uri.startsWith("/css/") || 
            uri.startsWith("/js/") || uri.startsWith("/images/") || uri.equals("/login") || 
            uri.equals("/logout") || uri.equals("/error") || uri.equals("/alterar-senha-obrigatorio") ||
            uri.startsWith("/webjars/")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return true; // Deixar o Spring Security redirecionar
        }

        if (auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
            User user = userDetails.getUser();
            if (user != null && user.isForcarAlteracaoSenha()) {
                response.sendRedirect("/alterar-senha-obrigatorio");
                return false;
            }
        }

        return true;
    }
}
