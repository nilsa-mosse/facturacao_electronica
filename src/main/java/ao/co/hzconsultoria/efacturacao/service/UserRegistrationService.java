package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class UserRegistrationService {

    @Autowired
    private DynamicMailService dynamicMailService;

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    public String gerarSenhaAleatoria() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(caracteres.charAt(random.nextInt(caracteres.length())));
        }
        return sb.toString();
    }

    public void enviarCredenciaisPorEmail(User user, String senhaPlana) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            System.out.println(">>> [UserRegistrationService] Usuário " + user.getLogin() + " não tem email cadastrado. Pulando envio.");
            return;
        }

        String baseUrl = "http://localhost:8080";
        String nomeSistema = "Kwanza ERP";
        try {
            baseUrl = configuracaoSistemaRepository.findById(1L)
                    .map(cfg -> cfg.getServidorBaseUrl())
                    .orElse("http://localhost:8080");
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            nomeSistema = configuracaoSistemaRepository.findById(1L)
                    .map(cfg -> cfg.getSistemaNome())
                    .orElse("Kwanza ERP");
        } catch (Exception e) {
            // Ignorar
        }

        String assunto = "Credenciais de Acesso - " + nomeSistema;
        String mensagem = String.format(
            "Olá %s,\n\n" +
            "A sua conta no sistema %s foi criada com sucesso.\n\n" +
            "Aqui estão as suas credenciais de acesso:\n" +
            "- Utilizador: %s\n" +
            "- Palavra-passe: %s\n\n" +
            "Para aceder ao sistema, utilize o seguinte endereço:\n" +
            "%s\n\n" +
            "Por motivos de segurança, ser-lhe-á solicitado que altere esta palavra-passe no seu primeiro login.\n\n" +
            "Com os melhores cumprimentos,\n" +
            "A equipa de suporte.",
            user.getNome(), nomeSistema, user.getLogin(), senhaPlana, baseUrl
        );

        Long empresaId = (user.getEmpresa() != null) ? user.getEmpresa().getId() : null;
        if (!dynamicMailService.isEmailConfigurado(empresaId)) {
            System.out.println(">>> [UserRegistrationService] Email não configurado nas definições de sistema.");
            System.out.println(">>> [UserRegistrationService] Credenciais para " + user.getLogin() + " (" + user.getEmail() + "): " + senhaPlana);
            return;
        }

        try {
            dynamicMailService.enviarEmail(empresaId, user.getEmail(), assunto, mensagem);
            System.out.println(">>> [UserRegistrationService] Email com credenciais enviado com sucesso para: " + user.getEmail());
        } catch (Exception e) {
            System.err.println(">>> [UserRegistrationService] Erro ao enviar email com credenciais para " + user.getEmail() + ": " + e.getMessage());
        }
    }
}
