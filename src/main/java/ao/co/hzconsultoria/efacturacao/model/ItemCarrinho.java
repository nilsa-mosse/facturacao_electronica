package ao.co.hzconsultoria.efacturacao.model;

public class ItemCarrinho {
    private Produto produto;
    private int quantidade;
    private double precoTotal;

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public double getPrecoTotal() {
        return precoTotal;
    }

    public void setPrecoTotal(double precoTotal) {
        this.precoTotal = precoTotal;
    }

    public String getNome() {
        return produto != null ? produto.getNome() : null;
    }

    public double getPreco() {
        return produto != null ? produto.getPreco() : 0.0;
    }
}