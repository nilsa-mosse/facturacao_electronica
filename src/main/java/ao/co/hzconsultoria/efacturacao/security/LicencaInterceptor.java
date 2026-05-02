package ao.co.hzconsultoria.efacturacao.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import ao.co.hzconsultoria.efacturacao.service.LicencaService;

@Component
public class LicencaInterceptor implements HandlerInterceptor {

    @Autowired
    private LicencaService licencaService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // Ignorar recursos estáticos, erros, login e ficheiros PWA
        if (uri.startsWith("/assets/") || uri.startsWith("/plugins/") || uri.startsWith("/css/") || 
            uri.startsWith("/js/") || uri.startsWith("/images/") || uri.equals("/error") || 
            uri.equals("/login") || uri.equals("/licenca-expirada") ||
            uri.equals("/manifest.json") || uri.equals("/sw.js")) {
            return true;
        }

        if (!licencaService.isLicencaValida()) {
            response.sendRedirect("/licenca-expirada");
            return false;
        }

        return true;
    }
}
