package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/user/configuracoes")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public String getUserSettings(Model model) {
        // Assuming a logged-in user with login "admin" for demonstration purposes
        Optional<User> usuarioOpt = userRepository.findByLogin("admin");
        User usuario = usuarioOpt.orElseGet(() -> {
            User u = new User();
            u.setNome("Default User");
            u.setLogin("admin");
            u.setRole("USER");
            return u;
        });

        model.addAttribute("usuario", usuario);

        return "dashboardConfiguracoes";
    }
}