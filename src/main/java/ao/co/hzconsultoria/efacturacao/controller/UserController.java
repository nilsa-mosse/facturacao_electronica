package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/user/configuracoes")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String getUserSettings(Model model) {
        // Assuming a logged-in user with login "admin" for demonstration purposes
        User usuario = userRepository.findByLogin("admin");
        if (usuario == null) {
            usuario = new User();
            usuario.setNome("Default User");
            usuario.setLogin("admin");
            usuario.setRole("USER");
        }
        model.addAttribute("usuario", usuario);

        return "dashboardConfiguracoes";
    }
}