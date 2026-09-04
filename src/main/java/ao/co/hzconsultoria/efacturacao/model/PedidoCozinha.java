package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "pedidos_cozinha")
public class PedidoCozinha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroPedido; // ex: "KDS-001"

    @ManyToOne
    @JoinColumn(name = "mesa_id")
    private Mesa mesa;

    @Column(nullable = false)
    private String status = "PENDENTE"; // PENDENTE, EM_PREPARACAO, PRONTO, ENTREGUE, CANCELADO

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataHora = new Date();

    private String observacoes;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_cozinha_id")
    private List<ItemPedidoCozinha> itens = new ArrayList<>();

    public PedidoCozinha() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public Mesa getMesa() { return mesa; }
    public void setMesa(Mesa mesa) { this.mesa = mesa; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getDataHora() { return dataHora; }
    public void setDataHora(Date dataHora) { this.dataHora = dataHora; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public List<ItemPedidoCozinha> getItens() { return itens; }
    public void setItens(List<ItemPedidoCozinha> itens) { this.itens = itens; }
}
