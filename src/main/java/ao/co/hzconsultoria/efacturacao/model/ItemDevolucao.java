package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
public class ItemDevolucao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private Integer quantidade;
    private Double preco;
    private Double subtotal;

    private Double ivaPercentual;
    private Double ivaValor;

    @ManyToOne
    @JoinColumn(name = "devolucao_id")
    private Devolucao devolucao;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public Double getPreco() { return preco; }
    public void setPreco(Double preco) { this.preco = preco; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public Double getIvaPercentual() { return ivaPercentual; }
    public void setIvaPercentual(Double ivaPercentual) { this.ivaPercentual = ivaPercentual; }
    public Double getIvaValor() { return ivaValor; }
    public void setIvaValor(Double ivaValor) { this.ivaValor = ivaValor; }
    public Devolucao getDevolucao() { return devolucao; }
    public void setDevolucao(Devolucao devolucao) { this.devolucao = devolucao; }
}
