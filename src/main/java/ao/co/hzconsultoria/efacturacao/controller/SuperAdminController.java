package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.beans.factory.annotation.Value;

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

    @Value("${app.upload.logo.dir:./uploads/logo/}")
    private String logoUploadDir;

    @PostMapping("/empresas/salvar")
    public String salvarEmpresa(@ModelAttribute Empresa empresa, 
                               @RequestParam(value = "logoFile", required = false) org.springframework.web.multipart.MultipartFile logoFile,
                               RedirectAttributes redirectAttributes) {
        
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(logoUploadDir).toAbsolutePath().normalize();
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                
                String originalName = logoFile.getOriginalFilename();
                String safeName = (originalName != null) ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_") : "logo";
                String fileName = "logo_empresa_" + (empresa.getId() != null ? empresa.getId() : "new") + "_" + safeName;
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                java.nio.file.Files.copy(logoFile.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                empresa.setLogotipo("/uploads/logo/" + fileName);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("erro", "Erro ao carregar o logotipo: " + e.getMessage());
            }
        }

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
        java.util.List<User> admins = userRepository.findAll().stream()
            .filter(u -> u.getRole() != null && (u.getRole().contains("ADMIN") || u.getRole().contains("SUPERADMIN")))
            .collect(java.util.stream.Collectors.toList());
        model.addAttribute("usuarios", admins);
        model.addAttribute("empresas", empresaRepository.findAll());
        model.addAttribute("novoUsuario", new User());
        return "superadmin/usuarios";
    }

    @PostMapping("/usuarios/salvar")
    public String salvarUsuario(@ModelAttribute User user, 
                               @RequestParam(value = "empresaId", required = false) Long empresaId,
                               RedirectAttributes ra) {
        
        // Validação de segurança: apenas ADMIN e SUPERADMIN permitidos aqui
        String role = user.getRole();
        boolean isAdminRole = role != null && (role.contains("ADMIN") || role.contains("SUPERADMIN"));
        
        if (!isAdminRole) {
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
        // O SuperAdmin NUNCA deve fazer parte de uma empresa (Acesso Global)
        if ("SUPERADMIN".equals(userToSave.getRole()) || "ROLE_SUPERADMIN".equals(userToSave.getRole())) {
            userToSave.setEmpresa(null);
        } else if (empresaId != null) {
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

    // ─── Configurações Globais do Sistema ─────────────────────────────────
    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    @GetMapping("/configuracoes")
    public String configuracoes(Model model) {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ConfiguracaoSistemaEntity());
        model.addAttribute("config", config);
        return "superadmin/configuracoes";
    }

    @PostMapping("/configuracoes/salvar-expiracao")
    public String salvarExpiracao(@RequestParam("tempo") int tempo,
                                 @RequestParam("unidade") String unidade,
                                 RedirectAttributes ra) {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ConfiguracaoSistemaEntity());
        
        config.setSegTempoExpiracaoSessao(tempo);
        config.setSegTempoExpiracaoUnidade(unidade);
        
        configuracaoSistemaRepository.save(config);
        ra.addFlashAttribute("mensagem", "Tempo de expiração atualizado com sucesso!");
        return "redirect:/superadmin/configuracoes";
    }

    @PostMapping("/configuracoes/salvar-agt")
    public String salvarAgt(@RequestParam("certificado") String certificado,
                           @RequestParam(value = "privateKey", required = false) String privateKey,
                           @RequestParam(value = "publicKey", required = false) String publicKey,
                           @RequestParam(value = "versao", required = false) Integer versao,
                           RedirectAttributes ra) {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ConfiguracaoSistemaEntity());
        
        config.setAgtCertificadoNumero(certificado);
        if (privateKey != null) config.setAgtPrivateKey(privateKey);
        if (publicKey != null) config.setAgtPublicKey(publicKey);
        if (versao != null) config.setAgtChaveVersao(versao);
        
        configuracaoSistemaRepository.save(config);
        ra.addFlashAttribute("mensagem", "Configurações AGT atualizadas com sucesso!");
        return "redirect:/superadmin/configuracoes";
    }

    @PostMapping("/configuracoes/salvar-sistema")
    public String salvarSistema(@RequestParam("nome") String nome,
                               @RequestParam("versao") String versao,
                               @RequestParam("email") String email,
                               @RequestParam("tema") String tema,
                               @RequestParam(value = "exibirDatasValidade", required = false, defaultValue = "false") boolean exibirDatasValidade,
                               @RequestParam(value = "logoFile", required = false) org.springframework.web.multipart.MultipartFile logoFile,
                               RedirectAttributes ra) {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ConfiguracaoSistemaEntity());
        
        config.setSistemaNome(nome);
        config.setSistemaVersao(versao);
        config.setSistemaEmailSuporte(email);
        config.setSistemaTema(tema);
        config.setExibirDatasValidade(exibirDatasValidade);

        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(logoUploadDir).toAbsolutePath().normalize();
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                
                String originalName = logoFile.getOriginalFilename();
                String safeName = (originalName != null) ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_") : "system_logo";
                String fileName = "sistema_logo_" + System.currentTimeMillis() + "_" + safeName;
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                java.nio.file.Files.copy(logoFile.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                
                config.setSistemaLogotipo("/uploads/logo/" + fileName);
            } catch (java.io.IOException e) {
                e.printStackTrace();
                ra.addFlashAttribute("erro", "Erro ao carregar o logotipo do sistema: " + e.getMessage());
            }
        }
        
        configuracaoSistemaRepository.save(config);
        ra.addFlashAttribute("mensagem", "Parâmetros do sistema atualizados!");
        return "redirect:/superadmin/configuracoes";
    }

    @PostMapping("/configuracoes/salvar-banco-dados")
    public String salvarBancoDados(@RequestParam("tipoBD") String tipoBD,
                                  @RequestParam("schema") String schema,
                                  @RequestParam("poolMax") int poolMax,
                                  RedirectAttributes ra) {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ConfiguracaoSistemaEntity());
        
        config.setDbTipoBD(tipoBD);
        config.setDbSchema(schema);
        config.setDbPoolMax(poolMax);
        
        configuracaoSistemaRepository.save(config);
        ra.addFlashAttribute("mensagem", "Configurações de Base de Dados atualizadas!");
        return "redirect:/superadmin/configuracoes";
    }

    @PostMapping("/configuracoes/salvar-email")
    public String salvarEmail(@RequestParam("host") String host,
                             @RequestParam("porta") int porta,
                             @RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("remetente") String remetente,
                             RedirectAttributes ra) {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ConfiguracaoSistemaEntity());
        
        config.setEmailSmtpHost(host);
        config.setEmailSmtpPorta(porta);
        config.setEmailSmtpUsername(username);
        config.setEmailSmtpPassword(password);
        config.setEmailRemetente(remetente);
        
        configuracaoSistemaRepository.save(config);
        ra.addFlashAttribute("mensagem", "Configurações de Email atualizadas!");
        return "redirect:/superadmin/configuracoes";
    }

    @PostMapping("/configuracoes/salvar-storage")
    public String salvarStorage(@RequestParam("tipo") String tipo,
                               @RequestParam("caminho") String caminho,
                               RedirectAttributes ra) {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ConfiguracaoSistemaEntity());
        
        config.setStorageTipo(tipo);
        config.setStorageCaminhoBase(caminho);
        
        configuracaoSistemaRepository.save(config);
        ra.addFlashAttribute("mensagem", "Configurações de Armazenamento atualizadas!");
        return "redirect:/superadmin/configuracoes";
    }
}
