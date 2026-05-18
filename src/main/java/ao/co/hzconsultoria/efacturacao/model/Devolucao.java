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
    private Double iva;
    private String motivo;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private User usuario;

    @ManyToOne
    @JoinColumn(name = "fatura_id")
    private Fatura fatura;

    @OneToOne
    @JoinColumn(name = "nota_credito_id")
    private Fatura notaCredito; // Documento NC gerado

    @OneToMany(mappedBy = "devolucao", cascade = CascadeType.ALL)
    private List<ItemDevolucao> itens;

    @Transient
    private String tipoNota = "NC";

    // Getters e Setters
    public String getTipoNota() { return tipoNota != null ? tipoNota : "NC"; }
    public void setTipoNota(String tipoNota) { this.tipoNota = tipoNota; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getDataDevolucao() { return dataDevolucao; }
    public void setDataDevolucao(LocalDateTime dataDevolucao) { this.dataDevolucao = dataDevolucao; }
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }
    public Fatura getFatura() { return fatura; }
    public void setFatura(Fatura fatura) { this.fatura = fatura; }
    public Fatura getNotaCredito() { return notaCredito; }
    public void setNotaCredito(Fatura notaCredito) { this.notaCredito = notaCredito; }
    public List<ItemDevolucao> getItens() { return itens; }
    public void setItens(List<ItemDevolucao> itens) { this.itens = itens; }
}
