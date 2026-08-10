package ao.co.hzconsultoria.efacturacao.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

/**
 * Utilitário para geração de Assinaturas JWS (JSON Web Signature) no padrão AGT (RS256: RSA + SHA-256 com Base64URL sem padding).
 */
public class JwsUtil {

    private static final Logger log = LoggerFactory.getLogger(JwsUtil.class);

    private static final String JWS_HEADER_BASE64URL = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"typ\":\"JOSE\",\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));

    /**
     * Assina um payload JSON gerando uma assinatura JWS RS256 válida (Header.Payload.Signature).
     */
    public static String gerarJwsRs256(String jsonPayload, String privateKeyPem) {
        try {
            if (jsonPayload == null || jsonPayload.trim().isEmpty()) {
                throw new IllegalArgumentException("O payload JSON não pode ser vazio para assinatura JWS.");
            }

            String payloadBase64Url = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));

            String contentToSign = JWS_HEADER_BASE64URL + "." + payloadBase64Url;

            PrivateKey privateKey = RsaKeyUtil.carregarPrivateKeyPem(privateKeyPem);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(contentToSign.getBytes(StandardCharsets.UTF_8));
            byte[] sigBytes = signature.sign();

            String signatureBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);

            return contentToSign + "." + signatureBase64Url;
        } catch (Exception e) {
            log.error("Erro ao gerar assinatura JWS RS256: {}", e.getMessage());
            return "ERROR_JWS_SIGNATURE";
        }
    }
}
