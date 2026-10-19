package ao.co.hzconsultoria.efacturacao.scheduler;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoFacebook;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoFacebookRepository;
import ao.co.hzconsultoria.efacturacao.service.FacebookService;
import ao.co.hzconsultoria.efacturacao.service.FacebookService.ResultadoPublicacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduler para publicação automática no Facebook.
 *
 * Executa publicações agendadas para todas as empresas que tenham
 * a integração com Facebook activa, respeitando os limites configurados.
 *
 * Horários padrão:
 *  - 09:00 → Publicar promoções activas
 *  - 12:00 → Publicar produtos com stock baixo (se configurado)
 *  - 18:00 → Publicar promoções da tarde (segunda execução)
 */
@Component
public class FacebookPublicacaoScheduler {

    private static final Logger log = LoggerFactory.getLogger(FacebookPublicacaoScheduler.class);

    @Autowired
    private FacebookService facebookService;

    @Autowired
    private ConfiguracaoFacebookRepository fbConfigRepo;

    /**
     * Publicação automática da manhã - Promoções.
     * Executa todos os dias às 09:00.
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void publicarPromocoesManha() {
        log.info("[Facebook Scheduler] Iniciando publicação automática de promoções - Manhã (09:00)");
        publicarPromocoesParaTodasEmpresas();
    }

    /**
     * Publicação automática ao meio-dia - Stock Baixo.
     * Executa todos os dias às 12:00.
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void publicarStockBaixoMeioDia() {
        log.info("[Facebook Scheduler] Iniciando publicação automática de stock baixo - 12:00");
        publicarStockBaixoParaTodasEmpresas();
    }

    /**
     * Publicação automática da tarde - Segunda ronda de promoções.
     * Executa todos os dias às 18:00.
     */
    @Scheduled(cron = "0 0 18 * * ?")
    public void publicarPromocoesTarde() {
        log.info("[Facebook Scheduler] Iniciando publicação automática de promoções - Tarde (18:00)");
        publicarPromocoesParaTodasEmpresas();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void publicarPromocoesParaTodasEmpresas() {
        List<ConfiguracaoFacebook> configs = fbConfigRepo.findAll();
        for (ConfiguracaoFacebook config : configs) {
            if (!config.isAtivo() || !config.isPublicarPromocoesAuto()) {
                continue;
            }
            Long empresaId = config.getEmpresa().getId();
            log.info("[Facebook Scheduler] Publicando promoções para empresa ID={}", empresaId);
            try {
                List<ResultadoPublicacao> resultados = facebookService.publicarPromocoesAutomaticas(empresaId);
                long sucesso = resultados.stream().filter(ResultadoPublicacao::isSucesso).count();
                long falha   = resultados.stream().filter(r -> !r.isSucesso()).count();
                log.info("[Facebook Scheduler] Empresa {}: {} publicações com sucesso, {} com falha.",
                        empresaId, sucesso, falha);
            } catch (Exception e) {
                log.error("[Facebook Scheduler] Erro ao publicar promoções para empresa {}: {}",
                        empresaId, e.getMessage());
            }
        }
    }

    private void publicarStockBaixoParaTodasEmpresas() {
        List<ConfiguracaoFacebook> configs = fbConfigRepo.findAll();
        for (ConfiguracaoFacebook config : configs) {
            if (!config.isAtivo() || !config.isPublicarStockBaixo()) {
                continue;
            }
            Long empresaId = config.getEmpresa().getId();
            log.info("[Facebook Scheduler] Publicando stock baixo para empresa ID={}", empresaId);
            try {
                List<ResultadoPublicacao> resultados = facebookService.publicarStockBaixoAutomatico(empresaId);
                long sucesso = resultados.stream().filter(ResultadoPublicacao::isSucesso).count();
                log.info("[Facebook Scheduler] Empresa {}: {} publicações de stock baixo com sucesso.", empresaId, sucesso);
            } catch (Exception e) {
                log.error("[Facebook Scheduler] Erro ao publicar stock baixo para empresa {}: {}",
                        empresaId, e.getMessage());
            }
        }
    }
}
