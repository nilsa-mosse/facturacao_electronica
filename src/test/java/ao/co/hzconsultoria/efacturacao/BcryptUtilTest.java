package ao.co.hzconsultoria.efacturacao;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptUtilTest {

    public static void main(String[] args) {

        System.out.println(decodificarPorForcaBruta("$2a$10$XnQGghJV./9iOMIPNrslGuqIoU8nS9n/fX9Vx8Myf1g/7IPEtRgee"));
        /*
         * BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
         * 
         * // 1. GERAR NOVA SENHA (caso queira alterar diretamente no Banco de Dados)
         * String novaSenha = "admin";
         * String novoHash = encoder.encode(novaSenha);
         * System.out.println("--- GERADOR DE SENHA ---");
         * System.out.println("Nova Senha Texto: " + novaSenha);
         * System.out.println("Novo Hash Bcrypt: " + novoHash);
         * System.out.
         * println("Query SQL para atualizar o admin: UPDATE usuario SET senha='" +
         * novoHash + "' WHERE login='admin';\n");
         * 
         * 
         * // 2. VERIFICAR SE UMA SENHA CORRESPONDE AO HASH
         * System.out.println("--- VERIFICADOR DE SENHA ---");
         * String senhaParaTestar = "admin"; // Coloque aqui a senha que acha que é a
         * correta
         * 
         * // Os hashes que tiramos da base de dados
         * String hashAdmin =
         * "$2a$10$UmfOxB3WbT6xT/zk8XXw1Oi6zyvcfF7W36ztbFMV0ujr5pcb/ukoa";
         * String hashUser =
         * "$2a$10$P51Vqmk1VDnxXwmMIC8H8ut1L4N.cGrisLBm6Agv9IKeCdXlP/0gC";
         * String hashSuperAdmin =
         * "$2a$10$XnQGghJV./9iOMIPNrslGuqIoU8nS9n/fX9Vx8Myf1g/7IPEtRgee";
         * 
         * boolean matchAdmin = encoder.matches(senhaParaTestar, hashAdmin);
         * boolean matchUser = encoder.matches(senhaParaTestar, hashUser);
         * boolean matchSuperAdmin = encoder.matches(senhaParaTestar, hashSuperAdmin);
         * 
         * System.out.println("A senha '" + senhaParaTestar + "' corresponde ao admin? "
         * + matchAdmin);
         * System.out.println("A senha '" + senhaParaTestar + "' corresponde ao user? "
         * + matchUser);
         * System.out.println("A senha '" + senhaParaTestar +
         * "' corresponde ao superadmin? " + matchSuperAdmin);
         * 
         * System.out.println("\n--- TENTATIVA DE 'DECODIFICAÇÃO' (FORÇA BRUTA) ---");
         * String senhaAdminEncontrada = decodificarPorForcaBruta(hashAdmin);
         * if (senhaAdminEncontrada != null) {
         * System.out.println("SUCESSO! A senha do admin é: " + senhaAdminEncontrada);
         * } else {
         * System.out.println("A senha do admin não está na nossa lista de testes.");
         * }
         */
    }

    /**
     * Tenta descobrir a senha testando contra uma lista de senhas comuns.
     * Como o Bcrypt não pode ser revertido, a única forma é por tentativa e erro.
     */
    public static String decodificarPorForcaBruta(String hash) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // Pode adicionar aqui mais senhas comuns para testar
        String[] dicionarioSenhas = {
                "123456", "12345678", "123456789", "admin", "admin123",
                "password", "senha", "root", "1234", "qwerty", "mosse", "superadmin"
        };

        System.out.println("A testar " + dicionarioSenhas.length + " senhas possíveis...");

        for (String tentativa : dicionarioSenhas) {
            if (encoder.matches(tentativa, hash)) {
                return tentativa; // Senha encontrada!
            }
        }

        return null; // Nenhuma senha da lista correspondeu
    }
}
