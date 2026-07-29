package ao.co.hzconsultoria.efacturacao.scheduler;

import ao.co.hzconsultoria.efacturacao.model.LicencaGerada;
import ao.co.hzconsultoria.efacturacao.repository.LicencaGeradaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifica automaticamente todas as licenças do sistema e desactiva
 * as que tenham ultrapassado a data de expiração.
 * Executa todos os dias à meia-noite e também 5 minutos após o arranque do servidor.
 */
@Component
public class LicencaExpiracaoScheduler {

    @Autowired
    private LicencaGeradaRepository licencaGeradaRepository;

    /**
     * Corre diariamente à meia-noite (00:00)
     * e também 5 minutos após o início da aplicação (delay inicial de 5 min).
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void verificarLicencasExpiradas() {
        LocalDateTime agora = LocalDateTime.now();
        List<LicencaGerada> todasLicencas = licencaGeradaRepository.findAll();

        int desativadas = 0;
        for (LicencaGerada licenca : todasLicencas) {
            if (licenca.isAtiva() && licenca.getDataExpiracao() != null
                    && agora.isAfter(licenca.getDataExpiracao())) {
                licenca.setAtiva(false);
                licencaGeradaRepository.save(licenca);
                desativadas++;
                System.out.println("[LICENCA-SCHEDULER] Licença ID=" + licenca.getId()
                        + " do cliente '" + licenca.getClienteNome()
                        + "' expirou em " + licenca.getDataExpiracao()
                        + " — estado alterado para INACTIVA.");
            }
        }

        if (desativadas > 0) {
            System.out.println("[LICENCA-SCHEDULER] " + desativadas
                    + " licença(s) desactivada(s) automaticamente.");
        } else {
            System.out.println("[LICENCA-SCHEDULER] Verificação concluída. Todas as licenças activas estão válidas.");
        }
    }

    /**
     * Verificação extra executada 5 minutos após o arranque do servidor.
     * Garante que licenças expiradas durante o downtime também são desactivadas.
     */
    @Scheduled(initialDelay = 300000, fixedDelay = Long.MAX_VALUE)
    public void verificarLicencasAoArrancar() {
        System.out.println("[LICENCA-SCHEDULER] Verificação inicial de licenças ao arrancar...");
        verificarLicencasExpiradas();
    }
}
