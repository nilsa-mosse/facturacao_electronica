package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import ao.co.hzconsultoria.efacturacao.service.ConfiguracaoSistemaService;
import ao.co.hzconsultoria.efacturacao.service.ConfiguracaoEmpresaService;
import ao.co.hzconsultoria.efacturacao.service.DynamicMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
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
    private EstabelecimentoRepository estabelecimentoRepository;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.AgtService agtService;
    @Autowired
    private ConfiguracaoSistemaService cfgService;
    @Autowired
    private ConfiguracaoEmpresaService configuracaoEmpresaService;
    @Autowired
    private DynamicMailService dynamicMailService;
    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.UserRegistrationService userRegistrationService;

    @Value("${app.upload.logo.dir:./uploads/logo/}")
    private String logoUploadDir;

    @GetMapping("/empresa")
    public String empresa(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(new Empresa())
                : new Empresa();
        model.addAttribute("empresa", empresa);
        model.addAttribute("regimes", regimeFiscalRepository.findAll());
        if (empresaId != null) {
            model.addAttribute("estabelecimentos", estabelecimentoRepository.findByEmpresa_Id(empresaId));
            model.addAttribute("configEmpresa", configuracaoEmpresaService.obterConfiguracao(empresaId));
        } else {
            model.addAttribute("estabelecimentos", java.util.Collections.emptyList());
            model.addAttribute("configEmpresa", new ConfiguracaoEmpresa());
        }
        return "configuracoes/empresa";
    }

    @PostMapping("/empresa/salvar")
    public String salvarEmpresa(@ModelAttribute Empresa empresa,
            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
            RedirectAttributes redirectAttributes) {
        Long currentEmpresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresa.getId() != null && !empresa.getId().equals(currentEmpresaId)) {
            return "redirect:/dashboard";
        }
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                Path uploadPath = Paths.get(logoUploadDir).toAbsolutePath().normalize();
                if (!Files.exists(uploadPath))
                    Files.createDirectories(uploadPath);
                String safeName = (logoFile.getOriginalFilename() != null)
                        ? logoFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                        : "logo";
                String fileName = "logo_empresa_" + currentEmpresaId + "_" + safeName;
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(logoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                empresa.setLogotipo("/uploads/logo/" + fileName);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("erro", "Erro ao carregar logotipo: " + e.getMessage());
            }
        }
        empresaRepository.save(empresa);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.salvo", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/empresa";
    }

    @GetMapping("/utilizadores")
    public String utilizadores(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
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
        if ("SUPERADMIN".equals(user.getRole()))
            user.setRole("OPERADOR");
        boolean isNew = (user.getId() == null);
        if (!isNew) {
            User existing = userRepository.findById(user.getId()).orElse(null);
            if (existing != null) {
                existing.setNome(user.getNome());
                existing.setEmail(user.getEmail());
                existing.setLogin(user.getLogin());
                existing.setRole(user.getRole());
                existing.setAtivo(user.isAtivo());
                existing.setEmpresa(empresa);
                if (user.getSenha() != null && !user.getSenha().isEmpty() && !user.getSenha().startsWith("$2a$"))
                    existing.setSenha(passwordEncoder.encode(user.getSenha()));
                if (estabelecimentoIds != null && !estabelecimentoIds.isEmpty())
                    existing.setEstabelecimentos(
                            new java.util.HashSet<>(estabelecimentoRepository.findAllById(estabelecimentoIds)));
                userRepository.save(existing);
            }
        } else {
            if (user.getLogin() != null && !user.getLogin().isEmpty()) {
                if (userRepository.findByLogin(user.getLogin()).isPresent()) {
                    redirectAttributes.addFlashAttribute("erro",
                            "Erro: O login '" + user.getLogin() + "' já está em uso.");
                    return "redirect:/configuracoes/utilizadores";
                }
            }
            user.setEmpresa(empresa);
            String senhaAleatoria = userRegistrationService.gerarSenhaAleatoria();
            user.setSenha(passwordEncoder.encode(senhaAleatoria));
            user.setForcarAlteracaoSenha(true);
            if (estabelecimentoIds != null && !estabelecimentoIds.isEmpty())
                user.setEstabelecimentos(
                        new java.util.HashSet<>(estabelecimentoRepository.findAllById(estabelecimentoIds)));
            userRepository.save(user);
            userRegistrationService.enviarCredenciaisPorEmail(user, senhaAleatoria);
        }
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

    @GetMapping("/impostos")
    public String impostos(Model model) {
        return "redirect:/configuracoes/fiscais/impostos";
    }

    @PostMapping("/impostos/salvar")
    public String salvarImposto(@ModelAttribute Imposto imposto, RedirectAttributes redirectAttributes) {
        impostoRepository.save(imposto);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/impostos";
    }

    @GetMapping("/impostos/eliminar/{id}")
    public String eliminarImposto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        impostoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/fiscais/impostos";
    }

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
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(err);
        }
        Map<String, Object> resultado = agtService.pingAgt(urlApi, token);
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(resultado);
    }

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

    @GetMapping("/email")
    public String email(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresaId);
            ConfiguracaoEmail cfg = new ConfiguracaoEmail();
            cfg.setSmtpHost(config.getEmailSmtpHost());
            cfg.setSmtpPorta(config.getEmailSmtpPorta());
            cfg.setSmtpUsername(config.getEmailSmtpUsername());
            cfg.setSmtpPassword(config.getEmailSmtpPassword());
            cfg.setSegurancaTipo(config.getEmailSegurancaTipo());
            cfg.setEmailRemetente(config.getEmailRemetente());
            cfg.setNomeRemetente(config.getEmailNomeRemetente());
            model.addAttribute("cfg", cfg);
        } else {
            model.addAttribute("cfg", cfgService.getEmail());
        }
        return "configuracoes/email";
    }

    @PostMapping("/email/salvar")
    public String salvarEmail(@ModelAttribute ConfiguracaoEmail cfg, RedirectAttributes ra) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            configuracaoEmpresaService.atualizarConfiguracaoEmail(empresaId, cfg.getSmtpHost(), cfg.getSmtpPorta(),
                    cfg.getSmtpUsername(), cfg.getSmtpPassword(), cfg.getSegurancaTipo(), cfg.getEmailRemetente(),
                    cfg.getNomeRemetente());
        } else {
            cfgService.saveEmail(cfg);
        }
        ra.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/email";
    }

    @PostMapping("/email/testar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testarEmail(@RequestBody Map<String, String> payload) {
        Map<String, Object> result = new HashMap<>();
        String dest = payload.get("emailDestino");
        if (dest == null || dest.isEmpty()) {
            result.put("sucesso", false);
            result.put("mensagem", "Email não informado.");
            return ResponseEntity.badRequest().contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(result);
        }
        try {
            Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
            dynamicMailService.enviarEmail(empresaId, dest, "Teste - Kwanza ERP", "Configuração de email OK.");
            result.put("sucesso", true);
            result.put("mensagem", "Email enviado para " + dest);
        } catch (Exception e) {
            result.put("sucesso", false);
            result.put("mensagem", "Erro: " + e.getMessage());
        }
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(result);
    }

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
        if (regimeFiscalRepository.count() == 0) {
            regimeFiscalRepository.save(new RegimeFiscal("Regime Geral", "GERAL",
                    "Para empresas com faturação superior a 350 Milhões de Kwanzas.", "fas fa-balance-scale"));
            regimeFiscalRepository.save(new RegimeFiscal("Regime Simplificado", "SIMPLIFICADO",
                    "Para PMEs com faturação entre 10 e 350 milhões de Kwanzas", "fas fa-shield-alt"));
            regimeFiscalRepository.save(new RegimeFiscal("Regime de Exclusão", "EXCLUSAO",
                    "Para negócios menores, com faturação inferior a 10 milhões de Kwanzas", "fas fa-ban"));
        }
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(new Empresa())
                : new Empresa();
        model.addAttribute("empresa", empresa);
        model.addAttribute("regimes", regimeFiscalRepository.findAll());
        model.addAttribute("novoRegime", new RegimeFiscal());
        model.addAttribute("todosImpostos", impostoRepository.findAll());
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
                redirectAttributes.addFlashAttribute("mensagem", "Regime Fiscal atualizado!");
            }
        }
        return "redirect:/configuracoes/fiscais/regime-fiscal";
    }

    @PostMapping("/fiscais/regime-fiscal/adicionar")
    public String salvarNovoRegime(@ModelAttribute RegimeFiscal regimeFiscal, RedirectAttributes redirectAttributes) {
        regimeFiscalRepository.save(regimeFiscal);
        redirectAttributes.addFlashAttribute("mensagem", "Regime adicionado!");
        return "redirect:/configuracoes/fiscais/regime-fiscal";
    }

    @PostMapping("/fiscais/regime-fiscal/associar-impostos")
    public String associarImpostosRegime(@RequestParam Long regimeId,
            @RequestParam(value = "impostoIds", required = false) List<Long> impostoIds,
            RedirectAttributes redirectAttributes) {
        RegimeFiscal regime = regimeFiscalRepository.findById(regimeId).orElse(null);
        if (regime != null) {
            regime.getImpostos().clear();
            if (impostoIds != null && !impostoIds.isEmpty()) {
                List<Imposto> impostosSelecionados = impostoRepository.findAllById(impostoIds);
                regime.getImpostos().addAll(impostosSelecionados);
            }
            regimeFiscalRepository.save(regime);
            redirectAttributes.addFlashAttribute("mensagem", "Impostos associados com sucesso ao regime " + regime.getNome() + "!");
        } else {
            redirectAttributes.addFlashAttribute("erro", "Regime fiscal não encontrado.");
        }
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

    @GetMapping("/estabelecimentos")
    public String estabelecimentos(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
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
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
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
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
        Estabelecimento est = estabelecimentoRepository.findById(id).orElse(null);
        if (est != null
                && (isSuperAdmin || (est.getEmpresa() != null && est.getEmpresa().getId().equals(currentEmpresaId)))) {
            estabelecimentoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagem",
                    messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
        }
        return "redirect:/configuracoes/estabelecimentos";
    }

    @GetMapping("/controlo-acesso")
    public String controloAcesso(@RequestParam(value = "usuarioId", required = false) Long usuarioId, Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERADMIN"));
        List<User> usuarios = isSuperAdmin ? userRepository.findAll() : userRepository.findByEmpresa_Id(empresaId);
        model.addAttribute("usuarios", usuarios);
        List<String> modulos = new java.util.ArrayList<>(
                ao.co.hzconsultoria.efacturacao.config.ModuloItens.ITENS_POR_MODULO.keySet());
        model.addAttribute("modulosList", modulos);
        model.addAttribute("modulosLabels", ao.co.hzconsultoria.efacturacao.config.ModuloItens.MODULO_LABELS);
        if (usuarioId != null) {
            User user = userRepository.findById(usuarioId).orElse(null);
            if (user != null) {
                for (String modulo : modulos) {
                    Optional<PermissaoModulo> opt = permissaoModuloRepository.findByModuloAndUsuario_Id(modulo,
                            usuarioId);
                    boolean isAdmin = "ADMIN".equals(user.getRole());
                    boolean isSuperAdminUser = "SUPERADMIN".equals(user.getRole());
                    boolean deveEstarAtivo = isSuperAdminUser ? true
                            : (isAdmin ? !modulo.equals("PAINEL_GLOBAL")
                                    : ("GESTOR".equals(user.getRole())
                                            ? modulo.matches("DASHBOARD|VENDAS|STOCK|ENTIDADES|FACTURACAO|FINANCEIRO")
                                            : modulo.equals("VENDAS")));
                    if (!opt.isPresent())
                        permissaoModuloRepository.save(new PermissaoModulo(modulo, user, deveEstarAtivo));
                }
                model.addAttribute("usuarioSelecionado", user);
                Map<String, Boolean> permsMap = new HashMap<>();
                permissaoModuloRepository.findByUsuario_Id(usuarioId)
                        .forEach(p -> permsMap.put(p.getModulo(), p.isAtivo()));
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

    @GetMapping("/empresa/configuracoes")
    public String configuracoeEmpresa(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null)
            return "redirect:/dashboard";
        ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresaId);
        model.addAttribute("configuracao", config);
        return "configuracoes/empresa-config";
    }

    @PostMapping("/empresa/salvar-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarConfiguracaoEmail(@RequestParam String smtpHost,
            @RequestParam int smtpPorta, @RequestParam String smtpUsername, @RequestParam String smtpPassword,
            @RequestParam String segurancaTipo, @RequestParam String remetente, @RequestParam String nomeRemetente) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null || !ao.co.hzconsultoria.efacturacao.security.SecurityUtils.temAcessoEmpresa(empresaId)) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Acesso negado");
            return ResponseEntity.status(403).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(res);
        }
        try {
            configuracaoEmpresaService.atualizarConfiguracaoEmail(empresaId, smtpHost, smtpPorta, smtpUsername,
                    smtpPassword, segurancaTipo, remetente, nomeRemetente);
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", true);
            res.put("mensagem", messageSource.getMessage("msg.sucesso.salvo", null, LocaleContextHolder.getLocale()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Erro: " + e.getMessage());
            return ResponseEntity.ok(res);
        }
    }

    @PostMapping("/empresa/salvar-storage")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarConfiguracaoStorage(@RequestParam String storageTipo,
            @RequestParam String caminhoBase, @RequestParam int tamanhoMaxFicheiro,
            @RequestParam int tamanhoMaxRequest) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null || !ao.co.hzconsultoria.efacturacao.security.SecurityUtils.temAcessoEmpresa(empresaId)) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Acesso negado");
            return ResponseEntity.status(403).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(res);
        }
        try {
            configuracaoEmpresaService.atualizarConfiguracaoStorage(empresaId, storageTipo, caminhoBase,
                    tamanhoMaxFicheiro, tamanhoMaxRequest);
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", true);
            res.put("mensagem", messageSource.getMessage("msg.sucesso.salvo", null, LocaleContextHolder.getLocale()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Erro: " + e.getMessage());
            return ResponseEntity.ok(res);
        }
    }

    @PostMapping("/empresa/salvar-seguranca")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarPoliticaSeguranca(@RequestParam int tempoExpiracaoSessao,
            @RequestParam(defaultValue = "false") boolean twoFactorAtivo,
            @RequestParam(defaultValue = "true") boolean requireUppercase,
            @RequestParam(defaultValue = "true") boolean requireNumbers,
            @RequestParam(defaultValue = "false") boolean requireSpecialChars,
            @RequestParam int comprimentoMinPassword) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null || !ao.co.hzconsultoria.efacturacao.security.SecurityUtils.temAcessoEmpresa(empresaId)) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Acesso negado");
            return ResponseEntity.status(403).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(res);
        }
        try {
            configuracaoEmpresaService.atualizarPoliticaSeguranca(empresaId, tempoExpiracaoSessao, twoFactorAtivo,
                    requireUppercase, requireNumbers, requireSpecialChars, comprimentoMinPassword);
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", true);
            res.put("mensagem", messageSource.getMessage("msg.sucesso.salvo", null, LocaleContextHolder.getLocale()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Erro: " + e.getMessage());
            return ResponseEntity.ok(res);
        }
    }

    @PostMapping("/empresa/salvar-agt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarConfiguracaoAGT(
            @RequestParam(defaultValue = "false") boolean habilitada, @RequestParam String urlServico,
            @RequestParam String usuario, @RequestParam String senha,
            @RequestParam(required = false) String certificado) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null || !ao.co.hzconsultoria.efacturacao.security.SecurityUtils.temAcessoEmpresa(empresaId)) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Acesso negado");
            return ResponseEntity.status(403).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(res);
        }
        try {
            configuracaoEmpresaService.atualizarConfiguracaoAGT(empresaId, habilitada, urlServico, usuario, senha,
                    certificado != null ? certificado : "");
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", true);
            res.put("mensagem", messageSource.getMessage("msg.sucesso.salvo", null, LocaleContextHolder.getLocale()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            Map<String, Object> res = new HashMap<>();
            res.put("sucesso", false);
            res.put("mensagem", "Erro: " + e.getMessage());
            return ResponseEntity.ok(res);
        }
    }
}
