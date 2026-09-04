package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "itens_pedido_cozinha")
public class ItemPedidoCozinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    private String nomeProduto;
    private Double quantidade = 1.0;
    private String observacao; // ex: "Bem passado", "Sem cebola"

    private String status = "PENDENTE"; // PENDENTE, PRONTO

    public ItemPedidoCozinha() {}

    public ItemPedidoCozinha(Produto produto, String nomeProduto, Double quantidade, String observacao) {
        this.produto = produto;
        this.nomeProduto = nomeProduto;
        this.quantidade = quantidade;
        this.observacao = observacao;
        this.status = "PENDENTE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }

    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
