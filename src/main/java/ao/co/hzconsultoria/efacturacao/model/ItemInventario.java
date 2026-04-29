package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "itens_inventario")
public class ItemInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "inventario_id", nullable = false)
    private Inventario inventario;

    @ManyToOne
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    private Double quantidadeSistema = 0.0;
    private Double quantidadeContada = 0.0;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Inventario getInventario() { return inventario; }
    public void setInventario(Inventario inventario) { this.inventario = inventario; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Double getQuantidadeSistema() { return quantidadeSistema; }
    public void setQuantidadeSistema(Double quantidadeSistema) { this.quantidadeSistema = quantidadeSistema; }

    public Double getQuantidadeContada() { return quantidadeContada; }
    public void setQuantidadeContada(Double quantidadeContada) { this.quantidadeContada = quantidadeContada; }

    // Helper methods for calculations
    public Double getDivergencia() {
        return (quantidadeContada != null ? quantidadeContada : 0.0) - (quantidadeSistema != null ? quantidadeSistema : 0.0);
    }

    public Double getValorDivergencia() {
        double preco = (produto != null && produto.getPrecoCompra() != null) ? produto.getPrecoCompra() : 0.0;
        return getDivergencia() * preco;
    }
}
