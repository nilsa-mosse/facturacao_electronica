package ao.co.hzconsultoria.efacturacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração de beans da aplicação.
 */
@Configuration
public class AppConfig {

    /**
     * Regista o RestTemplate como bean Spring para injecção no AgtService.
     * Pode ser configurado com timeouts e interceptors no futuro.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
