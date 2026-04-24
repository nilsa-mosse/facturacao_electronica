package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import ao.co.hzconsultoria.efacturacao.service.ConfiguracaoSistemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/configuracoes")
public class ConfiguracaoController {

    @Autowired
    private EmpresaRepository empresaRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ImpostoRepository impostoRepository;
    @Autowired
    private SerieRepository serieRepository;
    @Autowired
    private MoedaRepository moedaRepository;
    @Autowired
    private MetodoPagamentoRepository metodoPagamentoRepository;
    @Autowired
    private TaxaRepository taxaRepository;
    @Autowired
    private RetencaoRepository retencaoRepository;
    @Autowired
    private ConfiguracaoAGTRepository configuracaoAGTRepository;
    @Autowired
    private RegimeFiscalRepository regimeFiscalRepository;
    @Autowired
    private PermissaoModuloRepository permissaoModuloRepository;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.AgtService agtService;
    @Autowired
    private ConfiguracaoSistemaService cfgService;

    // ─── Dados da Empresa ────────────────────────────────────────────────────
    @GetMapping("/empresa")
    public String empresa(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(new Empresa()) : new Empresa();
        model.addAttribute("empresa", empresa);
        model.addAttribute("regimes", regimeFiscalRepository.findAll());
        return "configuracoes/empresa";
    }

    @PostMapping("/empresa/salvar")
    public String salvarEmpresa(@ModelAttribute Empresa empresa, 
                               @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                               RedirectAttributes redirectAttributes) {
        
        Long currentEmpresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        
        // Segurança: verificar se o usuário está tentando salvar sua própria empresa
        if (empresa.getId() != null && !empresa.getId().equals(currentEmpresaId)) {
            return "redirect:/dashboard";
        }

        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String uploadDir = "src/main/resources/static/uploads/logo/";
                Path uploadPath = Paths.get(uploadDir);
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                String fileName = "logo_empresa_" + currentEmpresaId + "_" + logoFile.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(logoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                empresa.setLogotipo("/uploads/logo/" + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        empresaRepository.save(empresa);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.salvo", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/empresa";
    }

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ─── Utilizadores e Perfis ───────────────────────────────────────────────
    @GetMapping("/utilizadores")
    public String utilizadores(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));

        if (isSuperAdmin) {
            model.addAttribute("utilizadores", userRepository.findAll());
            model.addAttribute("estabelecimentos", estabelecimentoRepository.findAll());
        } else {
            model.addAttribute("utilizadores", userRepository.findByEmpresa_Id(empresaId));
            model.addAttribute("estabelecimentos", estabelecimentoRepository.findByEmpresa_Id(empresaId));
        }
        
        model.addAttribute("novoUsuario", new User());
        model.addAttribute("isSuperAdmin", isSuperAdmin);
        return "configuracoes/utilizadores";
    }

    @PostMapping("/utilizadores/salvar")
    public String salvarUsuario(@ModelAttribute User user, 
                               @RequestParam(value = "estabelecimentoIds", required = false) List<Long> estabelecimentoIds,
                               RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        user.setEmpresa(empresa);
        
        // Proteção: Admin não pode criar SuperAdmin
        if ("SUPERADMIN".equals(user.getRole())) {
             user.setRole("OPERADOR"); 
        }

        // Criptografar senha se for novo usuário ou senha alterada
        if (user.getId() == null || (user.getSenha() != null && !user.getSenha().startsWith("$2a$"))) {
            if (user.getSenha() == null || user.getSenha().isEmpty()) {
                if (user.getId() == null) user.setSenha(passwordEncoder.encode("123456")); 
            } else {
                user.setSenha(passwordEncoder.encode(user.getSenha()));
            }
        }

        // Associar estabelecimentos
        if (estabelecimentoIds != null && !estabelecimentoIds.isEmpty()) {
            java.util.Set<Estabelecimento> ests = new java.util.HashSet<>(estabelecimentoRepository.findAllById(estabelecimentoIds));
            user.setEstabelecimentos(ests);
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/utilizadores";
    }

    @GetMapping("/utilizadores/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        User user = userRepository.findById(id).orElse(null);
        if (user != null && user.getEmpresa() != null && user.getEmpresa().getId().equals(empresaId)) {
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagem",
                    messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        }
        return "redirect:/configuracoes/utilizadores";
    }

    // ─── Impostos e Taxas ────────────────────────────────────────────────────
    @GetMapping("/impostos")
    public String impostos(Model model) {
        model.addAttribute("impostos", impostoRepository.findAll());
        model.addAttribute("novoImposto", new Imposto());
        return "configuracoes/impostos";
    }

    @PostMapping("/impostos/salvar")
    public String salvarImposto(@ModelAttribute Imposto imposto, RedirectAttributes redirectAttributes) {
        impostoRepository.save(imposto);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/impostos";
    }

    @GetMapping("/impostos/eliminar/{id}")
    public String eliminarImposto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        impostoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/impostos";
    }

