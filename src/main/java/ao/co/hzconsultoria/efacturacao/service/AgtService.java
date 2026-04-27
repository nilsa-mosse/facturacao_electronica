package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.dto.AgtResponse;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoAGT;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoAGTRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela comunicação com a API da AGT (Administração Geral Tributária).
 *
 * O fluxo de envio é:
 *  1. Verificar se existe uma configuração da AGT válida na base de dados.
 *  2. Construir o payload JSON com os dados da fatura e da empresa emissora.
 *  3. Efectuar o POST para o endpoint configurado, com o token de autenticação.
 *  4. Processar a resposta e devolver um AgtResponse.
 *
 * Em caso de falha de rede ou erro da API, o sistema regista o erro e devolve um
 * AgtResponse com sucesso=false, permitindo que o FaturaService marque a fatura
 * como "FALHA_ENVIO" para reenvio posterior.
 */
@Service
public class AgtService {

    private static final Logger log = LoggerFactory.getLogger(AgtService.class);

    @Autowired
    private ConfiguracaoAGTRepository agtRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Envia uma fatura emitida para a API da AGT e devolve a resposta oficial.
     *
     * @param fatura A fatura a ser enviada.
     * @return AgtResponse com o resultado do envio.
     */
    public AgtResponse enviarFatura(Fatura fatura) {
        // 1. Buscar configuração da AGT
        List<ConfiguracaoAGT> configs = agtRepository.findAll();
        if (configs.isEmpty()) {
            log.warn("Nenhuma configuração da AGT encontrada. Fatura {} não será enviada.", fatura.getNumeroFatura());
            return falha("Configuração da AGT não encontrada na base de dados.");
        }
        ConfiguracaoAGT config = configs.get(0);

        if (config.getUrlApi() == null || config.getUrlApi().trim().isEmpty()) {
            return falha("URL da API da AGT não está configurada.");
        }
        if (config.getToken() == null || config.getToken().trim().isEmpty()) {
            return falha("Token de autenticação da AGT não está configurado.");
        }

        // 2. Buscar dados da empresa emissora
        Empresa empresaEmissora = fatura.getEmpresa();
        if (empresaEmissora == null && fatura.getCompra() != null) {
            empresaEmissora = fatura.getCompra().getEmpresa();
        }
        String nifEmissor = (empresaEmissora == null || empresaEmissora.getNif() == null) ? "0000000000" : empresaEmissora.getNif();
        String nomeEmissor = (empresaEmissora == null || empresaEmissora.getNome() == null) ? "Empresa" : empresaEmissora.getNome();

        // 3. Construir o payload JSON
        Map<String, Object> payload = construirPayload(fatura, nifEmissor, nomeEmissor, config.getModo());

        // 4. Configurar cabeçalhos HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + config.getToken());
        headers.set("X-AGT-Modo", config.getModo() != null ? config.getModo() : "HOMOLOGACAO");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // 5. Efectuar chamada à API da AGT
        try {
            log.info("[AGT] Enviando fatura {} para {}", fatura.getNumeroFatura(), config.getUrlApi());

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                config.getUrlApi(),
                HttpMethod.POST,
                request,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String codigoAgt = body.containsKey("codigoAGT") ? String.valueOf(body.get("codigoAGT")) : "AGT-" + System.currentTimeMillis();
                String status    = body.containsKey("status")    ? String.valueOf(body.get("status"))    : "VALIDADA";
                String mensagem  = body.containsKey("mensagem")  ? String.valueOf(body.get("mensagem"))  : "Fatura enviada com sucesso.";

                log.info("[AGT] Fatura {} aceite com código: {}", fatura.getNumeroFatura(), codigoAgt);
                AgtResponse agtResp = new AgtResponse(true, codigoAgt, status, mensagem);
                if (body.containsKey("hash")) {
                    agtResp.setHashResposta(String.valueOf(body.get("hash")));
                }
                return agtResp;
            } else {
                log.warn("[AGT] Resposta inesperada para fatura {}: HTTP {}", fatura.getNumeroFatura(), response.getStatusCode());
                return falha("Resposta inesperada da AGT: HTTP " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            log.error("[AGT] Erro de cliente (4xx) ao enviar fatura {}: {}", fatura.getNumeroFatura(), e.getResponseBodyAsString());
            return falha("Erro de autenticação ou dados inválidos: " + e.getStatusCode());
        } catch (HttpServerErrorException e) {
            log.error("[AGT] Erro no servidor da AGT (5xx) para fatura {}: {}", fatura.getNumeroFatura(), e.getMessage());
            return falha("Servidor da AGT indisponível: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("[AGT] Timeout ou falha de rede ao enviar fatura {}: {}", fatura.getNumeroFatura(), e.getMessage());
            return falha("Falha de rede ao contactar a AGT. Verifique a conectividade.");
        } catch (Exception e) {
            log.error("[AGT] Erro inesperado ao enviar fatura {}: {}", fatura.getNumeroFatura(), e.getMessage());
            return falha("Erro inesperado: " + e.getMessage());
        }
    }

    /**
     * Testa a conexão (Ping) com a API da AGT devolvendo o resultado, código HTTP e tempo em ms.
     *
     * @param urlApi A URL da API a testar.
     * @param token O token de autenticação a utilizar.
     * @return Map contendo detalhes do teste de conexão.
     */
    public Map<String, Object> pingAgt(String urlApi, String token) {
        Map<String, Object> resultado = new HashMap<>();
        long inicio = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);
            headers.set("Accept", "application/json");

            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            // Geralmente nas APIs de Faturação, um GET para a raíz pode dar 404 Not Found, 
            // mas valida a presença do endpoint e o alcance DNS/TCP.
            ResponseEntity<String> response = restTemplate.exchange(
                    urlApi,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            resultado.put("codigoHttp", response.getStatusCodeValue());
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);

            // Considera OK tudo no nível 200, mas certas APIs podem dar 400+ pelo endpoint específico GET não existir.
            // Contudo, se retornar 401/403 sabemos que o token está rejeitado.
            if (response.getStatusCode().is2xxSuccessful()) {
                resultado.put("sucesso", true);
                resultado.put("mensagem", "Conexão estabelecida com sucesso! (HTTP " + response.getStatusCodeValue() + ")");
            } else {
                resultado.put("sucesso", false);
                resultado.put("mensagem", "A API respondeu, mas devolveu erro HTTP " + response.getStatusCodeValue());
            }

        } catch (HttpClientErrorException e) {
            resultado.put("codigoHttp", e.getRawStatusCode());
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("sucesso", false);
            if (e.getRawStatusCode() == 401 || e.getRawStatusCode() == 403) {
                resultado.put("mensagem", "Falha de Autenticação. Verifique o seu Token (HTTP " + e.getRawStatusCode() + ")");
            } else if (e.getRawStatusCode() == 404){
               // Often AGT APIs don't have GET on root, so a 404 with network success might still imply reachable API.
               resultado.put("sucesso", true);
               resultado.put("mensagem", "Servidor alcançado. Rota GET /Faturas indisponível (HTTP 404), mas a conexão com o servidor foi bem sucedida.");
            } else {
                resultado.put("mensagem", "Erro de cliente ao tentar contactar a API: HTTP " + e.getRawStatusCode());
            }
        } catch (HttpServerErrorException e) {
            resultado.put("codigoHttp", e.getRawStatusCode());
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("sucesso", false);
            resultado.put("mensagem", "Erro interno no servidor da AGT: HTTP " + e.getRawStatusCode());
        } catch (ResourceAccessException e) {
            resultado.put("sucesso", false);
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("mensagem", "Falha de rede ou Tempo de Conexão (Timeout) excedido.");
        } catch (Exception e) {
            resultado.put("sucesso", false);
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("mensagem", "Erro genérico: " + e.getMessage());
        }

        return resultado;
    }

