package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "mesas")
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numeroMesa; // ex: "Mesa 01", "Esplanada 4"

    private String zona; // ex: "Sala Principal", "Esplanada", "VIP"
    private Integer capacidade = 4;

    @Column(nullable = false)
    private String status = "LIVRE"; // LIVRE, OCUPADA, CONTA_PEDIDA, RESERVADA

    private Double totalAcumulado = 0.0;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAbertura;

    private Integer numeroPessoas = 1;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    public Mesa() {}

    public Mesa(String numeroMesa, String zona, Integer capacidade, Empresa empresa) {
        this.numeroMesa = numeroMesa;
        this.zona = zona;
        this.capacidade = capacidade;
        this.empresa = empresa;
        this.status = "LIVRE";
        this.totalAcumulado = 0.0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroMesa() { return numeroMesa; }
    public void setNumeroMesa(String numeroMesa) { this.numeroMesa = numeroMesa; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalAcumulado() { return totalAcumulado; }
    public void setTotalAcumulado(Double totalAcumulado) { this.totalAcumulado = totalAcumulado; }

    public Date getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(Date dataAbertura) { this.dataAbertura = dataAbertura; }

    public Integer getNumeroPessoas() { return numeroPessoas; }
    public void setNumeroPessoas(Integer numeroPessoas) { this.numeroPessoas = numeroPessoas; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}
