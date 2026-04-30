package ao.co.hzconsultoria.efacturacao;

import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableScheduling
public class EfaturacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EfaturacaoApplication.class, args);
    }

    @Bean
    public CommandLineRunner activateUsers(UserRepository repository) {
        return args -> {
            repository.findAll().forEach(user -> {
                if (!user.isAtivo()) {
                    user.setAtivo(true);
                    repository.save(user);
                }
            });
        };
    }
}