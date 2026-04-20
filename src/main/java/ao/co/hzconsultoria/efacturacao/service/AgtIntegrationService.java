package ao.co.hzconsultoria.efacturacao.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class AgtIntegrationService {

    @Value("${agt.api.url:https://homologacao.agt.minfin.gov.ao/api/v1}")
    private String agtUrl;

    @Value("${agt.api.token:SEU_TOKEN_AQUI}")
    private String agtToken;

    private final RestTemplate restTemplate;

    public AgtIntegrationService() {
        this.restTemplate = new RestTemplate();
    }

    public String submeterSaft(String xmlContent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.set("Authorization", "Bearer " + agtToken);

            HttpEntity<String> entity = new HttpEntity<>(xmlContent, headers);

            // Simulação de chamada real - Endpoint fictício de submissão
            // ResponseEntity<String> response = restTemplate.postForEntity(agtUrl + "/saft/submeter", entity, String.class);
            
            // Simulação de resposta de sucesso da AGT
            return "{\"status\": \"SUCESSO\", \"mensagem\": \"SAF-T recebido e validado\", \"id_recepcao\": \"AO-SAFT-2026-X99\"}";
        } catch (Exception e) {
            return "{\"status\": \"ERRO\", \"mensagem\": \"Falha na comunicação com a AGT: " + e.getMessage() + "\"}";
        }
    }
}
