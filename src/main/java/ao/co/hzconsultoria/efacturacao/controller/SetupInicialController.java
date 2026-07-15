package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Controlador público para a primeira instalação do sistema.
 * Acessível sem autenticação — cria a primeira empresa e o utilizador administrador.
 */
@Controller
@RequestMapping("/setup-inicial")
public class SetupInicialController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfiguracaoEmpresaRepository configuracaoEmpresaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * Mostra o wizard de primeira instalação.
     * Se já existirem empresas, redireciona para o login.
     */
    @GetMapping
    public String showSetupInicial(Model model) {
        try {
            if (empresaRepository.count() > 0) {
                return "redirect:/login";
            }
        } catch (Exception ignored) {}
        return "setup_inicial";
    }

    /**
     * Endpoint POST público que:
     * 1. Valida os dados do formulário de instalação
     * 2. Cria a primeira empresa
     * 3. Cria o utilizador administrador associado
     * 4. Faz login programático do administrador
     *
     * Após o sucesso, o frontend redireciona para /configuracoes/setup-wizard
     * para completar os passos restantes (já autenticado).
     */
    @PostMapping("/criar-empresa-e-admin")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> criarEmpresaEAdmin(
            @RequestParam("nomeEmpresa") String nomeEmpresa,
            @RequestParam("nif") String nif,
            @RequestParam(value = "regimeFiscal", required = false, defaultValue = "GERAL") String regimeFiscal,
            @RequestParam("nomeAdmin") String nomeAdmin,
            @RequestParam("loginAdmin") String loginAdmin,
            @RequestParam("senhaAdmin") String senhaAdmin,
            @RequestParam(value = "emailAdmin", required = false) String emailAdmin,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Segurança: só permitir se não existirem empresas
            if (empresaRepository.count() > 0) {
                response.put("sucesso", false);
                response.put("mensagem", "O sistema já está configurado. Faça login para continuar.");
                return ResponseEntity.badRequest().body(response);
            }

            // Validação básica
            if (nomeEmpresa == null || nomeEmpresa.trim().isEmpty()) {
                response.put("sucesso", false);
                response.put("mensagem", "O nome da empresa é obrigatório.");
                return ResponseEntity.badRequest().body(response);
            }
            if (nif == null || nif.trim().isEmpty()) {
                response.put("sucesso", false);
                response.put("mensagem", "O NIF é obrigatório.");
                return ResponseEntity.badRequest().body(response);
            }
            if (loginAdmin == null || loginAdmin.trim().isEmpty()) {
                response.put("sucesso", false);
                response.put("mensagem", "O login do administrador é obrigatório.");
                return ResponseEntity.badRequest().body(response);
            }
            if (senhaAdmin == null || senhaAdmin.length() < 6) {
                response.put("sucesso", false);
                response.put("mensagem", "A senha deve ter pelo menos 6 caracteres.");
                return ResponseEntity.badRequest().body(response);
            }
            if (userRepository.findByLogin(loginAdmin.trim()).isPresent()) {
                response.put("sucesso", false);
                response.put("mensagem", "Já existe um utilizador com esse login. Escolha outro.");
                return ResponseEntity.badRequest().body(response);
            }

            // 1. Criar Empresa
            Empresa empresa = new Empresa();
            empresa.setNome(nomeEmpresa.trim());
            empresa.setNif(nif.trim());
            empresa.setRegimeFiscal(regimeFiscal != null ? regimeFiscal.trim() : "GERAL");

            empresa = empresaRepository.save(empresa);

            // 2. Criar ConfiguracaoEmpresa (setup incompleto — será completado no wizard)
            ConfiguracaoEmpresa config = new ConfiguracaoEmpresa();
            config.setEmpresa(empresa);
            config.setSetupCompleto(false);
            configuracaoEmpresaRepository.save(config);

            // 3. Criar utilizador Administrador
            User adminUser = new User();
            adminUser.setLogin(loginAdmin.trim());
            adminUser.setNome((nomeAdmin != null && !nomeAdmin.trim().isEmpty()) ? nomeAdmin.trim() : loginAdmin.trim());
            adminUser.setSenha(passwordEncoder.encode(senhaAdmin));
            adminUser.setRole("ADMIN");
            adminUser.setEmpresa(empresa);
            adminUser.setAtivo(true);
            if (emailAdmin != null && !emailAdmin.trim().isEmpty()) {
                adminUser.setEmail(emailAdmin.trim());
            }
            adminUser.setPermissoes(new HashSet<>());
            adminUser = userRepository.save(adminUser);

            // 4. Login programático — carregar os detalhes e criar token de autenticação
            UserDetails userDetails = userDetailsService.loadUserByUsername(adminUser.getLogin());
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);

            // Persistir na sessão HTTP para que as próximas requisições estejam autenticadas
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            response.put("sucesso", true);
            response.put("mensagem", "Empresa e administrador criados com sucesso! A redirecionar...");
            response.put("redirect", "/configuracoes/setup-wizard");

        } catch (Exception e) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro ao configurar o sistema: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
