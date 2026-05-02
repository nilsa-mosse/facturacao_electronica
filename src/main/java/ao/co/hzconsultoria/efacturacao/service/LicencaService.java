package ao.co.hzconsultoria.efacturacao.service;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;

@Service
public class LicencaService {

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    private static final String NTP_SERVER = "pool.ntp.org";
    private static final int TRIAL_MINUTES = 5;

    private Long timeOffset = null;
    private LocalDateTime lastNtpFetch = null;

    public LocalDateTime getCurrentNetworkTime() {
        // Se já temos um offset e foi obtido há menos de 1 hora, usamos o offset + hora do sistema
        if (timeOffset != null && lastNtpFetch != null && 
            ChronoUnit.HOURS.between(lastNtpFetch, LocalDateTime.now()) < 1) {
            return LocalDateTime.now().plus(timeOffset, ChronoUnit.MILLIS);
        }

        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(3000); // Reduzir timeout para 3s
        try {
            client.open();
            InetAddress hostAddr = InetAddress.getByName(NTP_SERVER);
            TimeInfo info = client.getTime(hostAddr);
            long networkTimeMillis = info.getMessage().getTransmitTimeStamp().getTime();
            long systemTimeMillis = System.currentTimeMillis();
            
            this.timeOffset = networkTimeMillis - systemTimeMillis;
            this.lastNtpFetch = LocalDateTime.now();
            
            return LocalDateTime.ofInstant(new Date(networkTimeMillis).toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            System.err.println("Erro ao obter hora do NTP: " + e.getMessage());
            // Se falhar e já tivermos um offset antigo, usamos o offset antigo
            if (this.timeOffset != null) {
                return LocalDateTime.now().plus(this.timeOffset, ChronoUnit.MILLIS);
            }
            return LocalDateTime.now();
        } finally {
            client.close();
        }
    }

    public boolean isLicencaValida() {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L).orElseGet(() -> {
            ConfiguracaoSistemaEntity e = new ConfiguracaoSistemaEntity();
            e.setId(1L);
            return configuracaoSistemaRepository.save(e);
        });

        LocalDateTime agora = getCurrentNetworkTime();

        if (config.getLicencaDataAtivacao() == null) {
            config.setLicencaDataAtivacao(agora);
            configuracaoSistemaRepository.save(config);
            return true;
        }

        long minutosPassados = ChronoUnit.MINUTES.between(config.getLicencaDataAtivacao(), agora);
        return minutosPassados < TRIAL_MINUTES;
    }
}
