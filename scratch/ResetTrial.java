import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ResetTrial {
    public static void main(String[] args) {
        String url = "jdbc:h2:file:./data/efacturacao;MODE=MySQL;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        String user = "sa";
        String password = "";

        try (Connection conn = DriverManager.getConnection(url, user, password);
                Statement stmt = conn.createStatement()) {

            String sql = "UPDATE configuracao_sistema SET licenca_data_ativacao = NULL WHERE id = 1";
            int rows = stmt.executeUpdate(sql);

            if (rows > 0) {
                System.out.println("SUCESSO: O trial foi resetado com sucesso!");
            } else {
                System.out.println("AVISO: Nenhuma linha foi alterada. Verifique se a tabela/id existe.");
            }

        } catch (Exception e) {
            System.err.println("ERRO ao executar SQL: " + e.getMessage());
            if (e.getMessage().contains("Database may be already in use")) {
                System.err.println(
                        "DICA: Certifique-se de que a aplicao Spring Boot est DESLIGADA antes de correr este script.");
            }
        }
    }
}
