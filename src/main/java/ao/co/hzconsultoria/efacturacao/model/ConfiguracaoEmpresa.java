package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

/**
 * Configurações específicas de cada empresa
 * Esta entidade contém todas as configurações que variam por empresa:
 * - Configurações de Email/SMTP
 * - Configurações de Storage
 * - Configurações de Segurança
 * - Configurações de Servidor/Rede
 */
@Entity
@Table(name = "configuracao_empresa")
public class ConfiguracaoEmpresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

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
    private String emailRemetente = "";

    @Column(length = 255)
    private String emailNomeRemetente = "";

    private boolean emailHabilitado = false;

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

    private boolean storageBackupHabilitado = true;

    // ─── Segurança da Empresa ───────────────────────────────────────────
    private int segTempoExpiracaoSessao = 30;

    private boolean segTwoFactorAtivo = false;

    private boolean segRequireUppercase = true;

    private boolean segRequireNumbers = true;

    private boolean segRequireSpecialChars = false;

    private int segComprimentoMinPassword = 8;

    @Column(length = 2000)
    private String segIpWhitelist = "";

    private boolean segLogAcessosAtivo = true;

    // ─── Notificações ────────────────────────────────────────────────────
    private boolean notificacaoEmailHabilitada = true;

    private boolean notificacaoSmsHabilitada = false;

    @Column(length = 20)
    private String notificacaoSmsProvider = "";

    @Column(length = 255)
    private String notificacaoSmsApiKey = "";

    // ─── Integração AGT (Autoridade Geral Tributária) ─────────────────────
    private boolean agtIntegracaoHabilitada = false;

    @Column(length = 500)
    private String agtUrlServico = "";

    @Column(length = 255)
    private String agtUsuario = "";

    @Column(length = 500)
    private String agtSenha = "";

    @Column(length = 50)
    private String agtCertificado = "";

    // ─── Preferências da Empresa ────────────────────────────────────────
    private boolean usarLogotipoEmDocumentos = true;

    private boolean usarCabeçalhoPersonalizadoEmDocumentos = true;

    private boolean usarRodapéPersonalizadoEmDocumentos = false;

    @Column(length = 1000)
    private String rodapePersonalizado = "";

    // ================================================================
    // Getters & Setters
    // ================================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

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

    public boolean isEmailHabilitado() { return emailHabilitado; }
    public void setEmailHabilitado(boolean v) { this.emailHabilitado = v; }

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

    public boolean isStorageBackupHabilitado() { return storageBackupHabilitado; }
    public void setStorageBackupHabilitado(boolean v) { this.storageBackupHabilitado = v; }

    // Segurança
    public int getSegTempoExpiracaoSessao() { return segTempoExpiracaoSessao; }
    public void setSegTempoExpiracaoSessao(int v) { this.segTempoExpiracaoSessao = v; }

    public boolean isSegTwoFactorAtivo() { return segTwoFactorAtivo; }
    public void setSegTwoFactorAtivo(boolean v) { this.segTwoFactorAtivo = v; }

    public boolean isSegRequireUppercase() { return segRequireUppercase; }
    public void setSegRequireUppercase(boolean v) { this.segRequireUppercase = v; }

    public boolean isSegRequireNumbers() { return segRequireNumbers; }
    public void setSegRequireNumbers(boolean v) { this.segRequireNumbers = v; }

    public boolean isSegRequireSpecialChars() { return segRequireSpecialChars; }
    public void setSegRequireSpecialChars(boolean v) { this.segRequireSpecialChars = v; }

    public int getSegComprimentoMinPassword() { return segComprimentoMinPassword; }
    public void setSegComprimentoMinPassword(int v) { this.segComprimentoMinPassword = v; }

    public String getSegIpWhitelist() { return segIpWhitelist; }
    public void setSegIpWhitelist(String v) { this.segIpWhitelist = v; }

    public boolean isSegLogAcessosAtivo() { return segLogAcessosAtivo; }
    public void setSegLogAcessosAtivo(boolean v) { this.segLogAcessosAtivo = v; }

    // Notificações
    public boolean isNotificacaoEmailHabilitada() { return notificacaoEmailHabilitada; }
    public void setNotificacaoEmailHabilitada(boolean v) { this.notificacaoEmailHabilitada = v; }

    public boolean isNotificacaoSmsHabilitada() { return notificacaoSmsHabilitada; }
    public void setNotificacaoSmsHabilitada(boolean v) { this.notificacaoSmsHabilitada = v; }

    public String getNotificacaoSmsProvider() { return notificacaoSmsProvider; }
    public void setNotificacaoSmsProvider(String v) { this.notificacaoSmsProvider = v; }

    public String getNotificacaoSmsApiKey() { return notificacaoSmsApiKey; }
    public void setNotificacaoSmsApiKey(String v) { this.notificacaoSmsApiKey = v; }

    // AGT
    public boolean isAgtIntegracaoHabilitada() { return agtIntegracaoHabilitada; }
    public void setAgtIntegracaoHabilitada(boolean v) { this.agtIntegracaoHabilitada = v; }

    public String getAgtUrlServico() { return agtUrlServico; }
    public void setAgtUrlServico(String v) { this.agtUrlServico = v; }

    public String getAgtUsuario() { return agtUsuario; }
    public void setAgtUsuario(String v) { this.agtUsuario = v; }

    public String getAgtSenha() { return agtSenha; }
    public void setAgtSenha(String v) { this.agtSenha = v; }

    public String getAgtCertificado() { return agtCertificado; }
    public void setAgtCertificado(String v) { this.agtCertificado = v; }

    // Preferências
    public boolean isUsarLogotipoEmDocumentos() { return usarLogotipoEmDocumentos; }
    public void setUsarLogotipoEmDocumentos(boolean v) { this.usarLogotipoEmDocumentos = v; }

    public boolean isUsarCabeçalhoPersonalizadoEmDocumentos() { return usarCabeçalhoPersonalizadoEmDocumentos; }
    public void setUsarCabeçalhoPersonalizadoEmDocumentos(boolean v) { this.usarCabeçalhoPersonalizadoEmDocumentos = v; }

    public boolean isUsarRodapéPersonalizadoEmDocumentos() { return usarRodapéPersonalizadoEmDocumentos; }
    public void setUsarRodapéPersonalizadoEmDocumentos(boolean v) { this.usarRodapéPersonalizadoEmDocumentos = v; }

    public String getRodapePersonalizado() { return rodapePersonalizado; }
    public void setRodapePersonalizado(String v) { this.rodapePersonalizado = v; }
}