    // ─── Séries de Facturação ────────────────────────────────────────────────
    @GetMapping("/series")
    public String series(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("series", serieRepository.findByEmpresa_Id(empresaId));
        model.addAttribute("novaSerie", new Serie());
        return "configuracoes/series";
    }

    @PostMapping("/series/salvar")
    public String salvarSerie(@ModelAttribute Serie serie, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        serie.setEmpresa(empresa);
        serieRepository.save(serie);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/series";
    }

    @GetMapping("/series/eliminar/{id}")
    public String eliminarSerie(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Serie serie = serieRepository.findById(id).orElse(null);
        if (serie != null && serie.getEmpresa() != null && serie.getEmpresa().getId().equals(empresaId)) {
            serieRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagem",
                    messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        }
        return "redirect:/configuracoes/series";
    }

    // ─── Moedas e Câmbios ────────────────────────────────────────────────────
    @GetMapping("/moedas")
    public String moedas(Model model) {
        model.addAttribute("moedas", moedaRepository.findAll());
        model.addAttribute("novaMoeda", new Moeda());
        return "configuracoes/moedas";
    }

    @PostMapping("/moedas/salvar")
    public String salvarMoeda(@ModelAttribute Moeda moeda, RedirectAttributes redirectAttributes) {
        moedaRepository.save(moeda);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/moedas";
    }

    @GetMapping("/moedas/eliminar/{id}")
    public String eliminarMoeda(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        moedaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/moedas";
    }

    // ─── Métodos de Pagamento ────────────────────────────────────────────────
    @GetMapping("/pagamentos")
    public String pagamentos(Model model) {
        model.addAttribute("metodos", metodoPagamentoRepository.findAll());
        model.addAttribute("novoMetodo", new MetodoPagamento());
        return "configuracoes/pagamentos";
    }

    @PostMapping("/pagamentos/salvar")
    public String salvarPagamento(@ModelAttribute MetodoPagamento metodo, RedirectAttributes redirectAttributes) {
        metodoPagamentoRepository.save(metodo);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/pagamentos";
    }

