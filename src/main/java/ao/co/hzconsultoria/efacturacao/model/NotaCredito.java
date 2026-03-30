package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class NotaCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroNota;
    private LocalDateTime dataEmissao;
    
    @ManyToOne
    private Cliente cliente;

    @ManyToOne
    private Compra faturaOriginal; // Factura a que se refere a rectificação

    @OneToMany(mappedBy = "notaCredito", cascade = CascadeType.ALL)
    private List<ItemNotaCredito> itens;

    private Double totalCredito;
    private String motivo; // Ex: Erro de preço, Devolução parcial, etc.
    
    private String status = "VALIDADA"; // VALIDADA, CANCELADA

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroNota() { return numeroNota; }
    public void setNumeroNota(String numeroNota) { this.numeroNota = numeroNota; }
    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public Compra getFaturaOriginal() { return faturaOriginal; }
    public void setFaturaOriginal(Compra faturaOriginal) { this.faturaOriginal = faturaOriginal; }
    public List<ItemNotaCredito> getItens() { return itens; }
    public void setItens(List<ItemNotaCredito> itens) { this.itens = itens; }
    public Double getTotalCredito() { return totalCredito; }
    public void setTotalCredito(Double totalCredito) { this.totalCredito = totalCredito; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
