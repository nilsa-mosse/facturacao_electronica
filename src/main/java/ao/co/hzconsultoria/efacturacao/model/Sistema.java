package ao.co.hzconsultoria.efacturacao.model;

public class Sistema {
    private String nome;
    private String versao;
    private String emailSuporte;
    private boolean backup;

    // Getters and Setters
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getVersao() {
        return versao;
    }

    public void setVersao(String versao) {
        this.versao = versao;
    }

    public String getEmailSuporte() {
        return emailSuporte;
    }

    public void setEmailSuporte(String emailSuporte) {
        this.emailSuporte = emailSuporte;
    }

    public boolean isBackup() {
        return backup;
    }

    public void setBackup(boolean backup) {
        this.backup = backup;
    }
}