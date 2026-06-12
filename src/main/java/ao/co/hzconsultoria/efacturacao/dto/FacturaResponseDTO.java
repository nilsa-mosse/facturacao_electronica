package ao.co.hzconsultoria.efacturacao.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO padrão para respostas da API de Facturas
 */
public class FacturaResponseDTO {

    private String status; // "sucesso", "erro", "aviso"
    private String mensagem;
    private Object dados;
    private long timestamp;
    private Map<String, String> erros;

    public FacturaResponseDTO() {
        this.timestamp = System.currentTimeMillis();
        this.erros = new HashMap<>();
    }

    // Construtores

    public FacturaResponseDTO(String status, String mensagem) {
        this();
        this.status = status;
        this.mensagem = mensagem;
    }

    public FacturaResponseDTO(String status, String mensagem, Object dados) {
        this();
        this.status = status;
        this.mensagem = mensagem;
        this.dados = dados;
    }

    // Métodos estáticos para facilitar criação

    public static FacturaResponseDTO sucesso(String mensagem) {
        return new FacturaResponseDTO("sucesso", mensagem);
    }

    public static FacturaResponseDTO sucesso(String mensagem, Object dados) {
        return new FacturaResponseDTO("sucesso", mensagem, dados);
    }

    public static FacturaResponseDTO erro(String mensagem) {
        return new FacturaResponseDTO("erro", mensagem);
    }

    public static FacturaResponseDTO erro(String mensagem, Map<String, String> erros) {
        FacturaResponseDTO resp = new FacturaResponseDTO("erro", mensagem);
        resp.erros = erros;
        return resp;
    }

    public static FacturaResponseDTO aviso(String mensagem) {
        return new FacturaResponseDTO("aviso", mensagem);
    }

    // Getters e Setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public Object getDados() {
        return dados;
    }

    public void setDados(Object dados) {
        this.dados = dados;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getErros() {
        return erros;
    }

    public void setErros(Map<String, String> erros) {
        this.erros = erros;
    }

    public void adicionarErro(String campo, String mensagem) {
        this.erros.put(campo, mensagem);
    }
}
