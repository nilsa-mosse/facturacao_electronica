package ao.co.hzconsultoria.efacturacao.model;

public class ConfiguracaoEmail {

    private String smtpHost = "smtp.gmail.com";
    private int smtpPorta = 587;
    private String smtpUsername = "";
    private String smtpPassword = "";
    private String segurancaTipo = "TLS"; // TLS, SSL, NONE
    private String emailRemetente = "noreply@empresa.ao";
    private String nomeRemetente = "Sistema de Facturação";

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public int getSmtpPorta() { return smtpPorta; }
    public void setSmtpPorta(int smtpPorta) { this.smtpPorta = smtpPorta; }

    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }

    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }

    public String getSegurancaTipo() { return segurancaTipo; }
    public void setSegurancaTipo(String segurancaTipo) { this.segurancaTipo = segurancaTipo; }

    public String getEmailRemetente() { return emailRemetente; }
    public void setEmailRemetente(String emailRemetente) { this.emailRemetente = emailRemetente; }

    public String getNomeRemetente() { return nomeRemetente; }
    public void setNomeRemetente(String nomeRemetente) { this.nomeRemetente = nomeRemetente; }
}
