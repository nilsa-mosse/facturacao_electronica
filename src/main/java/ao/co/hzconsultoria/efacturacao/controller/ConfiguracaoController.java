package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.net.URL;
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
    private ConfiguracaoAGTRepository configuracaoAGTRepository;

    // ─── Dados da Empresa ────────────────────────────────────────────────────
    @GetMapping("/empresa")
    public String empresa(Model model) {
        List<Empresa> empresas = empresaRepository.findAll();
        model.addAttribute("empresa", empresas.isEmpty() ? new Empresa() : empresas.get(0));
        return "configuracoes/empresa";
    }

    @PostMapping("/empresa/salvar")
    public String salvarEmpresa(@ModelAttribute Empresa empresa) {
        empresaRepository.save(empresa);
        return "redirect:/configuracoes/empresa?success";
    }

    // ─── Utilizadores e Perfis ───────────────────────────────────────────────
    @GetMapping("/utilizadores")
    public String utilizadores(Model model) {
        model.addAttribute("utilizadores", userRepository.findAll());
        model.addAttribute("novoUsuario", new User());
        return "configuracoes/utilizadores";
    }

    @PostMapping("/utilizadores/salvar")
    public String salvarUsuario(@ModelAttribute User user) {
        userRepository.save(user);
        return "redirect:/configuracoes/utilizadores?success";
    }

    @GetMapping("/utilizadores/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/configuracoes/utilizadores?deleted";
    }

    // ─── Impostos e Taxas ────────────────────────────────────────────────────
    @GetMapping("/impostos")
    public String impostos(Model model) {
        model.addAttribute("impostos", impostoRepository.findAll());
        model.addAttribute("novoImposto", new Imposto());
        return "configuracoes/impostos";
    }

    @PostMapping("/impostos/salvar")
    public String salvarImposto(@ModelAttribute Imposto imposto) {
        impostoRepository.save(imposto);
        return "redirect:/configuracoes/impostos?success";
    }

    @GetMapping("/impostos/eliminar/{id}")
    public String eliminarImposto(@PathVariable Long id) {
        impostoRepository.deleteById(id);
        return "redirect:/configuracoes/impostos?deleted";
    }

    // ─── Séries de Facturação ────────────────────────────────────────────────
    @GetMapping("/series")
    public String series(Model model) {
        model.addAttribute("series", serieRepository.findAll());
        model.addAttribute("novaSerie", new Serie());
        return "configuracoes/series";
    }

    @PostMapping("/series/salvar")
    public String salvarSerie(@ModelAttribute Serie serie) {
        serieRepository.save(serie);
        return "redirect:/configuracoes/series?success";
    }

    @GetMapping("/series/eliminar/{id}")
    public String eliminarSerie(@PathVariable Long id) {
        serieRepository.deleteById(id);
        return "redirect:/configuracoes/series?deleted";
    }

    // ─── Moedas e Câmbios ────────────────────────────────────────────────────
    @GetMapping("/moedas")
    public String moedas(Model model) {
        model.addAttribute("moedas", moedaRepository.findAll());
        model.addAttribute("novaMoeda", new Moeda());
        return "configuracoes/moedas";
    }

    @PostMapping("/moedas/salvar")
    public String salvarMoeda(@ModelAttribute Moeda moeda) {
        moedaRepository.save(moeda);
        return "redirect:/configuracoes/moedas?success";
    }

    @GetMapping("/moedas/eliminar/{id}")
    public String eliminarMoeda(@PathVariable Long id) {
        moedaRepository.deleteById(id);
        return "redirect:/configuracoes/moedas?deleted";
    }

    // ─── Métodos de Pagamento ────────────────────────────────────────────────
    @GetMapping("/pagamentos")
    public String pagamentos(Model model) {
        model.addAttribute("metodos", metodoPagamentoRepository.findAll());
        model.addAttribute("novoMetodo", new MetodoPagamento());
        return "configuracoes/pagamentos";
    }

    @PostMapping("/pagamentos/salvar")
    public String salvarPagamento(@ModelAttribute MetodoPagamento metodo) {
        metodoPagamentoRepository.save(metodo);
        return "redirect:/configuracoes/pagamentos?success";
    }

    @GetMapping("/pagamentos/eliminar/{id}")
    public String eliminarPagamento(@PathVariable Long id) {
        metodoPagamentoRepository.deleteById(id);
        return "redirect:/configuracoes/pagamentos?deleted";
    }

    // ─── Comunicação AGT ─────────────────────────────────────────────────────
    @GetMapping("/comunicacao-agt")
    public String comunicacaoAgt(Model model) {
        List<ConfiguracaoAGT> cfgs = configuracaoAGTRepository.findAll();
        model.addAttribute("config", cfgs.isEmpty() ? new ConfiguracaoAGT() : cfgs.get(0));
        return "configuracoes/comunicacao-agt";
    }

    @PostMapping("/comunicacao-agt/salvar")
    public String salvarComunicacaoAgt(@ModelAttribute ConfiguracaoAGT config) {
        configuracaoAGTRepository.save(config);
        return "redirect:/configuracoes/comunicacao-agt?success";
    }

    @PostMapping("/comunicacao-agt/testar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testarConexaoAgt(@RequestBody Map<String, String> payload) {
        Map<String, Object> resultado = new HashMap<>();
        String urlApi = payload.get("urlApi");
        String token = payload.get("token");

        if (urlApi == null || urlApi.trim().isEmpty()) {
            resultado.put("sucesso", false);
            resultado.put("mensagem", "A URL da API não foi preenchida.");
            return ResponseEntity.badRequest().body(resultado);
        }

        long inicio = System.currentTimeMillis();
        try {
            URL url = new URL(urlApi);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            // Adicionar token de autenticação se disponível
            if (token != null && !token.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setRequestProperty("Accept", "application/json");

            int codigoHttp = conn.getResponseCode();
            long tempoMs = System.currentTimeMillis() - inicio;

            resultado.put("codigoHttp", codigoHttp);
            resultado.put("tempoMs", tempoMs);

            if (codigoHttp >= 200 && codigoHttp < 300) {
                resultado.put("sucesso", true);
                resultado.put("mensagem", "Conexão estabelecida com sucesso! (HTTP " + codigoHttp + ")");
            } else if (codigoHttp == 401 || codigoHttp == 403) {
                resultado.put("sucesso", false);
                resultado.put("mensagem", "Servidor acessível, mas o token foi rejeitado. (HTTP " + codigoHttp + ")");
            } else {
                resultado.put("sucesso", false);
                resultado.put("mensagem", "Servidor respondeu com código HTTP " + codigoHttp + ".");
            }

            conn.disconnect();
        } catch (java.net.SocketTimeoutException e) {
            resultado.put("sucesso", false);
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("mensagem", "Tempo de conexão expirado. Verifique a URL e a sua rede.");
        } catch (java.net.UnknownHostException e) {
            resultado.put("sucesso", false);
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("mensagem", "Endereço não encontrado. Verifique a URL da API.");
        } catch (Exception e) {
            resultado.put("sucesso", false);
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("mensagem", "Erro ao conectar: " + e.getMessage());
        }

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
    public String salvarGeral(@ModelAttribute Sistema sistema) {
        sistemaConfig = sistema;
        return "redirect:/configuracoes/geral?success";
    }
}
