package ao.co.hzconsultoria.efacturacao.dto;

public class TpaRequestDTO {
    private Double valor;
    private String tipoOperacao; // COMPRA, DEVOLUCAO
    private String idVenda; // Uma referência como ID do carrinho

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public String getTipoOperacao() {
        return tipoOperacao;
    }

    public void setTipoOperacao(String tipoOperacao) {
        this.tipoOperacao = tipoOperacao;
    }

    public String getIdVenda() {
        return idVenda;
    }

    public void setIdVenda(String idVenda) {
        this.idVenda = idVenda;
    }
}
