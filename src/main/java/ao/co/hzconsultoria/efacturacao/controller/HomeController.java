package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private CompraRepository compraRepository;

    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
            model.addAttribute("vendas", compraRepository.findByUsuario_IdOrderByDataCompraDesc(user.getId()));
        }
        return "home-operador";
    }
    
    @GetMapping("/")
    public String root(Authentication auth) {
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
