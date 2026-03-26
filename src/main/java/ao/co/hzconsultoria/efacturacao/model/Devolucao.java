package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Devolucao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime dataDevolucao;
    private Double total;
    @OneToMany(mappedBy = "devolucao", cascade = CascadeType.ALL)
    private List<ItemDevolucao> itens;
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(LocalDateTime dataDevolucao) { this.dataDevolucao = dataDevolucao; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public List<ItemDevolucao> getItens() { return itens; }
    public void setItens(List<ItemDevolucao> itens) { this.itens = itens; }
}
