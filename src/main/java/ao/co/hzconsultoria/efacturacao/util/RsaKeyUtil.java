package ao.co.hzconsultoria.efacturacao.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;

/**
 * Utilitário para gestão da Estrutura da Chave de Assinatura Digital (AGT):
 * - Tipo: RSA
 * - Tamanho: mínimo 2048 bits
 * - Formato: PEM (PKCS#8 Base64)
 * - Algoritmo de Assinatura: SHA1withRSA / SHA256withRSA
 */
public class RsaKeyUtil {

    private static final Logger log = LoggerFactory.getLogger(RsaKeyUtil.class);

    /**
     * Gera um novo par de chaves RSA de 2048 bits no formato PEM Base64.
     */
    public static KeyPairResult gerarParChavesRsa2048() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();

            String privateKeyPem = exportarPrivateKeyPem(keyPair.getPrivate());
            String publicKeyPem = exportarPublicKeyPem(keyPair.getPublic());

            return new KeyPairResult(privateKeyPem, publicKeyPem);
        } catch (Exception e) {
            log.error("Erro ao gerar par de chaves RSA 2048: {}", e.getMessage());
            throw new RuntimeException("Erro ao gerar chaves RSA 2048", e);
        }
    }

    /**
     * Exporta a chave privada em formato PEM (PKCS#8 Base64).
     */
    public static String exportarPrivateKeyPem(PrivateKey privateKey) {
        String base64 = java.util.Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" +
                chunkString(base64, 64) +
                "-----END PRIVATE KEY-----";
    }

    /**
     * Exporta a chave pública em formato PEM Base64.
     */
    public static String exportarPublicKeyPem(PublicKey publicKey) {
        String base64 = java.util.Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" +
                chunkString(base64, 64) +
                "-----END PUBLIC KEY-----";
    }

    /**
     * Carrega a chave privada a partir do formato PEM Base64.
     */
    public static PrivateKey carregarPrivateKeyPem(String pem) throws Exception {
        if (pem == null || pem.trim().isEmpty()) {
            throw new IllegalArgumentException("A chave PEM não pode ser nula ou vazia.");
        }

        String cleanPem = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = java.util.Base64.getDecoder().decode(cleanPem);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    /**
     * Realiza a assinatura digital de dados utilizando a chave privada RSA de 2048 bits.
     */
    public static String assinar(String dados, String privateKeyPem) throws Exception {
        PrivateKey privateKey = carregarPrivateKeyPem(privateKeyPem);
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initSign(privateKey);
        signature.update(dados.getBytes(StandardCharsets.UTF_8));
        byte[] signed = signature.sign();
        return java.util.Base64.getEncoder().encodeToString(signed);
    }

    /**
     * Valida se a chave PEM informada é uma chave RSA válida com pelo menos 2048 bits.
     */
    public static boolean validarChaveRsa2048(String pem) {
        try {
            PrivateKey pk = carregarPrivateKeyPem(pem);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKeySpec spec = kf.getKeySpec(pk, RSAPrivateKeySpec.class);
            int bitLength = spec.getModulus().bitLength();
            return bitLength >= 2048;
        } catch (Exception e) {
            log.debug("Validação de chave RSA falhou: {}", e.getMessage());
            return false;
        }
    }

    private static String chunkString(String str, int chunkSize) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i += chunkSize) {
            sb.append(str, i, Math.min(str.length(), i + chunkSize)).append("\n");
        }
        return sb.toString();
    }

    public static class KeyPairResult {
        private final String privateKeyPem;
        private final String publicKeyPem;

        public KeyPairResult(String privateKeyPem, String publicKeyPem) {
            this.privateKeyPem = privateKeyPem;
            this.publicKeyPem = publicKeyPem;
        }

        public String getPrivateKeyPem() { return privateKeyPem; }
        public String getPublicKeyPem() { return publicKeyPem; }
    }
}
