package ao.co.hzconsultoria.efacturacao.service.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Cliente HTTP resiliente para a API GRATUITA do Google Gemini.
 * Suporta fallback automático de modelos (gemini-1.5-flash-latest, gemini-2.0-flash, gemini-2.5-flash, gemini-pro).
 */
@Service
public class GeminiService {

    @Value("${ia.gemini.api-key:}")
    private String apiKey;

    @Value("${ia.gemini.model:gemini-1.5-flash-latest}")
    private String model;

    @Value("${ia.habilitada:true}")
    private boolean iaHabilitada;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Lista de modelos suportados para tentar automaticamente caso o primário não esteja disponível
    private static final String[] FALLBACK_MODELS = {
        "gemini-1.5-flash-latest",
        "gemini-2.0-flash",
        "gemini-2.5-flash",
        "gemini-1.5-pro",
        "gemini-pro"
    };

    public boolean isDisponivel() {
        return iaHabilitada && apiKey != null && !apiKey.trim().isEmpty() 
               && !apiKey.equals("SUA_CHAVE_GEMINI_GRATIS");
    }

    /**
     * Envia um prompt para o Google Gemini e retorna a resposta textual.
     */
    public String gerarTexto(String promptSistema, String promptUsuario) {
        if (!isDisponivel()) {
            return getMensagemModoOffline(promptSistema, promptUsuario);
        }

        // Tentar modelo configurado + modelos de fallback
        List<String> modelosParaTestar = new ArrayList<>();
        if (model != null && !model.trim().isEmpty()) {
            modelosParaTestar.add(model.trim());
        }
        for (String m : FALLBACK_MODELS) {
            if (!modelosParaTestar.contains(m)) {
                modelosParaTestar.add(m);
            }
        }

        String ultimoErro = null;

        for (String m : modelosParaTestar) {
            try {
                String endpoint = GEMINI_API_URL + m + ":generateContent?key=" + apiKey.trim();
                
                Map<String, Object> body = new HashMap<>();
                
                if (promptSistema != null && !promptSistema.isEmpty()) {
                    Map<String, Object> systemInstruction = new HashMap<>();
                    Map<String, Object> parts = new HashMap<>();
                    parts.put("text", promptSistema);
                    systemInstruction.put("parts", Collections.singletonList(parts));
                    body.put("system_instruction", systemInstruction);
                }

                Map<String, Object> contentMap = new HashMap<>();
                Map<String, Object> userPart = new HashMap<>();
                userPart.put("text", promptUsuario);
                contentMap.put("parts", Collections.singletonList(userPart));
                body.put("contents", Collections.singletonList(contentMap));

                String jsonPayload = objectMapper.writeValueAsString(body);

                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
                
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                }

                if (code == 200) {
                    JsonNode root = objectMapper.readTree(sb.toString());
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && candidates.size() > 0) {
                        JsonNode parts = candidates.get(0).path("content").path("parts");
                        if (parts.isArray() && parts.size() > 0) {
                            return parts.get(0).path("text").asText();
                        }
                    }
                } else if (code == 404) {
                    // Se for 404, ignora e tenta o próximo modelo no loop
                    ultimoErro = "Modelo " + m + " indisponível (404).";
                    continue;
                } else if (code == 429) {
                    // Limite de taxa/quota atingido: tentar próximo modelo ou responder via assistente local
                    ultimoErro = "Limite de quota temporário atingido (429).";
                    continue;
                } else {
                    return "⚠️ Erro no serviço Gemini (" + code + "): " + sb.toString();
                }

            } catch (Exception e) {
                ultimoErro = e.getMessage();
            }
        }

        // Se todos os modelos da API excederem a quota ou derem erro, responder graciosamente via Assistente Local
        return getMensagemModoOffline(promptSistema, promptUsuario);
    }

    /**
     * Respostas offline Inteligentes baseadas em Regras (para quando não houver chave de API configurada)
     */
    private String getMensagemModoOffline(String sistema, String usuario) {
        String query = removerAcentos(usuario.toLowerCase());
        
        if (query.contains("ola") || query.contains("oi") || query.contains("bom dia") || query.contains("boa tarde") || query.contains("boa noite") || query.contains("ajuda") || query.contains("inicio")) {
            return "👋 Olá! Sou o Assistente Virtual Inteligente do **Kwanza ERP**.\n\n" +
                   "Posso ajudá-lo com:\n" +
                   "• **Facturação & Vendas**: Como emitir Faturas (FT), Recibos (FR) e Notas de Crédito.\n" +
                   "• **Conformidade AGT & SAF-T**: Como gerar o ficheiro SAF-T AO auditável.\n" +
                   "• **Gestão de Stocks**: Previsão de rutura e controlo de inventário.\n" +
                   "• **Clientes & Crédito**: Deteção de clientes em risco de inadimplência.\n\n" +
                   "O que deseja consultar agora?";
        }
        if (query.contains("fatura") || query.contains("factura") || query.contains("emitir") || query.contains("venda") || query.contains("recibo") || query.contains("pdv")) {
            return "📄 **Como Emitir Documentos de Venda no Kwanza ERP**:\n\n" +
                   "1. Aceda ao menu **Vendas** -> **Nova Venda (PDV)**.\n" +
                   "2. Selecione o Cliente (ou 'Consumidor Final').\n" +
                   "3. Adicione os produtos ao carrinho e selecione o meio de pagamento (TPA, Cachet, Transferência).\n" +
                   "4. Clique em **Finalizar e Emitir**.\n\n" +
                   "O documento é assinado digitalmente com o algoritmo RSA e estrutura exigida pela AGT em Angola.";
        }
        if (query.contains("agt") || query.contains("saft") || query.contains("sift") || query.contains("imposto") || query.contains("iva")) {
            return "🏛️ **Conformidade AGT & Exportação SAF-T AO**:\n\n" +
                   "1. Aceda ao menu **Configurações** -> **Exportação SAF-T**.\n" +
                   "2. Selecione o Mês e Ano do período fiscal pretendido.\n" +
                   "3. Clique em **Descarregar Ficheiro XML**.\n\n" +
                   "O ficheiro gerado é compatível com o portal e-Fatura da AGT de Angola.";
        }
        if (query.contains("stock") || query.contains("estoque") || query.contains("produto") || query.contains("inventario") || query.contains("artigo")) {
            return "📦 **Gestão de Stock & Inventário**:\n\n" +
                   "• Para cadastrar novos produtos: vá a **Produtos** -> **Novo Produto**.\n" +
                   "• Para ver produtos em risco de rutura: consulte o widget **Previsão de Ruptura de Stock** nesta mesma página.\n" +
                   "• O sistema recalcula automaticamente a disponibilidade com base no consumo diário dos últimos 30 dias.";
        }
        if (query.contains("cliente") || query.contains("divida") || query.contains("inadimplente") || query.contains("pagamento")) {
            return "👥 **Deteção de Risco de Clientes**:\n\n" +
                   "Nesta página de Inteligência Artificial, a tabela **Deteção de Risco de Inadimplência** identifica clientes com faturas pendentes há mais de 30 ou 60 dias, recomendando o bloqueio de vendas a crédito.";
        }

        return "💡 **Assistente Kwanza ERP**:\n\n" +
               "Compreendi a sua questão sobre: *\"" + usuario + "\"*.\n\n" +
               "📌 **Sugestão**: Pode perguntar-me sobre *\"Como emitir fatura\"*, *\"Exportar SAF-T AGT\"*, *\"Previsão de Stock\"* ou *\"Clientes em atraso\"*.\n\n" +
               "*(Nota: Se desejar respostas conversacionais via IA generativa em tempo real, pode adicionar uma Chave Gratuita do Google Gemini em `application.properties` - obtenha 100% grátis em [aistudio.google.com](https://aistudio.google.com))*";
    }

    private String removerAcentos(String str) {
        if (str == null) return "";
        return java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
