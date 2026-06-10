package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.PasswordResetToken;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.repository.PasswordResetTokenRepository;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DynamicMailService dynamicMailService;

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    public Optional<User> findUserByLogin(String login) {
        return userRepository.findByLogin(login);
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findAll().stream()
                .filter(u -> email != null && email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    public Optional<User> findUserByLoginOrEmail(String loginOrEmail) {
        Optional<User> byLogin = userRepository.findByLogin(loginOrEmail);
        if (byLogin.isPresent()) return byLogin;
        return findUserByEmail(loginOrEmail);
    }

    @Transactional
    public String createTokenForUser(User user) {
        String token = UUID.randomUUID().toString();
        PasswordResetToken t = new PasswordResetToken();
        t.setToken(token);
        t.setUser(user);
        t.setExpiryDate(LocalDateTime.now().plusHours(2));
        tokenRepository.save(t);
        return token;
    }

    public Optional<PasswordResetToken> validateToken(String token) {
        Optional<PasswordResetToken> t = tokenRepository.findByToken(token);
        if (!t.isPresent()) return Optional.empty();
        if (t.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(t.get());
            return Optional.empty();
        }
        return t;
    }

    @Transactional
    public void removeToken(String token) {
        tokenRepository.deleteByToken(token);
    }

    public void sendResetEmail(String toEmail, String resetUrl) {
        String baseUrl = "http://localhost:8080";
        try {
            baseUrl = configuracaoSistemaRepository.findById(1L)
                    .map(cfg -> cfg.getServidorBaseUrl())
                    .orElse("http://localhost:8080");
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
        } catch (Exception e) {
            // Ignorar
        }
        
        String absoluteUrl = baseUrl + resetUrl;

        Optional<User> userOpt = findUserByEmail(toEmail);
        Long empresaId = userOpt.map(u -> u.getEmpresa() != null ? u.getEmpresa().getId() : null).orElse(null);

        if (!dynamicMailService.isEmailConfigurado(empresaId)) {
            // Mail not configured; just log the reset url to console
            System.out.println(">>> [DynamicMail] Email não configurado. URL para " + toEmail + ": " + absoluteUrl);
            return;
        }
        try {
            dynamicMailService.enviarEmail(
                empresaId,
                toEmail,
                "Recuperação de Palavra-passe",
                "Use o link abaixo para redefinir a sua palavra-passe:\n" + absoluteUrl + "\nSe não solicitou, ignore este email."
            );
        } catch (Exception e) {
            System.err.println(">>> Erro ao enviar email de redefinição de palavra-passe para " + toEmail + ": " + e.getMessage());
        }
    }
}
