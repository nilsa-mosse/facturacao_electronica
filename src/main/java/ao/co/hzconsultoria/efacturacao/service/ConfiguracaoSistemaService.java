package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço que mantém uma única linha (id=1) na tabela configuracao_sistema
 * e converte entre a entidade JPA e os POJOs usados pelos templates.
 */
@Service
public class ConfiguracaoSistemaService {

    private static final Long CONFIG_ID = 1L;

    @Autowired
    private ConfiguracaoSistemaRepository repository;

    // ─── Helpers ───────────────────────────────────────────────────────

    /** Carrega (ou cria) a linha singleton de configuração. */
    private ConfiguracaoSistemaEntity loadOrCreate() {
        return repository.findById(CONFIG_ID).orElseGet(() -> {
            ConfiguracaoSistemaEntity e = new ConfiguracaoSistemaEntity();
            e.setId(CONFIG_ID);
            return repository.save(e);
        });
    }

    // ─── Sistema (Parâmetros Gerais) ───────────────────────────────────

    public Sistema getSistema() {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        Sistema s = new Sistema();
        s.setNome(e.getSistemaNome());
        s.setVersao(e.getSistemaVersao());
        s.setEmailSuporte(e.getSistemaEmailSuporte());
        s.setBackup(e.isSistemaBackup());
        s.setTema(e.getSistemaTema());
        return s;
    }

    @Transactional
    public void saveSistema(Sistema s) {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        e.setSistemaNome(s.getNome());
        e.setSistemaVersao(s.getVersao());
        e.setSistemaEmailSuporte(s.getEmailSuporte());
        e.setSistemaBackup(s.isBackup());
        e.setSistemaTema(s.getTema());
        repository.save(e);
    }

    // ─── Servidor e Rede ──────────────────────────────────────────────

    public ConfiguracaoServidor getServidor() {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        ConfiguracaoServidor s = new ConfiguracaoServidor();
        s.setPorta(e.getServidorPorta());
        s.setHostname(nvl(e.getServidorHostname()));
        s.setBaseUrl(nvl(e.getServidorBaseUrl()));
        s.setProxyHabilitado(e.isServidorProxyHabilitado());
        s.setProxyHost(nvl(e.getServidorProxyHost()));
        s.setProxyPorta(e.getServidorProxyPorta());
        s.setCorsOrigensPermitidas(nvl(e.getServidorCorsOrigens()));
        return s;
    }

    @Transactional
    public void saveServidor(ConfiguracaoServidor s) {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        e.setServidorPorta(s.getPorta());
        e.setServidorHostname(s.getHostname());
        e.setServidorBaseUrl(s.getBaseUrl());
        e.setServidorProxyHabilitado(s.isProxyHabilitado());
        e.setServidorProxyHost(s.getProxyHost());
        e.setServidorProxyPorta(s.getProxyPorta());
        e.setServidorCorsOrigens(s.getCorsOrigensPermitidas());
        repository.save(e);
    }

    // ─── Email / SMTP ─────────────────────────────────────────────────

    public ConfiguracaoEmail getEmail() {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        ConfiguracaoEmail c = new ConfiguracaoEmail();
        c.setSmtpHost(nvl(e.getEmailSmtpHost()));
        c.setSmtpPorta(e.getEmailSmtpPorta());
        c.setSmtpUsername(nvl(e.getEmailSmtpUsername()));
        c.setSmtpPassword(nvl(e.getEmailSmtpPassword()));
        c.setSegurancaTipo(nvl(e.getEmailSegurancaTipo(), "TLS"));
        c.setEmailRemetente(nvl(e.getEmailRemetente()));
        c.setNomeRemetente(nvl(e.getEmailNomeRemetente()));
        return c;
    }

    @Transactional
    public void saveEmail(ConfiguracaoEmail c) {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        e.setEmailSmtpHost(c.getSmtpHost());
        e.setEmailSmtpPorta(c.getSmtpPorta());
        e.setEmailSmtpUsername(c.getSmtpUsername());
        e.setEmailSmtpPassword(c.getSmtpPassword());
        e.setEmailSegurancaTipo(c.getSegurancaTipo());
        e.setEmailRemetente(c.getEmailRemetente());
        e.setEmailNomeRemetente(c.getNomeRemetente());
        repository.save(e);
    }

    // ─── Base de Dados ────────────────────────────────────────────────

    public ConfiguracaoDatabase getDatabase() {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        ConfiguracaoDatabase d = new ConfiguracaoDatabase();
        d.setTipoBD(nvl(e.getDbTipoBD(), "MySQL"));
        d.setConnectionTimeout(e.getDbConnectionTimeout());
        d.setQueryTimeout(e.getDbQueryTimeout());
        d.setPoolMin(e.getDbPoolMin());
        d.setPoolMax(e.getDbPoolMax());
        d.setIdleTimeout(e.getDbIdleTimeout());
        d.setMaxLifetime(e.getDbMaxLifetime());
        d.setSchema(nvl(e.getDbSchema()));
        return d;
    }

