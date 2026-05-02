import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilitário para gerar chaves de ativação para clientes.
 * Como usar: Altere as variáveis 'machineId' e 'dataExpiracao' e execute este
 * arquivo.
 */
public class GerarLicenca {

    // DEVE SER A MESMA CHAVE DEFINIDA NO LicencaService.java
    private static final String SECRET_KEY = "HZ-FACT-2024-SYS";

    public static void main(String[] args) {
        // --- DADOS DO CLIENTE ---
        // String machineId = "A1B2C3D4"; // Peça este código ao cliente (aparece na
        // tela de erro)
        // String dataExpiracao = "2025-12-31"; // Formato YYYY-MM-DD (Ex: 2025-12-31
        // para um ano)

        String machineId = "736E21F0"; // Peça este código ao cliente (aparece na tela de erro)
        String dataExpiracao = "2026-05-01 13:00:00"; // Formato YYYY-MM-DD (Ex: 2025-12-31 para um ano)
        // -----------------------

        try {
            String rawContent = machineId + "|" + dataExpiracao + "|SIGNED_BY_MOSSE";

            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            byte[] encrypted = cipher.doFinal(rawContent.getBytes(StandardCharsets.UTF_8));
            String activationKey = Base64.getEncoder().encodeToString(encrypted);

            System.out.println("\n====================================================");
            System.out.println("   GERADOR DE LICENÇAS - eFacturação");
            System.out.println("====================================================");
            System.out.println("ID da Máquina: " + machineId);
            System.out.println("Válido até   : " + dataExpiracao);
            System.out.println("----------------------------------------------------");
            System.out.println("CHAVE DE ATIVAÇÃO:");
            System.out.println(activationKey);
            System.out.println("====================================================\n");

        } catch (Exception e) {
            System.err.println("Erro ao gerar licença: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
