package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
public class ItemNotaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeProduto;
    private Double quantidade;
    private Double precoUnitario;
    private Double subtotal;

    @ManyToOne
    private NotaCredito notaCredito;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }
    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }
    public Double getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(Double precoUnitario) { this.precoUnitario = precoUnitario; }
    public Double getSubtotal() { return subtotal; }
    public void setSubtotal(Double subtotal) { this.subtotal = subtotal; }
    public NotaCredito getNotaCredito() { return notaCredito; }
    public void setNotaCredito(NotaCredito notaCredito) { this.notaCredito = notaCredito; }
}
