package ao.co.hzconsultoria.efacturacao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String home() {
        return "home-operador";
    }
    
    @GetMapping("/")
    public String root(org.springframework.security.core.Authentication auth) {
        if (auth != null) {
            boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));
            
            if (isAdmin) {
                return "redirect:/dashboard";
            } else {
                return "redirect:/home";
            }
        }
        return "redirect:/login";
    }
}
