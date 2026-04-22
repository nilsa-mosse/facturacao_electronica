package ao.co.hzconsultoria.efacturacao.model;

public class ConfiguracaoSeguranca {

    private int tempoExpiracaoSessao = 30;      // minutos
    private int tentativasLoginMax = 5;
    private int lockoutDuracao = 15;            // minutos de bloqueio após tentativas falhadas
    private String politicaPassword = "MEDIA"; // FRACA, MEDIA, FORTE
    private int comprimentoMinPassword = 8;
    private boolean twoFactorAtivo = false;
    private boolean requireUppercase = true;
    private boolean requireNumbers = true;
    private boolean requireSpecialChars = false;
    private String ipWhitelist = "";            // IPs separados por vírgula; vazio = todos permitidos
    private boolean logAcessosAtivo = true;

    public int getTempoExpiracaoSessao() { return tempoExpiracaoSessao; }
    public void setTempoExpiracaoSessao(int tempoExpiracaoSessao) { this.tempoExpiracaoSessao = tempoExpiracaoSessao; }

    public int getTentativasLoginMax() { return tentativasLoginMax; }
    public void setTentativasLoginMax(int tentativasLoginMax) { this.tentativasLoginMax = tentativasLoginMax; }

    public int getLockoutDuracao() { return lockoutDuracao; }
    public void setLockoutDuracao(int lockoutDuracao) { this.lockoutDuracao = lockoutDuracao; }

    public String getPoliticaPassword() { return politicaPassword; }
    public void setPoliticaPassword(String politicaPassword) { this.politicaPassword = politicaPassword; }

    public int getComprimentoMinPassword() { return comprimentoMinPassword; }
    public void setComprimentoMinPassword(int comprimentoMinPassword) { this.comprimentoMinPassword = comprimentoMinPassword; }

    public boolean isTwoFactorAtivo() { return twoFactorAtivo; }
    public void setTwoFactorAtivo(boolean twoFactorAtivo) { this.twoFactorAtivo = twoFactorAtivo; }

    public boolean isRequireUppercase() { return requireUppercase; }
    public void setRequireUppercase(boolean requireUppercase) { this.requireUppercase = requireUppercase; }

    public boolean isRequireNumbers() { return requireNumbers; }
    public void setRequireNumbers(boolean requireNumbers) { this.requireNumbers = requireNumbers; }

    public boolean isRequireSpecialChars() { return requireSpecialChars; }
    public void setRequireSpecialChars(boolean requireSpecialChars) { this.requireSpecialChars = requireSpecialChars; }

    public String getIpWhitelist() { return ipWhitelist; }
    public void setIpWhitelist(String ipWhitelist) { this.ipWhitelist = ipWhitelist; }

    public boolean isLogAcessosAtivo() { return logAcessosAtivo; }
    public void setLogAcessosAtivo(boolean logAcessosAtivo) { this.logAcessosAtivo = logAcessosAtivo; }
}
