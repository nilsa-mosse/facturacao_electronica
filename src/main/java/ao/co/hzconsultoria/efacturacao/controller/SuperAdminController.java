package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/superadmin")
public class SuperAdminController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("empresas", empresaRepository.findAll());
        model.addAttribute("totalEmpresas", empresaRepository.count());
        return "superadmin/dashboard";
    }

    @GetMapping("/empresas/nova")
    public String novaEmpresa(Model model) {
        model.addAttribute("empresa", new Empresa());
        return "superadmin/empresa_form";
    }

    @PostMapping("/empresas/salvar")
    public String salvarEmpresa(@ModelAttribute Empresa empresa, RedirectAttributes redirectAttributes) {
        empresaRepository.save(empresa);
        redirectAttributes.addFlashAttribute("mensagem", "Empresa salva com sucesso!");
        return "redirect:/superadmin/dashboard";
    }

    @GetMapping("/empresas/editar/{id}")
    public String editarEmpresa(@PathVariable Long id, Model model) {
        empresaRepository.findById(id).ifPresent(e -> model.addAttribute("empresa", e));
        return "superadmin/empresa_form";
    }

    @GetMapping("/empresas/eliminar/{id}")
    public String eliminarEmpresa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        empresaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem", "Empresa removida!");
        return "redirect:/superadmin/dashboard";
    }

    // ─── Gestão de Utilizadores (Global) ───────────────────────────────────
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        model.addAttribute("usuarios", userRepository.findAll());
        model.addAttribute("empresas", empresaRepository.findAll());
        model.addAttribute("novoUsuario", new User());
        return "superadmin/usuarios";
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute User user, 
                               @RequestParam(value = "empresaId", required = false) Long empresaId,
                               RedirectAttributes ra) {
        
        // Validação de segurança: apenas ADMIN e SUPERADMIN permitidos aqui
        if (user.getRole() != null && !user.getRole().equals("ADMIN") && !user.getRole().equals("SUPERADMIN")) {
            ra.addFlashAttribute("mensagem_erro", "Operação não permitida: Apenas perfis Administrativos podem ser geridos aqui.");
            return "redirect:/superadmin/usuarios";
        }
        
        User userToSave;
        if (user.getId() != null) {
            userToSave = userRepository.findById(user.getId()).orElse(user);
            userToSave.setNome(user.getNome());
            userToSave.setLogin(user.getLogin());
            userToSave.setRole(user.getRole());
            userToSave.setAtivo(user.isAtivo());
            if (user.getSenha() != null && !user.getSenha().isEmpty() && !user.getSenha().startsWith("$2a$")) {
                userToSave.setSenha(passwordEncoder.encode(user.getSenha()));
            }
        } else {
            userToSave = user;
            if (userToSave.getSenha() == null || userToSave.getSenha().isEmpty()) {
                userToSave.setSenha(passwordEncoder.encode("123456"));
            } else {
                userToSave.setSenha(passwordEncoder.encode(userToSave.getSenha()));
            }
        }

        // Lógica de desassociação: se empresaId for nulo, limpamos a empresa
        if (empresaId != null) {
            userToSave.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
        } else {
            userToSave.setEmpresa(null);
        }

        userRepository.save(userToSave);
        ra.addFlashAttribute("mensagem", "Utilizador atualizado com sucesso!");
        return "redirect:/superadmin/usuarios";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.deleteById(id);
        ra.addFlashAttribute("mensagem", "Utilizador removido!");
        return "redirect:/superadmin/usuarios";
    }

    @GetMapping("/usuarios/desassociar/{id}")
    public String desassociarUsuario(@PathVariable Long id, RedirectAttributes ra) {
        userRepository.findById(id).ifPresent(u -> {
            u.setEmpresa(null);
            userRepository.save(u);
        });
        ra.addFlashAttribute("mensagem", "Utilizador desassociado da empresa com sucesso!");
        return "redirect:/superadmin/usuarios";
    }
}
