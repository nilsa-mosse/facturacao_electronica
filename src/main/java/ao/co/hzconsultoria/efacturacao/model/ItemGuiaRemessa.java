package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class ItemGuiaRemessa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomeProduto;
    private Double quantidade;
    private String unidadeMedida;

    @ManyToOne
    @JsonIgnore
    private GuiaRemessa guiaRemessa;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }
    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; }
    public String getUnidadeMedida() { return unidadeMedida; }
    public void setUnidadeMedida(String unidadeMedida) { this.unidadeMedida = unidadeMedida; }
    public GuiaRemessa getGuiaRemessa() { return guiaRemessa; }
    public void setGuiaRemessa(GuiaRemessa guiaRemessa) { this.guiaRemessa = guiaRemessa; }
}
