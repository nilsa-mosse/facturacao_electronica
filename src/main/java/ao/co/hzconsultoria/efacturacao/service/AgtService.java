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
 * Serviço responsável pela comunicação com a API da AGT (Administração Geral
 * Tributária).
 *
 * O fluxo de envio é:
 * 1. Verificar se existe uma configuração da AGT válida na base de dados.
 * 2. Construir o payload JSON com os dados da fatura e da empresa emissora.
 * 3. Efectuar o POST para o endpoint configurado, com o token de autenticação.
 * 4. Processar a resposta e devolver um AgtResponse.
 *
 * Em caso de falha de rede ou erro da API, o sistema regista o erro e devolve
 * um
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
    private ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Gera e imprime o JSON da factura no padrão oficial da AGT (Schema v1.2) para
     * a consola/logs.
     */
    public String imprimirJsonAgt(Fatura fatura) {
        try {
            Empresa empresaEmissora = fatura.getEmpresa();
            if (empresaEmissora == null && fatura.getCompra() != null) {
                empresaEmissora = fatura.getCompra().getEmpresa();
            }
            String nifEmissor = (empresaEmissora == null || empresaEmissora.getNif() == null) ? "999999999"
                    : empresaEmissora.getNif();

            ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity configSistema = configuracaoSistemaRepository
                    .findById(1L).orElse(new ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity());

            Map<String, Object> payload = construirPayloadOficialAgt(fatura, nifEmissor, configSistema);
            String jsonPretty = toJsonPretty(payload);

            System.out.println("\n==================== [AGT PAYLOAD JSON GERADO (DOCUMENTO " + fatura.getNumeroFatura()
                    + ")] ====================");
            System.out.println(jsonPretty);
            System.out.println(
                    "========================================================================================\n");
            log.info("[AGT Registo] JSON gerado para a fatura {}:\n{}", fatura.getNumeroFatura(), jsonPretty);

            return jsonPretty;
        } catch (Exception e) {
            log.error("Erro ao gerar JSON AGT para fatura {}: {}", fatura.getNumeroFatura(), e.getMessage());
            return "{}";
        }
    }

    /**
     * Envia uma fatura emitida para a API da AGT de acordo com as especificações
     * oficiais
     * (Serviço de Registo de Faturas Eletrónicas - Schema v1.2).
     *
     * @param fatura A fatura a ser enviada.
     * @return AgtResponse com o resultado da submissão.
     */
    public AgtResponse enviarFatura(Fatura fatura) {
        // Imprimir sempre o JSON para inspeção nos logs antes da submissão
        imprimirJsonAgt(fatura);

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

        // 2. Buscar dados da empresa emissora e configuração de sistema (Chaves RSA
        // 2048 bits / Certificado)
        Empresa empresaEmissora = fatura.getEmpresa();
        if (empresaEmissora == null && fatura.getCompra() != null) {
            empresaEmissora = fatura.getCompra().getEmpresa();
        }
        String nifEmissor = (empresaEmissora == null || empresaEmissora.getNif() == null) ? "999999999"
                : empresaEmissora.getNif();

        ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity configSistema = configuracaoSistemaRepository
                .findById(1L).orElse(new ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity());

        // 3. Construir o payload JSON oficial de registo AGT (Schema v1.2)
        Map<String, Object> payload = construirPayloadOficialAgt(fatura, nifEmissor, configSistema);

        // 4. Configurar cabeçalhos HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getToken() != null && !config.getToken().trim().isEmpty()) {
            headers.set("Authorization", "Bearer " + config.getToken());
        }
        headers.set("X-AGT-Modo", config.getModo() != null ? config.getModo() : "HOMOLOGACAO");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // 5. Efectuar chamada à API da AGT
        try {
            String jsonPretty = toJsonPretty(payload);
            log.info("[AGT Registo] Enviando fatura {} para {}", fatura.getNumeroFatura(), config.getUrlApi());
            log.info("[AGT Registo] PAYLOAD JSON A ENVIAR:\n{}", jsonPretty);
            System.out.println("==================== [AGT PAYLOAD JSON A ENVIAR] ====================");
            System.out.println(jsonPretty);
            System.out.println("====================================================================");

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    config.getUrlApi(),
                    HttpMethod.POST,
                    request,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                String requestId = body.containsKey("requestID") ? String.valueOf(body.get("requestID"))
                        : "REQ-" + System.currentTimeMillis();

                List<?> errorList = body.containsKey("errorList") ? (List<?>) body.get("errorList") : null;

                if (errorList != null && !errorList.isEmpty()) {
                    String msgErro = errorList.stream()
                            .map(Object::toString)
                            .collect(Collectors.joining("; "));
                    log.warn("[AGT Registo] Fatura {} registada com erros: {}", fatura.getNumeroFatura(), msgErro);
                    return falha("Rejeitado pela AGT: " + msgErro);
                }

                log.info("[AGT Registo] Fatura {} registada com sucesso. RequestID: {}", fatura.getNumeroFatura(),
                        requestId);
                AgtResponse agtResp = new AgtResponse(true, requestId, "VALIDADA_AGT",
                        "Factura electrónica registada com sucesso na AGT. RequestID: " + requestId);
                return agtResp;
            } else {
                log.warn("[AGT Registo] Resposta inesperada para fatura {}: HTTP {}", fatura.getNumeroFatura(),
                        response.getStatusCode());
                return falha("Resposta inesperada da AGT: HTTP " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            log.error("[AGT Registo] Erro de cliente (4xx) ao enviar fatura {}: {}", fatura.getNumeroFatura(),
                    e.getResponseBodyAsString());
            return falha("Erro de validação AGT (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            log.error("[AGT Registo] Erro no servidor da AGT (5xx) para fatura {}: {}", fatura.getNumeroFatura(),
                    e.getMessage());
            return falha("Servidor da AGT indisponível: HTTP " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("[AGT Registo] Timeout ou falha de rede ao enviar fatura {}: {}", fatura.getNumeroFatura(),
                    e.getMessage());
            return falha("Falha de rede ao contactar a AGT. Verifique a conectividade.");
        } catch (Exception e) {
            log.error("[AGT Registo] Erro inesperado ao enviar fatura {}: {}", fatura.getNumeroFatura(),
                    e.getMessage());
            return falha("Erro inesperado no registo AGT: " + e.getMessage());
        }
    }

    /**
     * Testa a conexão (Ping) com a API da AGT.
     */
    public Map<String, Object> pingAgt(String urlApi, String token) {
        Map<String, Object> resultado = new HashMap<>();
        long inicio = System.currentTimeMillis();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (token != null && !token.trim().isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }
            headers.set("Accept", "application/json");

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    urlApi,
                    HttpMethod.GET,
                    request,
                    String.class);

            resultado.put("codigoHttp", response.getStatusCodeValue());
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);

            if (response.getStatusCode().is2xxSuccessful()) {
                resultado.put("sucesso", true);
                resultado.put("mensagem",
                        "Conexão estabelecida com sucesso! (HTTP " + response.getStatusCodeValue() + ")");
            } else {
                resultado.put("sucesso", false);
                resultado.put("mensagem", "A API respondeu com código HTTP " + response.getStatusCodeValue());
            }

        } catch (HttpClientErrorException e) {
            resultado.put("codigoHttp", e.getRawStatusCode());
            resultado.put("tempoMs", System.currentTimeMillis() - inicio);
            resultado.put("sucesso", false);
            if (e.getRawStatusCode() == 401 || e.getRawStatusCode() == 403) {
                resultado.put("mensagem",
                        "Falha de Autenticação. Verifique o Token (HTTP " + e.getRawStatusCode() + ")");
            } else if (e.getRawStatusCode() == 404) {
                resultado.put("sucesso", true);
                resultado.put("mensagem", "Servidor AGT alcançado (HTTP 404), rota GET ativa para verificação.");
            } else {
                resultado.put("mensagem", "Erro de cliente ao contactar a API: HTTP " + e.getRawStatusCode());
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
     * Constrói o payload JSON no formato oficial exigido pelo Serviço de Registo de
     * Faturas da AGT (Schema v1.2):
     * https://portaldoparceiro.hml.minfin.gov.ao/doc-agt/faturacao-electronica/1/servicos/registar.html
     */
    private Map<String, Object> construirPayloadOficialAgt(Fatura fatura, String nifEmissor,
            ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity configSistema) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("schemaVersion", "1.2");
        payload.put("submissionUUID", java.util.UUID.randomUUID().toString());
        payload.put("taxRegistrationNumber", nifEmissor);

        java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        String nowIso = isoFormat.format(new java.util.Date());
        payload.put("submissionTimeStamp", nowIso);

        // 1. softwareInfo
        Map<String, Object> softwareInfo = new HashMap<>();
        Map<String, Object> softwareInfoDetail = new HashMap<>();
        softwareInfoDetail.put("productId",
                configSistema.getSistemaNome() != null ? configSistema.getSistemaNome() : "Kwanza ERP");
        softwareInfoDetail.put("productVersion",
                configSistema.getSistemaVersao() != null ? configSistema.getSistemaVersao() : "1.0.0");
        softwareInfoDetail.put("softwareValidationNumber",
                configSistema.getAgtCertificadoNumero() != null ? configSistema.getAgtCertificadoNumero() : "0000");

        String privateKeyPem = configSistema.getAgtPrivateKey();

        // Assinatura JWS RS256 do softwareInfoDetail
        String jsonSoftwareDetail = toJson(softwareInfoDetail);
        String jwsSoftwareSignature = ao.co.hzconsultoria.efacturacao.util.JwsUtil.gerarJwsRs256(jsonSoftwareDetail,
                privateKeyPem);

        softwareInfo.put("softwareInfoDetail", softwareInfoDetail);
        softwareInfo.put("jwsSoftwareSignature", jwsSoftwareSignature);
        payload.put("softwareInfo", softwareInfo);

        payload.put("numberOfEntries", 1);

        // 2. documents
        Map<String, Object> docObj = new HashMap<>();
        docObj.put("documentNo", fatura.getNumeroFatura());
        docObj.put("documentStatus", "N");

        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String docDateStr = fatura.getDataEmissao() != null ? dateFormat.format(fatura.getDataEmissao())
                : dateFormat.format(new java.util.Date());
        docObj.put("documentDate", docDateStr);

        String docType = fatura.getTipoDocumento() != null ? fatura.getTipoDocumento() : "FT";
        docObj.put("documentType", docType);

        String sysEntryDateStr = fatura.getSystemEntryDate() != null ? isoFormat.format(fatura.getSystemEntryDate())
                : nowIso;
        docObj.put("systemEntryDate", sysEntryDateStr);

        // Dados do cliente
        String customerNif = "999999999";
        String customerName = "Consumidor Final";
        String customerCountry = "AO";

        if (fatura.getCompra() != null) {
            if (fatura.getCompra().getNifCliente() != null && !fatura.getCompra().getNifCliente().trim().isEmpty()) {
                customerNif = fatura.getCompra().getNifCliente().trim();
            }
            if (fatura.getCompra().getNomeCliente() != null && !fatura.getCompra().getNomeCliente().trim().isEmpty()) {
                customerName = fatura.getCompra().getNomeCliente().trim();
            }
        }

        docObj.put("customerTaxID", customerNif);
        docObj.put("customerCountry", customerCountry);
        docObj.put("companyName", customerName);

        // Linhas de artigos/serviços
        List<Map<String, Object>> linesList = new java.util.ArrayList<>();
        if (fatura.getCompra() != null && fatura.getCompra().getItens() != null) {
            int lineNo = 1;
            for (ItemCompra item : fatura.getCompra().getItens()) {
                Map<String, Object> line = new HashMap<>();
                line.put("lineNumber", lineNo++);
                line.put("operationType", "TB"); // Transmissão de Bens por omissão
                line.put("productCode",
                        item.getProduto() != null && item.getProduto().getCodigoBarra() != null
                                ? item.getProduto().getCodigoBarra()
                                : "PROD" + item.getId());
                line.put("productDescription", item.getNomeProduto());
                line.put("quantity", item.getQuantidade());
                line.put("unitOfMeasure", "UN");

                double unitPrice = item.getPreco() != null ? item.getPreco() : 0.0;
                double subtotal = item.getSubtotal() != null ? item.getSubtotal() : 0.0;

                line.put("unitPriceBase", unitPrice);
                line.put("unitPrice", unitPrice);
                line.put("debitAmount", 0);
                line.put("creditAmount", subtotal);
                line.put("settlementAmount", 0);

                // Taxes na linha
                List<Map<String, Object>> taxesList = new java.util.ArrayList<>();
                Map<String, Object> taxObj = new HashMap<>();
                taxObj.put("taxType", "IVA");
                taxObj.put("taxCountryRegion", "AO");

                double ivaTax = item.getIvaPercentual() != null ? item.getIvaPercentual() : 14.0;
                double ivaVal = item.getIva() != null ? item.getIva() : 0.0;

                if (ivaTax <= 0) {
                    taxObj.put("taxCode", "ISE");
                    taxObj.put("taxPercentage", 0);
                    taxObj.put("taxContribution", 0);
                    taxObj.put("taxExemptionCode", "M00"); // Código genérico de isenção
                } else {
                    taxObj.put("taxCode", "NOR");
                    taxObj.put("taxPercentage", ivaTax);
                    taxObj.put("taxContribution", ivaVal);
                }
                taxesList.add(taxObj);
                line.put("taxes", taxesList);

                linesList.add(line);
            }
        }
        docObj.put("lines", linesList);

        // documentTotals
        Map<String, Object> docTotals = new HashMap<>();
        double totalIva = fatura.getIva() != null ? fatura.getIva() : 0.0;
        double totalGeral = fatura.getTotal() != null ? fatura.getTotal() : 0.0;
        double netTotal = totalGeral - totalIva;

        docTotals.put("taxPayable", Math.round(totalIva * 100.0) / 100.0);
        docTotals.put("netTotal", Math.round(netTotal * 100.0) / 100.0);
        docTotals.put("grossTotal", Math.round(totalGeral * 100.0) / 100.0);

        docObj.put("documentTotals", docTotals);

        // Assinatura JWS RS256 dos campos do documento
        Map<String, Object> docFieldsToSign = new HashMap<>();
        docFieldsToSign.put("documentNo", fatura.getNumeroFatura());
        docFieldsToSign.put("taxRegistrationNumber", nifEmissor);
        docFieldsToSign.put("documentType", docType);
        docFieldsToSign.put("documentDate", docDateStr);
        docFieldsToSign.put("customerTaxID", customerNif);
        docFieldsToSign.put("customerCountry", customerCountry);
        docFieldsToSign.put("companyName", customerName);
        docFieldsToSign.put("documentTotals", docTotals);

        String jwsDocumentSignature = ao.co.hzconsultoria.efacturacao.util.JwsUtil
                .gerarJwsRs256(toJson(docFieldsToSign), privateKeyPem);
        docObj.put("jwsDocumentSignature", jwsDocumentSignature);

        payload.put("documents", java.util.Collections.singletonList(docObj));

        return payload;
    }

    private String toJson(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String toJsonPretty(Object obj) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return toJson(obj);
        }
    }

    private AgtResponse falha(String mensagem) {
        return new AgtResponse(false, null, "FALHA_ENVIO", mensagem);
    }
}
