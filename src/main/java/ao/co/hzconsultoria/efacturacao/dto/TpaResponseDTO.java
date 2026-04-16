package ao.co.hzconsultoria.efacturacao.dto;

public class TpaResponseDTO {
    private boolean sucesso;
    private String mensagem;
    private String codigoResposta; // "00" para sucesso na ISO 8583
    private String referenciaBorderou;
    private String numCartaoMascarado;
    private String bancoEmissor;

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getCodigoResposta() {
        return codigoResposta;
    }

    public void setCodigoResposta(String codigoResposta) {
        this.codigoResposta = codigoResposta;
    }

    public String getReferenciaBorderou() {
        return referenciaBorderou;
    }

    public void setReferenciaBorderou(String referenciaBorderou) {
        this.referenciaBorderou = referenciaBorderou;
    }

    public String getNumCartaoMascarado() {
        return numCartaoMascarado;
    }

    public void setNumCartaoMascarado(String numCartaoMascarado) {
        this.numCartaoMascarado = numCartaoMascarado;
    }

    public String getBancoEmissor() {
        return bancoEmissor;
    }

    public void setBancoEmissor(String bancoEmissor) {
        this.bancoEmissor = bancoEmissor;
    }
}
