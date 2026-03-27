package ao.co.hzconsultoria.efacturacao.dto;

import java.time.LocalDateTime;

public class MovimentacaoDTO {
    private LocalDateTime data;
    private String descricao;
    private String tipo; // ENTRADA ou SAIDA
    private Double valor;
    private String categoria;

    public MovimentacaoDTO(LocalDateTime data, String descricao, String tipo, Double valor, String categoria) {
        this.data = data;
        this.descricao = descricao;
        this.tipo = tipo;
        this.valor = valor;
        this.categoria = categoria;
    }

    public LocalDateTime getData() {
        return data;
    }

    public void setData(LocalDateTime data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }
}