    @GetMapping("/pagamentos/eliminar/{id}")
    public String eliminarPagamento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        metodoPagamentoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/pagamentos";
    }

    // ─── Comunicação AGT ─────────────────────────────────────────────────────
    @GetMapping("/comunicacao-agt")
    public String comunicacaoAgt(Model model) {
        List<ConfiguracaoAGT> cfgs = configuracaoAGTRepository.findAll();
        model.addAttribute("config", cfgs.isEmpty() ? new ConfiguracaoAGT() : cfgs.get(0));
        return "configuracoes/comunicacao-agt";
    }

    @PostMapping("/comunicacao-agt/salvar")
    public String salvarComunicacaoAgt(@ModelAttribute ConfiguracaoAGT config, RedirectAttributes redirectAttributes) {
        configuracaoAGTRepository.save(config);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/comunicacao-agt";
    }

    @PostMapping("/comunicacao-agt/testar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testarConexaoAgt(@RequestBody Map<String, String> payload) {
        String urlApi = payload.get("urlApi");
        String token = payload.get("token");

        if (urlApi == null || urlApi.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("sucesso", false);
            err.put("mensagem",
                    messageSource.getMessage("config.agt.teste.falha_url", null, LocaleContextHolder.getLocale()));
            return ResponseEntity.badRequest().body(err);
        }

        Map<String, Object> resultado = agtService.pingAgt(urlApi, token);
        return ResponseEntity.ok(resultado);
    }

    // ─── Servidor e Rede ─────────────────────────────────────────────────
    @GetMapping("/servidor")
    public String servidor(Model model) {
        model.addAttribute("cfg", cfgService.getServidor());
        return "configuracoes/servidor";
    }

    @PostMapping("/servidor/salvar")
    public String salvarServidor(@ModelAttribute ConfiguracaoServidor cfg, RedirectAttributes ra) {
        cfgService.saveServidor(cfg);
        ra.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/servidor";
    }

    // ─── Email / SMTP ─────────────────────────────────────────────────────
    @GetMapping("/email")
    public String email(Model model) {
        model.addAttribute("cfg", cfgService.getEmail());
        return "configuracoes/email";
    }

    @PostMapping("/email/salvar")
    public String salvarEmail(@ModelAttribute ConfiguracaoEmail cfg, RedirectAttributes ra) {
        cfgService.saveEmail(cfg);
        ra.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/email";
    }

    @PostMapping("/email/testar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testarEmail(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        String dest = payload.get("emailDestino");
        if (dest == null || dest.isBlank()) {
            result.put("sucesso", false);
            result.put("mensagem", "Endereço de email de destino não informado.");
            return ResponseEntity.badRequest().body(result);
        }
        result.put("sucesso", true);
        result.put("mensagem", "Email de teste enviado para " + dest + " (simulação).");
        return ResponseEntity.ok(result);
    }

    // ─── Base de Dados ────────────────────────────────────────────────────
    @GetMapping("/banco-dados")
    public String bancoDados(Model model) {
        model.addAttribute("cfg", cfgService.getDatabase());
        return "configuracoes/banco-dados";
    }

    @PostMapping("/banco-dados/salvar")
    public String salvarBancoDados(@ModelAttribute ConfiguracaoDatabase cfg, RedirectAttributes ra) {
        cfgService.saveDatabase(cfg);
        ra.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/banco-dados";
    }

    // ─── Storage ──────────────────────────────────────────────────────────
    @GetMapping("/storage")
    public String storage(Model model) {
        model.addAttribute("cfg", cfgService.getStorage());
        return "configuracoes/storage";
    }

    @PostMapping("/storage/salvar")
    public String salvarStorage(@ModelAttribute ConfiguracaoStorage cfg, RedirectAttributes ra) {
        cfgService.saveStorage(cfg);
        ra.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/storage";
    }

    // ─── Segurança ────────────────────────────────────────────────────────
    @GetMapping("/seguranca")
    public String seguranca(Model model) {
        model.addAttribute("cfg", cfgService.getSeguranca());
        return "configuracoes/seguranca";
    }

    @PostMapping("/seguranca/salvar")
    public String salvarSeguranca(@ModelAttribute ConfiguracaoSeguranca cfg, RedirectAttributes ra) {
        cfgService.saveSeguranca(cfg);
        ra.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/seguranca";
    }

    // ─── Parâmetros Gerais ───────────────────────────────────────────────────
    @GetMapping("/geral")
    public String geral(Model model) {
        model.addAttribute("sistema", cfgService.getSistema());
        return "configuracoes/geral";
    }

    @PostMapping("/geral/salvar")
    public String salvarSistema(@ModelAttribute Sistema sistema, RedirectAttributes redirectAttributes) {
        cfgService.saveSistema(sistema);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/geral";
    }

    // ─── Configurações Fiscais ───────────────────────────────────────────────
    @GetMapping("/fiscais/impostos")
    public String fiscaisImpostos(Model model) {
        model.addAttribute("impostos", impostoRepository.findAll());
        model.addAttribute("novoImposto", new Imposto());
        return "configuracoes/fiscais/impostos";
    }
 
    @PostMapping("/fiscais/impostos/salvar")
    public String salvarImpostoFiscal(@ModelAttribute Imposto imposto, RedirectAttributes redirectAttributes) {
        impostoRepository.save(imposto);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/impostos";
    }
 
    @GetMapping("/fiscais/impostos/eliminar/{id}")
    public String eliminarImpostoFiscal(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        impostoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/impostos";
    }

    @GetMapping("/fiscais/taxas")
    public String fiscaisTaxas(Model model) {
        model.addAttribute("taxas", taxaRepository.findAll());
        model.addAttribute("novaTaxa", new Taxa());
        return "configuracoes/fiscais/taxas";
    }

    @PostMapping("/fiscais/taxas/salvar")
    public String salvarTaxa(@ModelAttribute Taxa taxa, RedirectAttributes redirectAttributes) {
        taxaRepository.save(taxa);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/taxas";
    }

    @GetMapping("/fiscais/taxas/eliminar/{id}")
    public String eliminarTaxa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        taxaRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/taxas";
    }

    @GetMapping("/fiscais/regime-fiscal")
    public String fiscaisRegime(Model model) {
        // Semente inicial se a tabela estiver vazia
        if (regimeFiscalRepository.count() == 0) {
            regimeFiscalRepository.save(new RegimeFiscal("Regime Geral", "GERAL",
                    "Para empresas com facturação superior a 250 Milhões de Kz.", "fas fa-balance-scale"));
            regimeFiscalRepository.save(new RegimeFiscal("Regime Simplificado", "SIMPLIFICADO",
                    "Para empresas com facturação entre 7.5 e 250 Milhões de Kz.", "fas fa-shield-alt"));
            regimeFiscalRepository.save(new RegimeFiscal("Regime de Exclusão", "EXCLUSAO",
                    "Para empresas com facturação inferior a 7.5 Milhões de Kz.", "fas fa-ban"));
        }

        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(new Empresa()) : new Empresa();
        model.addAttribute("empresa", empresa);
        model.addAttribute("regimes", regimeFiscalRepository.findAll());
        model.addAttribute("novoRegime", new RegimeFiscal());
        return "configuracoes/fiscais/regime_fiscal";
    }

    @PostMapping("/fiscais/regime-fiscal/salvar")
    public String salvarRegimeFiscal(@RequestParam String regime, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            Empresa e = empresaRepository.findById(empresaId).orElse(null);
            if (e != null) {
                e.setRegimeFiscal(regime);
                empresaRepository.save(e);
                redirectAttributes.addFlashAttribute("mensagem", "Regime Fiscal actualizado com sucesso!");
            } else {
                redirectAttributes.addFlashAttribute("erro", "Empresa não encontrada.");
            }
        } else {
            redirectAttributes.addFlashAttribute("erro", "Não foi possível identificar a empresa do utilizador.");
        }
        return "redirect:/configuracoes/fiscais/regime-fiscal";
    }

    @PostMapping("/fiscais/regime-fiscal/adicionar")
    public String salvarNovoRegime(@ModelAttribute RegimeFiscal regimeFiscal, RedirectAttributes redirectAttributes) {
        regimeFiscalRepository.save(regimeFiscal);
        redirectAttributes.addFlashAttribute("mensagem", "Novo regime fiscal adicionado com sucesso!");
        return "redirect:/configuracoes/fiscais/regime-fiscal";
    }

    @GetMapping("/fiscais/retencoes")
    public String fiscaisRetencoes(Model model) {
        model.addAttribute("retencoes", retencaoRepository.findAll());
        model.addAttribute("novaRetencao", new Retencao());
        return "configuracoes/fiscais/retencoes";
    }

    @PostMapping("/fiscais/retencoes/salvar")
    public String salvarRetencao(@ModelAttribute Retencao retencao, RedirectAttributes redirectAttributes) {
        retencaoRepository.save(retencao);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/retencoes";
    }

    @GetMapping("/fiscais/retencoes/eliminar/{id}")
    public String eliminarRetencao(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        retencaoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/retencoes";
    }
    // ─── Estabelecimentos ────────────────────────────────────────────────────
    @GetMapping("/estabelecimentos")
    public String estabelecimentos(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));

        if (isSuperAdmin) {
            model.addAttribute("estabelecimentos", estabelecimentoRepository.findAll());
            model.addAttribute("empresas", empresaRepository.findAll());
        } else {
            model.addAttribute("estabelecimentos", estabelecimentoRepository.findByEmpresa_Id(empresaId));
        }
        
        model.addAttribute("novoEstabelecimento", new Estabelecimento());
        model.addAttribute("isSuperAdmin", isSuperAdmin);
        return "configuracoes/estabelecimentos";
    }

    @PostMapping("/estabelecimentos/salvar")
    public String salvarEstabelecimento(@ModelAttribute Estabelecimento estabelecimento, 
                                       @RequestParam(value = "empresaSelecionadaId", required = false) Long empresaSelecionadaId,
                                       RedirectAttributes redirectAttributes) {
        
        Long currentEmpresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));

        Empresa empresa;
        if (isSuperAdmin && empresaSelecionadaId != null) {
            empresa = empresaRepository.findById(empresaSelecionadaId).orElse(null);
        } else {
            empresa = empresaRepository.findById(currentEmpresaId).orElse(null);
        }

        estabelecimento.setEmpresa(empresa);
        estabelecimentoRepository.save(estabelecimento);
        
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/estabelecimentos";
    }

    @GetMapping("/estabelecimentos/eliminar/{id}")
    public String eliminarEstabelecimento(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long currentEmpresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));

        Estabelecimento est = estabelecimentoRepository.findById(id).orElse(null);
        if (est != null) {
            if (isSuperAdmin || (est.getEmpresa() != null && est.getEmpresa().getId().equals(currentEmpresaId))) {
                estabelecimentoRepository.deleteById(id);
                redirectAttributes.addFlashAttribute("mensagem",
                        messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
            }
        }
        return "redirect:/configuracoes/estabelecimentos";
    }

    // ─── Controlo de Acesso ──────────────────────────────────────────────────
    @GetMapping("/controlo-acesso")
    public String controloAcesso(@RequestParam(value = "usuarioId", required = false) Long usuarioId, Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));

        List<User> usuarios;
        if (isSuperAdmin) {
            usuarios = userRepository.findAll();
        } else {
            usuarios = userRepository.findByEmpresa_Id(empresaId);
        }
        model.addAttribute("usuarios", usuarios);

        List<String> modulos = java.util.Arrays.asList("DASHBOARD", "VENDAS", "STOCK", "ENTIDADES", "FACTURACAO", "FINANCEIRO", "ADMINISTRACAO");
        model.addAttribute("modulosList", modulos);

        if (usuarioId != null) {
            User user = userRepository.findById(usuarioId).orElse(null);
            if (user != null) {
                // Garantir que todas as permissões base existam e estejam corretas para este usuário
                for (String modulo : modulos) {
                    Optional<PermissaoModulo> opt = permissaoModuloRepository.findByModuloAndUsuario_Id(modulo, usuarioId);
                    boolean isAdmin = "ADMIN".equals(user.getRole());
                    boolean deveEstarAtivo = (isAdmin && !"ADMINISTRACAO".equals(modulo)) || "VENDAS".equals(modulo);
                    
                    if (!opt.isPresent()) {
                        // Se não existe, cria com o default
                        permissaoModuloRepository.save(new PermissaoModulo(modulo, user, deveEstarAtivo));
                    } else if (isAdmin && deveEstarAtivo && !opt.get().isAtivo()) {
                        // Se é ADMIN e o módulo básico está desativado, força a ativação "por default"
                        PermissaoModulo p = opt.get();
                        p.setAtivo(true);
                        permissaoModuloRepository.save(p);
                    }
                }
                
                model.addAttribute("usuarioSelecionado", user);
                Map<String, Boolean> permsMap = new HashMap<>();
                permissaoModuloRepository.findByUsuario_Id(usuarioId).forEach(p -> {
                    permsMap.put(p.getModulo(), p.isAtivo());
                });
                model.addAttribute("permsMap", permsMap);
            }
        }
        
        return "configuracoes/controlo-acesso";
    }

    @PostMapping("/controlo-acesso/salvar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarControloAcesso(@RequestBody Map<String, Object> payload) {
        Long usuarioId = Long.valueOf(payload.get("usuarioId").toString());
        List<Map<String, Object>> permissoes = (List<Map<String, Object>>) payload.get("permissoes");
        
        User user = userRepository.findById(usuarioId).orElse(null);
        if (user != null) {
            for (Map<String, Object> p : permissoes) {
                String modulo = (String) p.get("modulo");
                boolean ativo = (boolean) p.get("ativo");
                
                permissaoModuloRepository.findByModuloAndUsuario_Id(modulo, usuarioId).ifPresent(perm -> {
                    perm.setAtivo(ativo);
                    permissaoModuloRepository.save(perm);
                });
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("sucesso", true);
        return ResponseEntity.ok(res);
    }
}
