package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

/**
 * Entidade singleton (id=1) que persiste todas as configurações
 * do sistema (Servidor, Email, BD, Storage, Segurança) na base de dados.
 */
@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistemaEntity {

    @Id
    private Long id = 1L;

    // ─── Parâmetros Gerais ──────────────────────────────────────────────
    private String sistemaNome = "Sistema de Facturação";
    private String sistemaVersao = "1.0.0";
    private String sistemaEmailSuporte = "suporte@facturacao.com";
    private boolean sistemaBackup = true;
    private String sistemaTema = "light";

    // ─── Servidor e Rede ────────────────────────────────────────────────
    private int servidorPorta = 8080;

    @Column(length = 255)
    private String servidorHostname = "localhost";

    @Column(length = 500)
    private String servidorBaseUrl = "http://localhost:8080";

    private boolean servidorProxyHabilitado = false;

    @Column(length = 255)
    private String servidorProxyHost = "";

    private int servidorProxyPorta = 80;

    @Column(length = 1000)
    private String servidorCorsOrigens = "*";

    // ─── Email / SMTP ───────────────────────────────────────────────────
    @Column(length = 255)
    private String emailSmtpHost = "smtp.gmail.com";

    private int emailSmtpPorta = 587;

    @Column(length = 255)
    private String emailSmtpUsername = "";

    @Column(length = 500)
    private String emailSmtpPassword = "";

    @Column(length = 10)
    private String emailSegurancaTipo = "TLS";

    @Column(length = 255)
    private String emailRemetente = "noreply@empresa.ao";

    @Column(length = 255)
    private String emailNomeRemetente = "Sistema de Facturação";

    // ─── Base de Dados ──────────────────────────────────────────────────
    @Column(length = 50)
    private String dbTipoBD = "MySQL";

    private int dbConnectionTimeout = 30000;
    private int dbQueryTimeout = 60000;
    private int dbPoolMin = 5;
    private int dbPoolMax = 20;
    private int dbIdleTimeout = 30000;
    private int dbMaxLifetime = 60000;

    @Column(length = 100)
    private String dbSchema = "efacturacao";

    // ─── Storage ────────────────────────────────────────────────────────
    @Column(length = 20)
    private String storageTipo = "LOCAL";

    @Column(length = 500)
    private String storageCaminhoBase = "uploads/";

    private int storageTamanhoMaxFicheiro = 10;
    private int storageTamanhoMaxRequest = 20;

    @Column(length = 20)
    private String storageEstrategiaBackup = "DIARIO";

    @Column(length = 20)
    private String storageCloudProvider = "";

    @Column(length = 255)
    private String storageCloudBucket = "";

    @Column(length = 100)
    private String storageCloudRegion = "";

    // ─── Segurança ──────────────────────────────────────────────────────
    private int segTempoExpiracaoSessao = 30;
    private int segTentativasLoginMax = 5;
    private int segLockoutDuracao = 15;

    @Column(length = 20)
    private String segPoliticaPassword = "MEDIA";

    private int segComprimentoMinPassword = 8;
    private boolean segTwoFactorAtivo = false;
    private boolean segRequireUppercase = true;
    private boolean segRequireNumbers = true;
    private boolean segRequireSpecialChars = false;

    @Column(length = 2000)
    private String segIpWhitelist = "";

    private boolean segLogAcessosAtivo = true;

    // ─── Licença ────────────────────────────────────────────────────────
    private java.time.LocalDateTime licencaDataAtivacao;


    // ================================================================
    // Getters & Setters
    // ================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // Gerais
    public String getSistemaNome() { return sistemaNome; }
    public void setSistemaNome(String v) { this.sistemaNome = v; }
    public String getSistemaVersao() { return sistemaVersao; }
    public void setSistemaVersao(String v) { this.sistemaVersao = v; }
    public String getSistemaEmailSuporte() { return sistemaEmailSuporte; }
    public void setSistemaEmailSuporte(String v) { this.sistemaEmailSuporte = v; }
    public boolean isSistemaBackup() { return sistemaBackup; }
    public void setSistemaBackup(boolean v) { this.sistemaBackup = v; }
    public String getSistemaTema() { return sistemaTema; }
    public void setSistemaTema(String v) { this.sistemaTema = v; }

    // Servidor
    public int getServidorPorta() { return servidorPorta; }
    public void setServidorPorta(int v) { this.servidorPorta = v; }
    public String getServidorHostname() { return servidorHostname; }
    public void setServidorHostname(String v) { this.servidorHostname = v; }
    public String getServidorBaseUrl() { return servidorBaseUrl; }
    public void setServidorBaseUrl(String v) { this.servidorBaseUrl = v; }
    public boolean isServidorProxyHabilitado() { return servidorProxyHabilitado; }
    public void setServidorProxyHabilitado(boolean v) { this.servidorProxyHabilitado = v; }
    public String getServidorProxyHost() { return servidorProxyHost; }
    public void setServidorProxyHost(String v) { this.servidorProxyHost = v; }
    public int getServidorProxyPorta() { return servidorProxyPorta; }
    public void setServidorProxyPorta(int v) { this.servidorProxyPorta = v; }
    public String getServidorCorsOrigens() { return servidorCorsOrigens; }
    public void setServidorCorsOrigens(String v) { this.servidorCorsOrigens = v; }

    // Email
    public String getEmailSmtpHost() { return emailSmtpHost; }
    public void setEmailSmtpHost(String v) { this.emailSmtpHost = v; }
    public int getEmailSmtpPorta() { return emailSmtpPorta; }
    public void setEmailSmtpPorta(int v) { this.emailSmtpPorta = v; }
    public String getEmailSmtpUsername() { return emailSmtpUsername; }
    public void setEmailSmtpUsername(String v) { this.emailSmtpUsername = v; }
    public String getEmailSmtpPassword() { return emailSmtpPassword; }
    public void setEmailSmtpPassword(String v) { this.emailSmtpPassword = v; }
    public String getEmailSegurancaTipo() { return emailSegurancaTipo; }
    public void setEmailSegurancaTipo(String v) { this.emailSegurancaTipo = v; }
    public String getEmailRemetente() { return emailRemetente; }
    public void setEmailRemetente(String v) { this.emailRemetente = v; }
    public String getEmailNomeRemetente() { return emailNomeRemetente; }
    public void setEmailNomeRemetente(String v) { this.emailNomeRemetente = v; }

    // Database
    public String getDbTipoBD() { return dbTipoBD; }
    public void setDbTipoBD(String v) { this.dbTipoBD = v; }
    public int getDbConnectionTimeout() { return dbConnectionTimeout; }
    public void setDbConnectionTimeout(int v) { this.dbConnectionTimeout = v; }
    public int getDbQueryTimeout() { return dbQueryTimeout; }
    public void setDbQueryTimeout(int v) { this.dbQueryTimeout = v; }
    public int getDbPoolMin() { return dbPoolMin; }
    public void setDbPoolMin(int v) { this.dbPoolMin = v; }
    public int getDbPoolMax() { return dbPoolMax; }
    public void setDbPoolMax(int v) { this.dbPoolMax = v; }
    public int getDbIdleTimeout() { return dbIdleTimeout; }
    public void setDbIdleTimeout(int v) { this.dbIdleTimeout = v; }
    public int getDbMaxLifetime() { return dbMaxLifetime; }
    public void setDbMaxLifetime(int v) { this.dbMaxLifetime = v; }
    public String getDbSchema() { return dbSchema; }
    public void setDbSchema(String v) { this.dbSchema = v; }

    // Storage
    public String getStorageTipo() { return storageTipo; }
    public void setStorageTipo(String v) { this.storageTipo = v; }
    public String getStorageCaminhoBase() { return storageCaminhoBase; }
    public void setStorageCaminhoBase(String v) { this.storageCaminhoBase = v; }
    public int getStorageTamanhoMaxFicheiro() { return storageTamanhoMaxFicheiro; }
    public void setStorageTamanhoMaxFicheiro(int v) { this.storageTamanhoMaxFicheiro = v; }
    public int getStorageTamanhoMaxRequest() { return storageTamanhoMaxRequest; }
    public void setStorageTamanhoMaxRequest(int v) { this.storageTamanhoMaxRequest = v; }
    public String getStorageEstrategiaBackup() { return storageEstrategiaBackup; }
    public void setStorageEstrategiaBackup(String v) { this.storageEstrategiaBackup = v; }
    public String getStorageCloudProvider() { return storageCloudProvider; }
    public void setStorageCloudProvider(String v) { this.storageCloudProvider = v; }
    public String getStorageCloudBucket() { return storageCloudBucket; }
    public void setStorageCloudBucket(String v) { this.storageCloudBucket = v; }
    public String getStorageCloudRegion() { return storageCloudRegion; }
    public void setStorageCloudRegion(String v) { this.storageCloudRegion = v; }

    // Segurança
    public int getSegTempoExpiracaoSessao() { return segTempoExpiracaoSessao; }
    public void setSegTempoExpiracaoSessao(int v) { this.segTempoExpiracaoSessao = v; }
    public int getSegTentativasLoginMax() { return segTentativasLoginMax; }
    public void setSegTentativasLoginMax(int v) { this.segTentativasLoginMax = v; }
    public int getSegLockoutDuracao() { return segLockoutDuracao; }
    public void setSegLockoutDuracao(int v) { this.segLockoutDuracao = v; }
    public String getSegPoliticaPassword() { return segPoliticaPassword; }
    public void setSegPoliticaPassword(String v) { this.segPoliticaPassword = v; }
    public int getSegComprimentoMinPassword() { return segComprimentoMinPassword; }
    public void setSegComprimentoMinPassword(int v) { this.segComprimentoMinPassword = v; }
    public boolean isSegTwoFactorAtivo() { return segTwoFactorAtivo; }
    public void setSegTwoFactorAtivo(boolean v) { this.segTwoFactorAtivo = v; }
    public boolean isSegRequireUppercase() { return segRequireUppercase; }
    public void setSegRequireUppercase(boolean v) { this.segRequireUppercase = v; }
    public boolean isSegRequireNumbers() { return segRequireNumbers; }
    public void setSegRequireNumbers(boolean v) { this.segRequireNumbers = v; }
    public boolean isSegRequireSpecialChars() { return segRequireSpecialChars; }
    public void setSegRequireSpecialChars(boolean v) { this.segRequireSpecialChars = v; }
    public String getSegIpWhitelist() { return segIpWhitelist; }
    public void setSegIpWhitelist(String v) { this.segIpWhitelist = v; }
    public boolean isSegLogAcessosAtivo() { return segLogAcessosAtivo; }
    public void setSegLogAcessosAtivo(boolean v) { this.segLogAcessosAtivo = v; }

    // Licença
    public java.time.LocalDateTime getLicencaDataAtivacao() { return licencaDataAtivacao; }
    public void setLicencaDataAtivacao(java.time.LocalDateTime v) { this.licencaDataAtivacao = v; }
}

