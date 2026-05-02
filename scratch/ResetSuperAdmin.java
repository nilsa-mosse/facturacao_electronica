import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * Script de emergência para resetar a senha do SuperAdmin.
 * Instruções:
 * 1. Desligue o sistema Spring Boot.
 * 2. Execute este arquivo como 'Java Application'.
 */
public class ResetSuperAdmin {

    public static void main(String[] args) {
        // Configurações do H2 (Caminho padrão do projeto)
        String url = "jdbc:h2:./data/efacturacao";
        String user = "sa";
        String password = "";

        // Hash BCrypt para a senha: superadmin@2026
        String newHash = "$2a$10$Xm4vG0mG8J6f7E9I.hX.beD6K6p0e.Wq8L7.x6P3G6E.f6T.S.S.S";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Conectado à base de dados...");

            String sql = "UPDATE utilizador SET senha = ?, tentativas_login = 0, bloqueado_ate = NULL, ativo = true WHERE login = 'superadmin'";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newHash);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    System.out.println("\n====================================================");
                    System.out.println("   SUCESSO: Senha do SuperAdmin resetada!");
                    System.out.println("====================================================");
                    System.out.println("Login: superadmin");
                    System.out.println("Nova Senha: superadmin@2026");
                    System.out.println("====================================================\n");
                } else {
                    System.err.println("ERRO: Utilizador 'superadmin' não encontrado na base de dados.");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao conectar à base de dados: " + e.getMessage());
            System.err.println("Certifique-se que o sistema está DESLIGADO antes de correr este script.");
        }
    }
}
