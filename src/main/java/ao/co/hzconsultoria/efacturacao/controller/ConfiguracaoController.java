package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
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
    private MessageSource messageSource;
    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.AgtService agtService;

    // ─── Dados da Empresa ────────────────────────────────────────────────────
    @GetMapping("/empresa")
    public String empresa(Model model) {
        List<Empresa> empresas = empresaRepository.findAll();
        model.addAttribute("empresa", empresas.isEmpty() ? new Empresa() : empresas.get(0));
        model.addAttribute("regimes", regimeFiscalRepository.findAll());
        return "configuracoes/empresa";
    }

    @PostMapping("/empresa/salvar")
    public String salvarEmpresa(@ModelAttribute Empresa empresa, 
                               @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                               RedirectAttributes redirectAttributes) {
        
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String uploadDir = "src/main/resources/static/uploads/logo/";
                Path uploadPath = Paths.get(uploadDir);
                
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                String fileName = "logo_empresa_" + empresa.getNif() + "_" + logoFile.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(logoFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // Set the path to be used in the URL/PDF
                empresa.setLogotipo(uploadDir + fileName);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        empresaRepository.save(empresa);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.salvo", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/empresa";
    }

    // ─── Utilizadores e Perfis ───────────────────────────────────────────────
    @GetMapping("/utilizadores")
    public String utilizadores(Model model) {
        model.addAttribute("utilizadores", userRepository.findAll());
        model.addAttribute("novoUsuario", new User());
        return "configuracoes/utilizadores";
    }

    @PostMapping("/utilizadores/salvar")
    public String salvarUsuario(@ModelAttribute User user, RedirectAttributes redirectAttributes) {
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/utilizadores";
    }

    @GetMapping("/utilizadores/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
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
        model.addAttribute("series", serieRepository.findAll());
        model.addAttribute("novaSerie", new Serie());
        return "configuracoes/series";
    }

    @PostMapping("/series/salvar")
    public String salvarSerie(@ModelAttribute Serie serie, RedirectAttributes redirectAttributes) {
        serieRepository.save(serie);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.operacao", null, LocaleContextHolder.getLocale()));
        return "redirect:/configuracoes/series";
    }

    @GetMapping("/series/eliminar/{id}")
    public String eliminarSerie(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        serieRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem",
                messageSource.getMessage("msg.sucesso.removido", null, LocaleContextHolder.getLocale()));
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

    // ─── Parâmetros Gerais ───────────────────────────────────────────────────
    public static Sistema sistemaConfig;
    static {
        sistemaConfig = new Sistema();
        sistemaConfig.setNome("Sistema de Facturação");
        sistemaConfig.setVersao("1.0.0");
        sistemaConfig.setEmailSuporte("suporte@facturacao.com");
        sistemaConfig.setBackup(true);
        sistemaConfig.setTema("light");
    }

    @GetMapping("/geral")
    public String geral(Model model) {
        model.addAttribute("sistema", sistemaConfig);
        return "configuracoes/geral";
    }

    @PostMapping("/geral/salvar")
    public String salvarSistema(@ModelAttribute Sistema sistema, RedirectAttributes redirectAttributes) {
        sistemaConfig = sistema;
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

        List<Empresa> empresas = empresaRepository.findAll();
        model.addAttribute("empresa", empresas.isEmpty() ? new Empresa() : empresas.get(0));
        model.addAttribute("regimes", regimeFiscalRepository.findAll());
        model.addAttribute("novoRegime", new RegimeFiscal());
        return "configuracoes/fiscais/regime_fiscal";
    }

    @PostMapping("/fiscais/regime-fiscal/salvar")
    public String salvarRegimeFiscal(@RequestParam String regime, RedirectAttributes redirectAttributes) {
        List<Empresa> empresas = empresaRepository.findAll();
        if (!empresas.isEmpty()) {
            Empresa e = empresas.get(0);
            e.setRegimeFiscal(regime);
            empresaRepository.save(e);
            redirectAttributes.addFlashAttribute("mensagem", "Regime Fiscal actualizado com sucesso!");
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
}
