package ao.co.hzconsultoria.efacturacao.dto;

public class EstoqueDTO {
    private Long estabelecimentoId;
    private String nome;
    private String endereco;
    private String tipo;
    private Double quantidade;
    private Boolean visivel;

    public EstoqueDTO() {}

    public EstoqueDTO(Long estabelecimentoId, String nome, String endereco, String tipo, Double quantidade, Boolean visivel) {
        this.estabelecimentoId = estabelecimentoId;
        this.nome = nome;
        this.endereco = endereco;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.visivel = visivel;
    }

    public Long getEstabelecimentoId() { return estabelecimentoId; }
    public void setEstabelecimentoId(Long estabelecimentoId) { this.estabelecimentoId = estabelecimentoId; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }

    public Boolean getVisivel() { return visivel; }
    public void setVisivel(Boolean visivel) { this.visivel = visivel; }
}
