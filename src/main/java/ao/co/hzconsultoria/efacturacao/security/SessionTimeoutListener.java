package ao.co.hzconsultoria.efacturacao.security;

import ao.co.hzconsultoria.efacturacao.service.ConfiguracaoSistemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Listener que aplica o tempo de expiração de sessão configurado na base de dados
 * a cada nova sessão criada no sistema.
 */
@Component
public class SessionTimeoutListener implements HttpSessionListener {

    @Autowired
    private ConfiguracaoSistemaService configService;

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        try {
            // O tempo na configuração está em MINUTOS
            int timeoutMinutes = configService.getSeguranca().getTempoExpiracaoSessao();
            
            if (timeoutMinutes < 1) timeoutMinutes = 1;

            // setMaxInactiveInterval aceita SEGUNDOS
            se.getSession().setMaxInactiveInterval(timeoutMinutes * 60);
            
        } catch (Exception e) {
            // Caso falte configuração ou BD offline, define padrão de 30 minutos
            se.getSession().setMaxInactiveInterval(30 * 60);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        // Nada a fazer ao destruir
    }
}
