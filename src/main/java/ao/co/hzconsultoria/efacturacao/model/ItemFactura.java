package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
public class ItemFactura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "factura_id")
    private Fatura factura;

    private String produto;
    private Integer quantidade;
    private Double preco;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Fatura getFactura() { return factura; }
    public void setFactura(Fatura factura) { this.factura = factura; }

    public String getProduto() { return produto; }
    public void setProduto(String produto) { this.produto = produto; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Double getPreco() { return preco; }
    public void setPreco(Double preco) { this.preco = preco; }
}