    /**
     * Constrói o payload JSON no formato esperado pela AGT angolana.
     * Adaptar os campos conforme a especificação técnica oficial da AGT.
     */
    private Map<String, Object> construirPayload(Fatura fatura, String nifEmissor, String nomeEmissor, String modo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroFatura", fatura.getNumeroFatura());
        payload.put("dataEmissao",  fatura.getDataEmissao() != null
            ? new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(fatura.getDataEmissao())
            : "");
        payload.put("hash",         fatura.getHash());
        payload.put("totalSemIva",  fatura.getTotal() != null ? fatura.getTotal() - (fatura.getIva() != null ? fatura.getIva() : 0) : 0);
        payload.put("totalIva",     fatura.getIva()   != null ? fatura.getIva()   : 0);
        payload.put("totalGeral",   fatura.getTotal() != null ? fatura.getTotal() : 0);
        payload.put("moeda",        "AOA");
        payload.put("modo",         modo != null ? modo : "HOMOLOGACAO");

        // Emissor
        Map<String, String> emissor = new HashMap<>();
        emissor.put("nif",  nifEmissor);
        emissor.put("nome", nomeEmissor);
        payload.put("emissor", emissor);

        // Linhas de itens
        if (fatura.getCompra() != null && fatura.getCompra().getItens() != null) {
            List<Map<String, Object>> linhas = fatura.getCompra().getItens().stream().map(item -> {
                Map<String, Object> linha = new HashMap<>();
                linha.put("descricao",  item.getNomeProduto());
                linha.put("quantidade", item.getQuantidade());
                linha.put("precoUnit",  item.getPreco());
                linha.put("subtotal",   item.getSubtotal());
                return linha;
            }).collect(Collectors.toList());
            payload.put("linhas", linhas);
        }

        return payload;
    }

    private AgtResponse falha(String mensagem) {
        return new AgtResponse(false, null, "FALHA_ENVIO", mensagem);
    }
}
