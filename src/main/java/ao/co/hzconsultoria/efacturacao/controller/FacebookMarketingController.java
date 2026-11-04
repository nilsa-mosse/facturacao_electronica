package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import ao.co.hzconsultoria.efacturacao.service.FacebookService;
import ao.co.hzconsultoria.efacturacao.service.FacebookService.ResultadoPublicacao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Controller do módulo de Marketing no Facebook.
 * Gere a configuração, publicação manual e visualização do histórico.
 *
 * URLs:
 *   GET  /marketing/facebook           → Painel principal
 *   GET  /marketing/facebook/config    → Formulário de configuração
 *   POST /marketing/facebook/config    → Salvar configuração
 *   POST /marketing/facebook/testar    → Testar conexão (AJAX/JSON)
 *   POST /marketing/facebook/publicar/{produtoId}  → Publicar produto manual
 *   POST /marketing/facebook/publicar-promocoes    → Publicar promoções agora
 *   POST /marketing/facebook/mensagem              → Publicar mensagem personalizada
 *   GET  /marketing/facebook/historico             → Histórico de publicações
 */
@Controller
@RequestMapping("/marketing/facebook")
public class FacebookMarketingController {

    @Autowired
    private FacebookService facebookService;

    @Autowired
    private ConfiguracaoFacebookRepository fbConfigRepo;

    @Autowired
    private PublicacaoFacebookRepository publicacaoRepo;

    @Autowired
    private ProdutoRepository produtoRepo;

    @Autowired
    private EmpresaRepository empresaRepo;