    @Transactional
    public void saveDatabase(ConfiguracaoDatabase d) {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        e.setDbTipoBD(d.getTipoBD());
        e.setDbConnectionTimeout(d.getConnectionTimeout());
        e.setDbQueryTimeout(d.getQueryTimeout());
        e.setDbPoolMin(d.getPoolMin());
        e.setDbPoolMax(d.getPoolMax());
        e.setDbIdleTimeout(d.getIdleTimeout());
        e.setDbMaxLifetime(d.getMaxLifetime());
        e.setDbSchema(d.getSchema());
        repository.save(e);
    }

    // ─── Storage ──────────────────────────────────────────────────────

    public ConfiguracaoStorage getStorage() {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        ConfiguracaoStorage s = new ConfiguracaoStorage();
        s.setTipoStorage(nvl(e.getStorageTipo(), "LOCAL"));
        s.setCaminhoBase(nvl(e.getStorageCaminhoBase(), "uploads/"));
        s.setTamanhoMaxFicheiro(e.getStorageTamanhoMaxFicheiro());
        s.setTamanhoMaxRequest(e.getStorageTamanhoMaxRequest());
        s.setEstrategiaBackup(nvl(e.getStorageEstrategiaBackup(), "DIARIO"));
        s.setCloudProvider(nvl(e.getStorageCloudProvider()));
        s.setCloudBucket(nvl(e.getStorageCloudBucket()));
        s.setCloudRegion(nvl(e.getStorageCloudRegion()));
        return s;
    }

    @Transactional
    public void saveStorage(ConfiguracaoStorage s) {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        e.setStorageTipo(s.getTipoStorage());
        e.setStorageCaminhoBase(s.getCaminhoBase());
        e.setStorageTamanhoMaxFicheiro(s.getTamanhoMaxFicheiro());
        e.setStorageTamanhoMaxRequest(s.getTamanhoMaxRequest());
        e.setStorageEstrategiaBackup(s.getEstrategiaBackup());
        e.setStorageCloudProvider(s.getCloudProvider());
        e.setStorageCloudBucket(s.getCloudBucket());
        e.setStorageCloudRegion(s.getCloudRegion());
        repository.save(e);
    }

    // ─── Segurança ────────────────────────────────────────────────────

    public ConfiguracaoSeguranca getSeguranca() {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        ConfiguracaoSeguranca s = new ConfiguracaoSeguranca();
        s.setTempoExpiracaoSessao(e.getSegTempoExpiracaoSessao());
        s.setTentativasLoginMax(e.getSegTentativasLoginMax());
        s.setLockoutDuracao(e.getSegLockoutDuracao());
        s.setPoliticaPassword(nvl(e.getSegPoliticaPassword(), "MEDIA"));
        s.setComprimentoMinPassword(e.getSegComprimentoMinPassword());
        s.setTwoFactorAtivo(e.isSegTwoFactorAtivo());
        s.setRequireUppercase(e.isSegRequireUppercase());
        s.setRequireNumbers(e.isSegRequireNumbers());
        s.setRequireSpecialChars(e.isSegRequireSpecialChars());
        s.setIpWhitelist(nvl(e.getSegIpWhitelist()));
        s.setLogAcessosAtivo(e.isSegLogAcessosAtivo());
        return s;
    }

    @Transactional
    public void saveSeguranca(ConfiguracaoSeguranca s) {
        ConfiguracaoSistemaEntity e = loadOrCreate();
        e.setSegTempoExpiracaoSessao(s.getTempoExpiracaoSessao());
        e.setSegTentativasLoginMax(s.getTentativasLoginMax());
        e.setSegLockoutDuracao(s.getLockoutDuracao());
        e.setSegPoliticaPassword(s.getPoliticaPassword());
        e.setSegComprimentoMinPassword(s.getComprimentoMinPassword());
        e.setSegTwoFactorAtivo(s.isTwoFactorAtivo());
        e.setSegRequireUppercase(s.isRequireUppercase());
        e.setSegRequireNumbers(s.isRequireNumbers());
        e.setSegRequireSpecialChars(s.isRequireSpecialChars());
        e.setSegIpWhitelist(s.getIpWhitelist());
        e.setSegLogAcessosAtivo(s.isLogAcessosAtivo());
        repository.save(e);
    }

    // ─── Utilitários ──────────────────────────────────────────────────

    private String nvl(String value) {
        return value != null ? value : "";
    }

    private String nvl(String value, String defaultVal) {
        return (value != null && !value.isEmpty()) ? value : defaultVal;
    }
}
