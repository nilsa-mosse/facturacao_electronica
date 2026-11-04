package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço de integração com a Facebook Graph API.
 * Gere a publicação automática e manual de produtos na página de Facebook da empresa.
 *
 * Funcionalidades:
 *  - Publicação de produtos (com e sem imagem)
 *  - Publicação de promoções destacadas
 *  - Publicação do catálogo completo ou seleccionado
 *  - Verificação do token e da página
 *  - Contador de publicações diárias (anti-spam)
 */
@Service
public class FacebookService {

    private static final Logger log = LoggerFactory.getLogger(FacebookService.class);
    private static final String GRAPH_API_URL = "https://graph.facebook.com/v19.0";

    @Autowired
    private ConfiguracaoFacebookRepository fbConfigRepo;

    @Autowired
    private PublicacaoFacebookRepository publicacaoRepo;

    @Autowired
    private ProdutoRepository produtoRepo;

    @Autowired
    private EmpresaRepository empresaRepo;

    // ═══════════════════════════════════════════════════════════════════════════
    //  CONFIGURAÇÃO
    // ═══════════════════════════════════════════════════════════════════════════

    /** Obtém ou cria a configuração Facebook para a empresa indicada */
    @Transactional
    public ConfiguracaoFacebook obterOuCriarConfig(Long empresaId) {
        Optional<ConfiguracaoFacebook> opt = fbConfigRepo.findByEmpresaId(empresaId);
        if (opt.isPresent()) {
            return opt.get();
        }
        Empresa empresa = empresaRepo.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + empresaId));
        ConfiguracaoFacebook config = new ConfiguracaoFacebook();
        config.setEmpresa(empresa);
        return fbConfigRepo.save(config);
    }

    @Transactional
    public ConfiguracaoFacebook salvarConfig(ConfiguracaoFacebook config) {
        return fbConfigRepo.save(config);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLICAÇÃO PRINCIPAL
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Publica um produto específico na página Facebook da empresa.
     * Utiliza o template configurado e adiciona as hashtags definidas.
     *
     * @param produto      Produto a publicar
     * @param empresaId    ID da empresa
     * @param manual       Se foi iniciado manualmente pelo utilizador
     * @return resultado da publicação com status e ID do post
     */
    @Transactional
    public ResultadoPublicacao publicarProduto(Produto produto, Long empresaId, boolean manual) {
        ConfiguracaoFacebook config = obterOuCriarConfig(empresaId);

        if (!config.isAtivo()) {
            return ResultadoPublicacao.falha("A integração com Facebook não está activa. Configure o token na secção de Marketing > Facebook.");
        }
        if (isTokenVazio(config)) {
            return ResultadoPublicacao.falha("Page Access Token não configurado. Obtenha o token no Facebook Developers.");
        }
        if (isPageIdVazio(config)) {
            return ResultadoPublicacao.falha("ID da página Facebook não configurado.");
        }

        // Verificar limite diário
        if (!manual && !podePublicarHoje(config, empresaId)) {
            return ResultadoPublicacao.falha("Limite diário de publicações atingido (" + config.getMaxPublicacoesPorDia() + " publicações/dia).");
        }

        // Construir mensagem
        String mensagem = construirMensagemProduto(produto, config);

        // Tentar publicar com imagem primeiro, depois sem imagem como fallback
        ResultadoPublicacao resultado;
        if (produto.getImagemBlob() != null && produto.getImagemBlob().length > 0) {
            resultado = publicarComImagemBlob(config, mensagem, produto.getImagemBlob());
        } else if (produto.getImagem() != null && !produto.getImagem().isEmpty()) {
            resultado = publicarComImagemUrl(config, mensagem, produto.getImagem());
        } else {
            resultado = publicarTexto(config, mensagem);
        }

        // Registar no histórico
        registarPublicacao(produto, empresaId, "PRODUTO", mensagem, resultado, manual, null);

        // Actualizar contadores da configuração
        actualizarContadores(config, resultado.isSucesso());

        return resultado;
    }

    /**
     * Publica uma promoção com destaque especial na página Facebook.
     */
    @Transactional
    public ResultadoPublicacao publicarPromocao(Produto produto, Long empresaId, boolean manual) {
        ConfiguracaoFacebook config = obterOuCriarConfig(empresaId);

        if (!config.isAtivo() || isTokenVazio(config) || isPageIdVazio(config)) {
            return ResultadoPublicacao.falha("Facebook não configurado ou inactivo.");
        }
        if (!manual && !podePublicarHoje(config, empresaId)) {
            return ResultadoPublicacao.falha("Limite diário de publicações atingido.");
        }

        String mensagem = construirMensagemPromocao(produto, config);

        ResultadoPublicacao resultado;
        if (produto.getImagemBlob() != null && produto.getImagemBlob().length > 0) {
            resultado = publicarComImagemBlob(config, mensagem, produto.getImagemBlob());
        } else {
            resultado = publicarTexto(config, mensagem);
        }

        registarPublicacao(produto, empresaId, "PROMOCAO", mensagem, resultado, manual, null);
        actualizarContadores(config, resultado.isSucesso());

        return resultado;
    }

    /**
     * Publica uma mensagem personalizada simples (sem mídia).
     */
    @Transactional
    public ResultadoPublicacao publicarMensagemPersonalizada(String mensagem, Long empresaId) {
        return publicarMensagemPersonalizada(mensagem, empresaId, null, null, null, null);
    }

    /**
     * Publica uma mensagem personalizada com suporte a imagem ou vídeo (upload ou URL).
     */
    @Transactional
    public ResultadoPublicacao publicarMensagemPersonalizada(String mensagem, Long empresaId,
                                                             byte[] mediaBytes, String mediaType,
                                                             String mediaNome, String mediaUrl) {
        ConfiguracaoFacebook config = obterOuCriarConfig(empresaId);

        if (!config.isAtivo() || isTokenVazio(config) || isPageIdVazio(config)) {
            return ResultadoPublicacao.falha("Facebook não configurado ou inactivo.");
        }

        ResultadoPublicacao resultado;
        boolean hasBytes = (mediaBytes != null && mediaBytes.length > 0);
        boolean hasUrl = (mediaUrl != null && !mediaUrl.trim().isEmpty());
        boolean isVideo = (mediaType != null && mediaType.toLowerCase().startsWith("video/"))
                || (mediaNome != null && (mediaNome.toLowerCase().endsWith(".mp4") || mediaNome.toLowerCase().endsWith(".mov")))
                || (hasUrl && (mediaUrl.toLowerCase().contains(".mp4") || mediaUrl.toLowerCase().contains(".mov")));

        if (isVideo) {
            if (hasBytes) {
                resultado = publicarVideoBlob(config, mensagem, mediaBytes, mediaNome);
            } else if (hasUrl) {
                resultado = publicarVideoUrl(config, mensagem, mediaUrl);
            } else {
                resultado = publicarTexto(config, mensagem);
            }
        } else if (hasBytes) {
            resultado = publicarComImagemBlob(config, mensagem, mediaBytes);
        } else if (hasUrl) {
            resultado = publicarComImagemUrl(config, mensagem, mediaUrl);
        } else {
            resultado = publicarTexto(config, mensagem);
        }

        String mediaRef = hasUrl ? mediaUrl : (hasBytes ? (isVideo ? "video-upload" : "imagem-upload") : null);
        String tipoPub = isVideo ? "VIDEO" : (hasBytes || hasUrl ? "IMAGEM" : "MANUAL");
        registarPublicacao(null, empresaId, tipoPub, mensagem, resultado, true, mediaRef);
        actualizarContadores(config, resultado.isSucesso());

        return resultado;
    }

    /**
     * Publica automaticamente todos os produtos em promoção.
     * Chamado pelo scheduler agendado.
     */
    @Transactional
    public List<ResultadoPublicacao> publicarPromocoesAutomaticas(Long empresaId) {
        ConfiguracaoFacebook config = obterOuCriarConfig(empresaId);
        List<ResultadoPublicacao> resultados = new ArrayList<>();

        if (!config.isAtivo() || !config.isPublicarPromocoesAuto()) {
            return resultados;
        }

        List<Produto> produtos = produtoRepo.findByEmpresa_Id(empresaId);
        for (Produto p : produtos) {
            if (!p.isEmPromocao()) continue;
            if (!podePublicarHoje(config, empresaId)) break;

            ResultadoPublicacao r = publicarPromocao(p, empresaId, false);
            resultados.add(r);

            // Pausa de 3 segundos entre publicações para respeitar os rate limits da API
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        return resultados;
    }

    /**
     * Publica automaticamente produtos com stock baixo para estimular vendas.
     */
    @Transactional
    public List<ResultadoPublicacao> publicarStockBaixoAutomatico(Long empresaId) {
        ConfiguracaoFacebook config = obterOuCriarConfig(empresaId);
        List<ResultadoPublicacao> resultados = new ArrayList<>();

        if (!config.isAtivo() || !config.isPublicarStockBaixo()) {
            return resultados;
        }

        List<Produto> produtos = produtoRepo.findProdutosComStockBaixo(empresaId);
        int publicados = 0;
        for (Produto p : produtos) {
            if (publicados >= 2) break; // Máximo 2 publicações de stock baixo por ciclo
            if (!podePublicarHoje(config, empresaId)) break;

            String mensagem = "🚨 Últimas unidades disponíveis!\n\n🛍️ " + p.getNome() +
                    "\n📦 Apenas " + (p.getQuantidadeEstoque() != null ? p.getQuantidadeEstoque().intValue() : 0) + " unidades em stock!\n" +
                    "💰 Preço: " + String.format("%.2f", p.getPreco()) + " Kz\n\n" +
                    "📞 Não perca! Contacte-nos agora!\n" + config.getHashtags();

            ResultadoPublicacao r = publicarTexto(config, mensagem);
            registarPublicacao(p, empresaId, "STOCK_BAIXO", mensagem, r, false, null);
            actualizarContadores(config, r.isSucesso());
            resultados.add(r);
            publicados++;

            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

        return resultados;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CHAMADAS PARA A FACEBOOK GRAPH API
    // ═══════════════════════════════════════════════════════════════════════════

    /** Publica apenas texto na página */
    private ResultadoPublicacao publicarTexto(ConfiguracaoFacebook config, String mensagem) {
        try {
            String endpoint = GRAPH_API_URL + "/" + config.getPageId() + "/feed";
            String params = "message=" + URLEncoder.encode(mensagem, "UTF-8") +
                    "&access_token=" + URLEncoder.encode(config.getPageAccessToken(), "UTF-8");

            String resposta = fazerPostHttp(endpoint, params);
            return processarResposta(resposta);

        } catch (Exception e) {
            log.error("Erro ao publicar texto no Facebook: {}", e.getMessage());
            return ResultadoPublicacao.falha("Erro de comunicação: " + e.getMessage());
        }
    }

    /** Publica com imagem via URL */
    private ResultadoPublicacao publicarComImagemUrl(ConfiguracaoFacebook config, String mensagem, String urlImagem) {
        try {
            String endpoint = GRAPH_API_URL + "/" + config.getPageId() + "/photos";
            String params = "message=" + URLEncoder.encode(mensagem, "UTF-8") +
                    "&url=" + URLEncoder.encode(urlImagem, "UTF-8") +
                    "&access_token=" + URLEncoder.encode(config.getPageAccessToken(), "UTF-8");

            String resposta = fazerPostHttp(endpoint, params);
            return processarResposta(resposta);

        } catch (Exception e) {
            log.warn("Erro ao publicar com URL de imagem, fazendo fallback para texto: {}", e.getMessage());
            return publicarTexto(config, mensagem);
        }
    }

    /** Publica com imagem em bytes (blob) - faz upload directamente */
    private ResultadoPublicacao publicarComImagemBlob(ConfiguracaoFacebook config, String mensagem, byte[] imagemBytes) {
        try {
            String endpoint = GRAPH_API_URL + "/" + config.getPageId() + "/photos";
            String boundary = "----FacebookUpload" + System.currentTimeMillis();

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream os = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                // Campo: access_token
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"access_token\"").append("\r\n\r\n");
                writer.append(config.getPageAccessToken()).append("\r\n").flush();

                // Campo: message
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"message\"").append("\r\n\r\n");
                writer.append(mensagem).append("\r\n").flush();

                // Campo: source (imagem binária)
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"source\"; filename=\"produto.jpg\"").append("\r\n");
                writer.append("Content-Type: image/jpeg").append("\r\n\r\n");
                writer.flush();
                os.write(imagemBytes);
                os.flush();
                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            String resposta = lerResposta(conn, responseCode);
            return processarResposta(resposta);

        } catch (Exception e) {
            log.warn("Erro ao publicar com imagem blob, fazendo fallback para texto: {}", e.getMessage());
            return publicarTexto(config, mensagem);
        }
    }

    /** Publica vídeo via URL */
    private ResultadoPublicacao publicarVideoUrl(ConfiguracaoFacebook config, String descricao, String videoUrl) {
        try {
            String endpoint = "https://graph-video.facebook.com/v19.0/" + config.getPageId() + "/videos";
            String params = "description=" + URLEncoder.encode(descricao, "UTF-8") +
                    "&file_url=" + URLEncoder.encode(videoUrl, "UTF-8") +
                    "&access_token=" + URLEncoder.encode(config.getPageAccessToken(), "UTF-8");

            String resposta = fazerPostHttp(endpoint, params);
            return processarResposta(resposta);

        } catch (Exception e) {
            log.warn("Erro ao publicar vídeo por URL, tentando endpoint padrão: {}", e.getMessage());
            try {
                String endpointFallback = GRAPH_API_URL + "/" + config.getPageId() + "/videos";
                String params = "description=" + URLEncoder.encode(descricao, "UTF-8") +
                        "&file_url=" + URLEncoder.encode(videoUrl, "UTF-8") +
                        "&access_token=" + URLEncoder.encode(config.getPageAccessToken(), "UTF-8");
                String resposta = fazerPostHttp(endpointFallback, params);
                return processarResposta(resposta);
            } catch (Exception ex) {
                return ResultadoPublicacao.falha("Erro ao publicar vídeo no Facebook: " + ex.getMessage());
            }
        }
    }

    /** Publica vídeo em bytes (upload directo) */
    private ResultadoPublicacao publicarVideoBlob(ConfiguracaoFacebook config, String descricao, byte[] videoBytes, String nomeArquivo) {
        try {
            String endpoint = "https://graph-video.facebook.com/v19.0/" + config.getPageId() + "/videos";
            String boundary = "----FacebookVideoUpload" + System.currentTimeMillis();

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setConnectTimeout(60000);
            conn.setReadTimeout(180000); // 3 minutos para vídeos maiores

            try (OutputStream os = conn.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true)) {

                // Campo: access_token
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"access_token\"").append("\r\n\r\n");
                writer.append(config.getPageAccessToken()).append("\r\n").flush();

                // Campo: description
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"description\"").append("\r\n\r\n");
                writer.append(descricao).append("\r\n").flush();

                // Campo: source (vídeo binário)
                String filename = (nomeArquivo != null && !nomeArquivo.isEmpty()) ? nomeArquivo : "video.mp4";
                String contentType = (filename.toLowerCase().endsWith(".mov")) ? "video/quicktime" : "video/mp4";

                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"source\"; filename=\"").append(filename).append("\"").append("\r\n");
                writer.append("Content-Type: ").append(contentType).append("\r\n\r\n");
                writer.flush();
                os.write(videoBytes);
                os.flush();
                writer.append("\r\n").flush();
                writer.append("--").append(boundary).append("--").append("\r\n");
                writer.flush();
            }

            int responseCode = conn.getResponseCode();
            String resposta = lerResposta(conn, responseCode);
            return processarResposta(resposta);

        } catch (Exception e) {
            log.warn("Erro ao publicar vídeo blob no Facebook: {}", e.getMessage());
            return ResultadoPublicacao.falha("Erro ao carregar vídeo para o Facebook: " + e.getMessage());
        }
    }

    /**
     * Verifica se o token e a página estão válidos, chamando a API Graph.
     * Utilizado no ecrã de configuração para validar antes de guardar.
     */
    public Map<String, Object> verificarConexao(String pageAccessToken, String pageId) {
        Map<String, Object> resultado = new HashMap<>();
        try {
            String endpoint = GRAPH_API_URL + "/" + pageId + "?fields=name,fan_count,followers_count&access_token="
                    + URLEncoder.encode(pageAccessToken, "UTF-8");

            String resposta = fazerGetHttp(endpoint);

            if (resposta.contains("\"name\"")) {
                // Extrair nome da página de forma simples
                String nome = extrairValorJson(resposta, "name");
                String fanCount = extrairValorJson(resposta, "fan_count");

                resultado.put("sucesso", true);
                resultado.put("pageName", nome);
                resultado.put("fanCount", fanCount);
                resultado.put("mensagem", "Conexão estabelecida com sucesso! Página: " + nome);
            } else if (resposta.contains("\"error\"")) {
                String errorMsg = extrairValorJson(resposta, "message");
                resultado.put("sucesso", false);
                resultado.put("mensagem", "Erro da API: " + errorMsg);
            } else {
                resultado.put("sucesso", false);
                resultado.put("mensagem", "Resposta inesperada da API do Facebook.");
            }
        } catch (Exception e) {
            resultado.put("sucesso", false);
            resultado.put("mensagem", "Erro de rede: " + e.getMessage());
        }
        return resultado;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILITÁRIOS HTTP
    // ═══════════════════════════════════════════════════════════════════════════

    private String fazerPostHttp(String endpoint, String params) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(params.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        return lerResposta(conn, responseCode);
    }

    private String fazerGetHttp(String endpoint) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        int responseCode = conn.getResponseCode();
        return lerResposta(conn, responseCode);
    }

    private String lerResposta(HttpURLConnection conn, int responseCode) throws IOException {
        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (is == null) return "{\"error\":\"Sem resposta do servidor\"}";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CONSTRUÇÃO DE MENSAGENS
    // ═══════════════════════════════════════════════════════════════════════════

    private String construirMensagemProduto(Produto produto, ConfiguracaoFacebook config) {
        String template = config.getTemplateMensagemProduto();
        if (template == null || template.isEmpty()) {
            template = "✨ PRODUTO DISPONÍVEL NO SISTEMA KwanzaERP! ✨\n\n🛍️ Artigo: {nome}\n📂 Categoria: {categoria}\n💰 Preço: Sob Consulta\n📦 Stock: {stock} unidades\n\nℹ️ Detalhes do Produto:\n{descricao}\n\n📍 Entregas rápidas em Luanda e envio para todas as províncias!\n📲 Contacte-nos já (WhatsApp e Chamada Normal Unitel):\n📞 934 068 121";
        }

        String preco = String.format("%.2f", produto.getPreco());
        String descricao = (produto.getDescricao() != null && !produto.getDescricao().isEmpty())
                ? produto.getDescricao() : "";

        String mensagem = template
                .replace("{nome}", produto.getNome())
                .replace("{preco}", preco)
                .replace("{descricao}", descricao)
                .replace("{categoria}", produto.getCategoria() != null ? produto.getCategoria().getNome() : "")
                .replace("{stock}", produto.getQuantidadeEstoque() != null ? String.valueOf(produto.getQuantidadeEstoque().intValue()) : "");

        // Adicionar hashtags
        if (config.getHashtags() != null && !config.getHashtags().isEmpty()) {
            mensagem = mensagem + "\n\n" + config.getHashtags();
        }

        return mensagem;
    }

    private String construirMensagemPromocao(Produto produto, ConfiguracaoFacebook config) {
        String template = config.getTemplateMensagemPromocao();
        if (template == null || template.isEmpty()) {
            template = "🔥 OPORTUNIDADE ESPECIAL NO KwanzaERP! 🔥\n\n🛍️ Artigo: {nome}\n💥 Condições promocionais exclusivas!\n💰 Preço: Sob Consulta\n\nℹ️ Informações:\n{descricao}\n\n⏰ Oferta válida até durar o stock existente!\n📲 Garanta já as suas condições especiais (WhatsApp e Chamada Normal Unitel):\n📞 934 068 121";
        }

        String preco = String.format("%.2f", produto.getPreco());
        String precoOriginal = (produto.getPrecoOriginal() != null)
                ? String.format("%.2f", produto.getPrecoOriginal()) : preco;
        String descricao = (produto.getDescricao() != null) ? produto.getDescricao() : "";

        String mensagem = template
                .replace("{nome}", produto.getNome())
                .replace("{preco}", preco)
                .replace("{preco_original}", precoOriginal)
                .replace("{descricao}", descricao);

        if (config.getHashtags() != null && !config.getHashtags().isEmpty()) {
            mensagem = mensagem + "\n\n" + config.getHashtags();
        }

        return mensagem;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PROCESSAMENTO DE RESPOSTAS E REGISTO
    // ═══════════════════════════════════════════════════════════════════════════

    private ResultadoPublicacao processarResposta(String resposta) {
        if (resposta == null) return ResultadoPublicacao.falha("Sem resposta da API");

        if (resposta.contains("\"id\"") && !resposta.contains("\"error\"")) {
            String postId = extrairValorJson(resposta, "id");
            return ResultadoPublicacao.sucesso(postId, "Publicado com sucesso! ID do post: " + postId);
        } else if (resposta.contains("\"error\"")) {
            String errorMsg = extrairValorJson(resposta, "message");
            if (errorMsg != null && (errorMsg.contains("pages_manage_posts") || errorMsg.contains("publish_to_groups") || resposta.contains("\"code\":200") || resposta.contains("\"code\": 200"))) {
                errorMsg = "Permissão negada pelo Facebook (#200): O token configurado é um Token Pessoal de Utilizador e não tem autorização para publicar na página. É necessário configurar um 'Page Access Token' da página com privilégios de Administrador.";
            }
            return ResultadoPublicacao.falha("Erro da API: " + (errorMsg != null ? errorMsg : resposta));
        }

        return ResultadoPublicacao.falha("Resposta inesperada: " + resposta);
    }

    @Transactional
    public void registarPublicacao(Produto produto, Long empresaId, String tipo,
                                    String mensagem, ResultadoPublicacao resultado,
                                    boolean manual, String urlImagem) {
        try {
            PublicacaoFacebook pub = new PublicacaoFacebook();
            pub.setEmpresa(empresaRepo.findById(empresaId).orElse(null));
            pub.setProduto(produto);
            pub.setTipoPublicacao(tipo);
            pub.setMensagem(mensagem);
            pub.setStatus(resultado.isSucesso() ? "SUCESSO" : "FALHA");
            pub.setFacebookPostId(resultado.getPostId());
            pub.setRespostaApi(resultado.getMensagem());
            pub.setDataPublicacao(LocalDateTime.now());
            pub.setPublicacaoManual(manual);
            pub.setUrlImagem(urlImagem);
            publicacaoRepo.save(pub);
        } catch (Exception e) {
            log.error("Erro ao registar publicação no histórico: {}", e.getMessage());
        }
    }

    @Transactional
    public void actualizarContadores(ConfiguracaoFacebook config, boolean sucesso) {
        try {
            config.setDataUltimaPublicacao(LocalDateTime.now());
            if (sucesso) {
                config.setTotalPublicacoesSucesso(config.getTotalPublicacoesSucesso() + 1);
                config.setPublicacoesHoje(config.getPublicacoesHoje() + 1);
            } else {
                config.setTotalPublicacoesFalha(config.getTotalPublicacoesFalha() + 1);
            }
            fbConfigRepo.save(config);
        } catch (Exception e) {
            log.error("Erro ao actualizar contadores: {}", e.getMessage());
        }
    }

    private boolean podePublicarHoje(ConfiguracaoFacebook config, Long empresaId) {
        // Reset do contador se for um novo dia
        if (config.getDataResetContador() == null ||
                config.getDataResetContador().toLocalDate().isBefore(LocalDate.now())) {
            config.setPublicacoesHoje(0);
            config.setDataResetContador(LocalDateTime.now());
            fbConfigRepo.save(config);
        }
        return config.getPublicacoesHoje() < config.getMaxPublicacoesPorDia();
    }

    private boolean isTokenVazio(ConfiguracaoFacebook config) {
        return config.getPageAccessToken() == null || config.getPageAccessToken().trim().isEmpty();
    }

    private boolean isPageIdVazio(ConfiguracaoFacebook config) {
        return config.getPageId() == null || config.getPageId().trim().isEmpty();
    }

    /** Extracção simples de valor de JSON sem dependência de biblioteca externa */
    private String extrairValorJson(String json, String campo) {
        String chave = "\"" + campo + "\":";
        int idx = json.indexOf(chave);
        if (idx < 0) return null;
        int inicio = idx + chave.length();
        if (inicio >= json.length()) return null;

        char primeiro = json.charAt(inicio);
        if (primeiro == '"') {
            // Valor é string
            int fim = json.indexOf('"', inicio + 1);
            if (fim < 0) return null;
            return json.substring(inicio + 1, fim);
        } else {
            // Valor numérico ou boolean
            int fim = json.indexOf(',', inicio);
            if (fim < 0) fim = json.indexOf('}', inicio);
            if (fim < 0) return null;
            return json.substring(inicio, fim).trim();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CLASSE INTERNA: RESULTADO DA PUBLICAÇÃO
    // ═══════════════════════════════════════════════════════════════════════════

    public static class ResultadoPublicacao {
        private final boolean sucesso;
        private final String postId;
        private final String mensagem;

        private ResultadoPublicacao(boolean sucesso, String postId, String mensagem) {
            this.sucesso = sucesso;
            this.postId = postId;
            this.mensagem = mensagem;
        }

        public static ResultadoPublicacao sucesso(String postId, String mensagem) {
            return new ResultadoPublicacao(true, postId, mensagem);
        }

        public static ResultadoPublicacao falha(String mensagem) {
            return new ResultadoPublicacao(false, null, mensagem);
        }

        public boolean isSucesso() { return sucesso; }
        public String getPostId() { return postId; }
        public String getMensagem() { return mensagem; }
    }
}
