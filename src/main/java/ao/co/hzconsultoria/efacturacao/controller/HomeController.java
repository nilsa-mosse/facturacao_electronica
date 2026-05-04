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

    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.CaixaService caixaService;

    @GetMapping("/home")
    public String home(Authentication auth, Model model) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();
            if ("GESTOR".equals(user.getRole())) {
                return "redirect:/dashboard";
            }
            model.addAttribute("vendas", compraRepository.findByUsuario_IdOrderByDataCompraDesc(user.getId()));
            
            // Informações do Caixa
            model.addAttribute("caixaAberto", caixaService.getCaixaAbertoAtual());
            model.addAttribute("isCaixaAberto", caixaService.isCaixaAberto());
        }
        return "home-operador";
    }
    
    @GetMapping("/")
    public String root(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && 
            !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            
            boolean isManager = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || 
                               a.getAuthority().equals("ROLE_SUPERADMIN") || 
                               a.getAuthority().equals("ROLE_GESTOR"));
            
            if (isManager) {
                return "redirect:/dashboard";
            } else {
                return "redirect:/home";
            }
        }
        return "redirect:/login";
    }
}
