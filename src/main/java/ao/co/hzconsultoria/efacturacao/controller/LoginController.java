package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.PasswordResetToken;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
import ao.co.hzconsultoria.efacturacao.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.UserRepository userRepository;

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String login, Model model) {
        Optional<User> uOpt = passwordResetService.findUserByLoginOrEmail(login);
        if (!uOpt.isPresent()) {
            // Não revelar se existe ou não
            model.addAttribute("mensagem", "Se o utilizador existir, será enviado um email com instruções.");
            return "forgot-password";
        }
        User user = uOpt.get();
        // Enviar apenas se o user tiver email configurado
        String emailDestino = (user.getEmail() != null && !user.getEmail().trim().isEmpty())
                ? user.getEmail()
                : null;
        if (emailDestino == null) {
            model.addAttribute("mensagem", "Este utilizador não tem email associado. Contacte o administrador.");
            return "forgot-password";
        }
        String token = passwordResetService.createTokenForUser(user);
        String resetUrl = "/reset-password?token=" + token;
        passwordResetService.sendResetEmail(emailDestino, resetUrl);
        model.addAttribute("mensagem", "Se o utilizador existir, será enviado um email com instruções.");
        return "forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam String token, Model model) {
        Optional<PasswordResetToken> t = passwordResetService.validateToken(token);
        if (!t.isPresent()) {
            model.addAttribute("erro", "Token inválido ou expirado.");
            return "reset-password";
        }
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@RequestParam String token, @RequestParam String novaSenha, Model model) {
        Optional<PasswordResetToken> tOpt = passwordResetService.validateToken(token);
        if (!tOpt.isPresent()) {
            model.addAttribute("erro", "Token inválido ou expirado.");
            return "reset-password";
        }
        User user = tOpt.get().getUser();
        user.setSenha(passwordEncoder.encode(novaSenha));
        userRepository.save(user);
        model.addAttribute("mensagem", "Palavra-passe alterada com sucesso. Pode iniciar sessão agora.");
        passwordResetService.removeToken(token);
        return "reset-password";
    }

    @GetMapping("/alterar-senha-obrigatorio")
    public String alterarSenhaObrigatorio(Model model) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails ud = (CustomUserDetails) auth.getPrincipal();
            model.addAttribute("nomeUsuario", ud.getNome());
        }
        return "alterar-senha-obrigatorio";
    }

    @PostMapping("/alterar-senha-obrigatorio")
    public String handleAlterarSenhaObrigatorio(@RequestParam String novaSenha, 
                                                @RequestParam String confirmarSenha, 
                                                Model model) {
        if (novaSenha == null || novaSenha.trim().isEmpty()) {
            model.addAttribute("erro", "A nova senha não pode ser vazia.");
            return "alterar-senha-obrigatorio";
        }
        if (!novaSenha.equals(confirmarSenha)) {
            model.addAttribute("erro", "As senhas não coincidem.");
            return "alterar-senha-obrigatorio";
        }

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails ud = (CustomUserDetails) auth.getPrincipal();
            User user = userRepository.findById(ud.getId()).orElse(null);
            if (user != null) {
                user.setSenha(passwordEncoder.encode(novaSenha));
                user.setForcarAlteracaoSenha(false);
                userRepository.save(user);
                
                ud.getUser().setSenha(user.getSenha());
                ud.getUser().setForcarAlteracaoSenha(false);

                model.addAttribute("mensagem", "Palavra-passe alterada com sucesso! Redirecionando...");
                model.addAttribute("sucesso", true);
                return "alterar-senha-obrigatorio";
            }
        }
        model.addAttribute("erro", "Usuário não autenticado ou inválido.");
        return "alterar-senha-obrigatorio";
    }
}
