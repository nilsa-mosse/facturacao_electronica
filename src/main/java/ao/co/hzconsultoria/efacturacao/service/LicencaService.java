package ao.co.hzconsultoria.efacturacao.service;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

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
    
    // Chave mestra para criptografia das licenças (deve ser mantida em segredo pelo desenvolvedor)
    private static final String SECRET_KEY = "HZ-FACT-2024-SYS"; 

    private Long timeOffset = null;
    private LocalDateTime lastNtpFetch = null;

    public LocalDateTime getCurrentNetworkTime() {
        if (timeOffset != null && lastNtpFetch != null && 
            ChronoUnit.HOURS.between(lastNtpFetch, LocalDateTime.now()) < 1) {
            return LocalDateTime.now().plus(timeOffset, ChronoUnit.MILLIS);
        }

        NTPUDPClient client = new NTPUDPClient();
        client.setDefaultTimeout(3000);
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
            if (this.timeOffset != null) {
                return LocalDateTime.now().plus(this.timeOffset, ChronoUnit.MILLIS);
            }
            return LocalDateTime.now();
        } finally {
            client.close();
        }
    }

    /**
     * Gera um ID único para esta máquina baseado no Hardware/SO
     */
    public String getMachineId() {
        try {
            String rawId = System.getProperty("os.name") + 
                           System.getProperty("os.arch") + 
                           System.getProperty("user.name") +
                           Runtime.getRuntime().availableProcessors();
            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawId.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < 4; i++) { // Pegar apenas os primeiros 8 caracteres (4 bytes) para simplificar
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().toUpperCase();
        } catch (Exception e) {
            return "UNKNOWN-DEVICE";
        }
    }

    /**
     * Valida uma chave de ativação profissional
     */
    public boolean validarChave(String chave) {
        if (chave == null || chave.isEmpty()) return false;
        
        try {
            // Decriptação simples (exemplo didático com AES)
            SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            
            byte[] decoded = Base64.getDecoder().decode(chave);
            byte[] original = cipher.doFinal(decoded);
            String decrypted = new String(original, StandardCharsets.UTF_8);
            
            // Formato esperado: MACHINE_ID|EXPIRY_DATE(YYYY-MM-DD)|HASH
            String[] parts = decrypted.split("\\|");
            if (parts.length < 3) return false;
            
            String machineId = parts[0];
            String expiryStr = parts[1];
            
            // 1. Validar Máquina
            if (!machineId.equals(getMachineId())) return false;
            
            // 2. Validar Data e Hora (comparando com hora da rede)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime expiryDate = LocalDateTime.parse(expiryStr, formatter);
            LocalDateTime agora = getCurrentNetworkTime();
            
            return !agora.isAfter(expiryDate);
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Gera uma chave de ativação criptografada
     */
    public String gerarChave(String machineId, String dataExpiracao) throws Exception {
        String rawContent = machineId + "|" + dataExpiracao + "|SIGNED_BY_MOSSE";
        
        SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        
        byte[] encrypted = cipher.doFinal(rawContent.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public boolean isLicencaValida() {
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L).orElseGet(() -> {
            ConfiguracaoSistemaEntity e = new ConfiguracaoSistemaEntity();
            e.setId(1L);
            return configuracaoSistemaRepository.save(e);
        });

        // Primeiro verificar se existe uma licença profissional instalada
        String chave = config.getLicencaChaveAtivacao();
        if (chave != null && !chave.trim().isEmpty()) {
            // Se existe uma chave, a validade do sistema depende EXCLUSIVAMENTE dela.
            // Não deve fazer fallback para o trial de 5 minutos se a chave pro expirar.
            return validarChave(chave);
        }

        // Se não houver licença pro, validar o trial de 5 minutos
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
