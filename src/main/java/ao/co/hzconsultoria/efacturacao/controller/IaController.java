package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.service.ia.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ia")
public class IaController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private IaAnaliticaService iaAnaliticaService;

    @Autowired
    private PesquisaInteligenteService pesquisaInteligenteService;

    // --- Página Principal do Painel de IA ---
    @GetMapping
    public String dashboardIa(Model model) {
        model.addAttribute("iaStatus", geminiService.isDisponivel() ? "ONLINE (Google Gemini)" : "MODO LOCAL / ESTATÍSTICO");
        model.addAttribute("previsaoVendas", iaAnaliticaService.preverVendasProximoMes(null));
        model.addAttribute("rupturaStock", iaAnaliticaService.preverRupturaStock(null));
        model.addAttribute("clientesRisco", iaAnaliticaService.detectarClientesRiscoInadimplencia(null));
        model.addAttribute("promocoesSugeridas", iaAnaliticaService.sugerirPromocoes(null));
        return "ia/dashboard";
    }

    // --- Endpoint do Chat Assistente Virtual ---
    @PostMapping("/chat")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> payload) {
        String mensagem = payload.get("mensagem");
        String sistemaPrompt = "És o assistente virtual inteligente do Kwanza ERP, um software de facturação electrónica certificado pela AGT em Angola. Responde sempre em Português de Angola de forma útil, cortês e objectiva.";
        
        String resposta = geminiService.gerarTexto(sistemaPrompt, mensagem);
        
        Map<String, Object> res = new HashMap<>();
        res.put("sucesso", true);
        res.put("resposta", resposta);
        return ResponseEntity.ok(res);
    }

    // --- Endpoint para Geração Automática de Descrição de Produto ---
    @PostMapping("/gerar-descricao-produto")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> gerarDescricaoProduto(@RequestBody Map<String, String> payload) {
        String nomeProduto = payload.get("nome");
        String categoria = payload.get("categoria");
        String preco = payload.get("preco");

        String promptSistema = "És um perito em marketing e copywriting de comércio eletrónico em Angola.";
        String promptUser = "Escreve uma descrição comercial atrativa, curta (2 a 3 frases) e profissional para o produto: '" 
                + nomeProduto + "' da categoria '" + (categoria != null ? categoria : "Geral") + "' com preço " + (preco != null ? preco : "") + " Kz.";

        String descricao = geminiService.gerarTexto(promptSistema, promptUser);

        Map<String, Object> res = new HashMap<>();
        res.put("sucesso", true);
        res.put("descricao", descricao);
        return ResponseEntity.ok(res);
    }

    // --- Endpoint para Pesquisa Inteligente ("Vendas do João em Junho") ---
    @GetMapping("/pesquisa-inteligente")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> pesquisaInteligente(@RequestParam("q") String query) {
        Map<String, Object> resultado = pesquisaInteligenteService.pesquisar(query, null);
        return ResponseEntity.ok(resultado);
    }

    // --- Endpoint de Sugestão de Preços Dinâmicos ---
    @GetMapping("/sugestao-precos")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> sugestaoPrecos() {
        return ResponseEntity.ok(iaAnaliticaService.sugerirPrecosProdutos(null));
    }

    // --- Endpoint de Resposta Automática Simulada para WhatsApp ---
    @PostMapping("/whatsapp/auto-resposta")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> whatsappAutoResposta(@RequestBody Map<String, String> payload) {
        String mensagemCliente = payload.get("mensagem");
        String numeroCliente = payload.get("telefone");

        String promptSistema = "És o bot de atendimento automático do Kwanza ERP via WhatsApp. Responde de forma curta e amigável para envio de mensagem instantânea.";
        String respostaBot = geminiService.gerarTexto(promptSistema, mensagemCliente);

        Map<String, Object> res = new HashMap<>();
        res.put("sucesso", true);
        res.put("telefone", numeroCliente);
        res.put("respostaEnviada", respostaBot);
        return ResponseEntity.ok(res);
    }
}
