package ao.co.hzconsultoria.efacturacao.dto;

/**
 * DTO para encapsular a resposta da AGT após envio de uma fatura.
 */
public class AgtResponse {

    private boolean sucesso;
    private String codigoAgt;
    private String status;
    private String mensagem;
    private String hashResposta;

    public AgtResponse() {}

    public AgtResponse(boolean sucesso, String codigoAgt, String status, String mensagem) {
        this.sucesso = sucesso;
        this.codigoAgt = codigoAgt;
        this.status = status;
        this.mensagem = mensagem;
    }

    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }

    public String getCodigoAgt() { return codigoAgt; }
    public void setCodigoAgt(String codigoAgt) { this.codigoAgt = codigoAgt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public String getHashResposta() { return hashResposta; }
    public void setHashResposta(String hashResposta) { this.hashResposta = hashResposta; }
}
