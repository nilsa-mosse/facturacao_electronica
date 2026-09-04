package ao.co.hzconsultoria.efacturacao.scheduler;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.service.AgtService;
import ao.co.hzconsultoria.efacturacao.dto.AgtResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tarefa agendada para consultar assincronamente (Polling) a AGT
 * (/servicos/consultar.html) relativa às facturas pendentes de resposta.
 */
@Component
public class AgtPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(AgtPollingScheduler.class);

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private AgtService agtService;

    /**
     * Executa a cada 60 segundos (fixedDelay = 60000ms) com um delay inicial de 30s.
     */
    @Scheduled(initialDelay = 30000, fixedDelay = 60000)
    public void consultarFaturasPendentesAgt() {
        List<Fatura> pendentes = faturaRepository.findFaturasPendentesPollingAgt();
        if (pendentes.isEmpty()) {
            return;
        }

        log.info("[AGT POLLING] A verificar estado na AGT para {} fatura(s) pendente(s)...", pendentes.size());

        for (Fatura fatura : pendentes) {
            String requestId = fatura.getCodigoAgt();
            if (requestId == null || requestId.trim().isEmpty() || requestId.startsWith("ERRO:")) {
                continue;
            }

            try {
                AgtResponse resp = agtService.consultarEstadoFaturaAgt(requestId);
                if (resp.isSucesso()) {
                    fatura.setEnviadaAGT(true);
                    fatura.setStatus("VALIDADA_AGT");
                    faturaRepository.save(fatura);
                    log.info("[AGT POLLING] Fatura {} ({}) VALIDADA com sucesso pela AGT!", fatura.getNumeroFatura(), requestId);
                } else if ("REJEITADA".equalsIgnoreCase(resp.getStatus()) || "REJEITADO".equalsIgnoreCase(resp.getStatus())) {
                    fatura.setEnviadaAGT(false);
                    fatura.setStatus("REJEITADA_AGT");
                    faturaRepository.save(fatura);
                    log.warn("[AGT POLLING] Fatura {} ({}) REJEITADA pela AGT. Detalhes: {}", fatura.getNumeroFatura(), requestId, resp.getMensagem());
                } else {
                    log.info("[AGT POLLING] Fatura {} ({}) ainda em processamento na AGT.", fatura.getNumeroFatura(), requestId);
                }
            } catch (Exception e) {
                log.error("[AGT POLLING] Erro ao consultar fatura {}: {}", fatura.getNumeroFatura(), e.getMessage());
            }
        }
    }
}
