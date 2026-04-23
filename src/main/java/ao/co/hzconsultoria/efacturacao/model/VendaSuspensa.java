package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "venda_suspensa")
public class VendaSuspensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dataHora;

    private String clienteNome;

    @Column(columnDefinition = "TEXT")
    private String itensJson;

    @ManyToOne
    private Empresa empresa;

    @ManyToOne
    private User operador;

    public VendaSuspensa() {
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public void setClienteNome(String clienteNome) {
        this.clienteNome = clienteNome;
    }

    public String getItensJson() {
        return itensJson;
    }

    public void setItensJson(String itensJson) {
        this.itensJson = itensJson;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public User getOperador() {
        return operador;
    }

    public void setOperador(User operador) {
        this.operador = operador;
    }
}
