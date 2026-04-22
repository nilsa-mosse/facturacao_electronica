package ao.co.hzconsultoria.efacturacao.security;

import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSeguranca;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import ao.co.hzconsultoria.efacturacao.service.ConfiguracaoSistemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationFailureLockedEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AuthenticationEventListener {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfiguracaoSistemaService configService;

    @EventListener
    @Transactional
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String login = event.getAuthentication().getName();
        userRepository.findByLogin(login).ifPresent(user -> {
            ConfiguracaoSeguranca config = configService.getSeguranca();
            
            int tentativas = user.getTentativasLogin() + 1;
            user.setTentativasLogin(tentativas);
            
            if (tentativas >= config.getTentativasLoginMax()) {
                LocalDateTime bloqueadoAte = LocalDateTime.now().plusMinutes(config.getLockoutDuracao());
                user.setBloqueadoAte(bloqueadoAte);
            }
            
            userRepository.save(user);
        });
    }

    @EventListener
    @Transactional
    public void onAuthenticationLocked(AuthenticationFailureLockedEvent event) {
        // Apenas para log ou auditoria se necessário
        String login = event.getAuthentication().getName();
        // O Spring já lançou a LockedException pois CustomUserDetails retornou isAccountNonLocked = false
    }

    @EventListener
    @Transactional
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String login = event.getAuthentication().getName();
        userRepository.findByLogin(login).ifPresent(user -> {
            if (user.getTentativasLogin() > 0 || user.getBloqueadoAte() != null) {
                user.setTentativasLogin(0);
                user.setBloqueadoAte(null);
                userRepository.save(user);
            }
        });
    }
}