    // ─────────────────────────────────────────────────────────────────────────
    //  PAINEL PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping({"", "/"})
    public String painelFacebook(Model model) {
        Long empresaId = getEmpresaId();
        if (empresaId == null) {
            return "redirect:/dashboard";
        }

        ConfiguracaoFacebook config = facebookService.obterOuCriarConfig(empresaId);
        List<PublicacaoFacebook> historico = publicacaoRepo.findTop20ByEmpresaIdOrderByDataPublicacaoDesc(empresaId);
        List<Produto> produtos = produtoRepo.findByEmpresa_Id(empresaId);
        List<Produto> produtosEmPromocao = new ArrayList<>();
        for (Produto p : produtos) {
            if (p.isEmPromocao()) produtosEmPromocao.add(p);
        }

        long totalSucesso = publicacaoRepo.countPublicacoesHoje(
                empresaId, java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

        model.addAttribute("fbConfig", config);
        model.addAttribute("historico", historico);
        model.addAttribute("produtos", produtos);
        model.addAttribute("produtosEmPromocao", produtosEmPromocao);
        model.addAttribute("totalSucessoHoje", totalSucesso);
        model.addAttribute("activePage", "marketing");
        return "facebook/painel-facebook";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CONFIGURAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping({"/config", "/config/"})
    public String configFacebook(Model model) {
        Long empresaId = getEmpresaId();
        if (empresaId == null) {
            return "redirect:/dashboard";
        }

        ConfiguracaoFacebook config = facebookService.obterOuCriarConfig(empresaId);
        model.addAttribute("fbConfig", config);
        model.addAttribute("activePage", "marketing");
        return "facebook/config-facebook";
    }

    @PostMapping("/config")
    public String salvarConfig(@ModelAttribute ConfiguracaoFacebook configForm,
                               RedirectAttributes redirectAttrs) {
        Long empresaId = getEmpresaId();
        if (empresaId == null) {
            return "redirect:/dashboard";
        }

        try {
            ConfiguracaoFacebook config = facebookService.obterOuCriarConfig(empresaId);

            config.setPageAccessToken(configForm.getPageAccessToken());
            config.setPageId(configForm.getPageId());
            config.setPageName(configForm.getPageName());
            config.setAppId(configForm.getAppId());
            config.setAppSecret(configForm.getAppSecret());
            config.setAtivo(configForm.isAtivo());
            config.setPublicarPromocoesAuto(configForm.isPublicarPromocoesAuto());
            config.setPublicarStockBaixo(configForm.isPublicarStockBaixo());
            config.setPublicarNovosProdutos(configForm.isPublicarNovosProdutos());
            config.setTemplateMensagemProduto(configForm.getTemplateMensagemProduto());
            config.setTemplateMensagemPromocao(configForm.getTemplateMensagemPromocao());
            config.setHashtags(configForm.getHashtags());
            config.setMaxPublicacoesPorDia(
                    configForm.getMaxPublicacoesPorDia() > 0 ? configForm.getMaxPublicacoesPorDia() : 5);

            facebookService.salvarConfig(config);
            redirectAttrs.addFlashAttribute("sucesso", "✅ Configuração do Facebook guardada com sucesso!");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", "❌ Erro ao guardar configuração: " + e.getMessage());
        }
        return "redirect:/marketing/facebook/config";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TESTAR CONEXÃO (AJAX)
    // ─────────────────────────────────────────────────────────────────────────

    @RequestMapping(value = "/testar", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testarConexao(
            @RequestBody(required = false) Map<String, String> body,
            @RequestParam(required = false) Map<String, String> params) {

        String token = null;
        String pageId = null;

        if (body != null) {
            token = body.get("pageAccessToken");
            pageId = body.get("pageId");
        }
        if ((token == null || token.isEmpty()) && params != null) {
            token = params.get("pageAccessToken");
            pageId = params.get("pageId");
        }

        if (token == null || token.trim().isEmpty() || pageId == null || pageId.trim().isEmpty()) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("sucesso", false);
            erro.put("mensagem", "Por favor preencha o Page Access Token e o ID da página antes de testar.");
            return ResponseEntity.badRequest().body(erro);
        }

        Map<String, Object> resultado = facebookService.verificarConexao(token, pageId);
        return ResponseEntity.ok(resultado);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLICAÇÃO MANUAL DE PRODUTO
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/publicar/{produtoId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> publicarProduto(
            @PathVariable Long produtoId,
            @RequestParam(defaultValue = "PRODUTO") String tipo) {

        Long empresaId = getEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("erro", "Não autenticado"));
        }

        Optional<Produto> prodOpt = produtoRepo.findById(produtoId);
        if (!prodOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Produto produto = prodOpt.get();

        ResultadoPublicacao resultado;
        if ("PROMOCAO".equalsIgnoreCase(tipo)) {
            resultado = facebookService.publicarPromocao(produto, empresaId, true);
        } else {
            resultado = facebookService.publicarProduto(produto, empresaId, true);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("sucesso", resultado.isSucesso());
        resp.put("mensagem", resultado.getMensagem());
        resp.put("postId", resultado.getPostId());

        return ResponseEntity.ok(resp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLICAR PROMOÇÕES AGORA (disparo manual do scheduler)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/publicar-promocoes")
    public String publicarPromocoesAgora(RedirectAttributes redirectAttrs) {
        Long empresaId = getEmpresaId();
        if (empresaId == null) {
            return "redirect:/dashboard";
        }

        try {
            List<ResultadoPublicacao> resultados = facebookService.publicarPromocoesAutomaticas(empresaId);
            long sucesso = resultados.stream().filter(ResultadoPublicacao::isSucesso).count();
            long falha   = resultados.stream().filter(r -> !r.isSucesso()).count();

            if (resultados.isEmpty()) {
                redirectAttrs.addFlashAttribute("aviso", "⚠️ Nenhum produto em promoção encontrado ou limite diário atingido.");
            } else {
                redirectAttrs.addFlashAttribute("sucesso",
                        "✅ Processo concluído: " + sucesso + " publicações com sucesso, " + falha + " com falha.");
            }
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("erro", "❌ Erro: " + e.getMessage());
        }
        return "redirect:/marketing/facebook";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLICAR MENSAGEM PERSONALIZADA (COM SUPORTE A IMAGEM / VÍDEO)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/mensagem")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> publicarMensagem(
            @RequestParam(value = "mensagem", required = false) String mensagemParam,
            @RequestParam(value = "mediaUrl", required = false) String mediaUrl,
            @RequestParam(value = "ficheiro", required = false) org.springframework.web.multipart.MultipartFile ficheiro,
            @RequestBody(required = false) Map<String, String> jsonBody) {

        Long empresaId = getEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.status(401).body(Collections.singletonMap("erro", "Não autenticado"));
        }

        String mensagem = (jsonBody != null && jsonBody.containsKey("mensagem"))
                ? jsonBody.get("mensagem") : mensagemParam;

        if (mensagem == null || mensagem.trim().isEmpty()) {
            Map<String, Object> erro = new HashMap<>();
            erro.put("sucesso", false);
            erro.put("mensagem", "A mensagem não pode estar vazia.");
            return ResponseEntity.badRequest().body(erro);
        }

        byte[] mediaBytes = null;
        String mediaType = null;
        String mediaNome = null;

        if (ficheiro != null && !ficheiro.isEmpty()) {
            try {
                mediaBytes = ficheiro.getBytes();
                mediaType = ficheiro.getContentType();
                mediaNome = ficheiro.getOriginalFilename();
            } catch (Exception e) {
                // Log and continue with text
            }
        }

        ResultadoPublicacao resultado = facebookService.publicarMensagemPersonalizada(
                mensagem, empresaId, mediaBytes, mediaType, mediaNome, mediaUrl);

        Map<String, Object> resp = new HashMap<>();
        resp.put("sucesso", resultado.isSucesso());
        resp.put("mensagem", resultado.getMensagem());
        resp.put("postId", resultado.getPostId());

        return ResponseEntity.ok(resp);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HISTÓRICO
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping({"/historico", "/historico/"})
    public String historico(Model model) {
        Long empresaId = getEmpresaId();
        if (empresaId == null) {
            return "redirect:/dashboard";
        }

        List<PublicacaoFacebook> historico = publicacaoRepo.findByEmpresaIdOrderByDataPublicacaoDesc(empresaId);
        long totalSucesso = historico.stream().filter(h -> "SUCESSO".equals(h.getStatus())).count();
        long totalFalha = historico.stream().filter(h -> "FALHA".equals(h.getStatus())).count();
        long totalAutomaticos = historico.stream().filter(h -> !h.isPublicacaoManual()).count();

        model.addAttribute("historico", historico);
        model.addAttribute("totalSucesso", totalSucesso);
        model.addAttribute("totalFalha", totalFalha);
        model.addAttribute("totalAutomaticos", totalAutomaticos);
        model.addAttribute("activePage", "marketing");
        return "facebook/historico-facebook";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITÁRIO: obter ID da empresa do utilizador autenticado
    // ─────────────────────────────────────────────────────────────────────────

    private Long getEmpresaId() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            List<Empresa> empresas = empresaRepo.findAll();
            if (!empresas.isEmpty()) {
                empresaId = empresas.get(0).getId();
            }
        }
        return empresaId;
    }
}
