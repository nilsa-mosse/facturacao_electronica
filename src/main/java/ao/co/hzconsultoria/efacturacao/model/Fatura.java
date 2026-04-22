package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Fatura {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Compra compra;

    private String numeroFatura;
    private Date dataEmissao;
    private boolean enviada_agt;
    private String status;
    private Double total;
    private Double iva;
    private String hash;
    private String codigoAgt;
    private String tipoDocumento; // FT, FR, FP (Pro-forma)

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }

    public String getNumeroFatura() { return numeroFatura; }
    public void setNumeroFatura(String numeroFatura) { this.numeroFatura = numeroFatura; }

    public Date getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(Date dataEmissao) { this.dataEmissao = dataEmissao; }

    public boolean isEnviadaAGT() { return enviada_agt; }
    public void setEnviadaAGT(boolean enviadaAGT) { this.enviada_agt = enviadaAGT; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public Double getIva() { return iva; }
    public void setIva(Double iva) { this.iva = iva; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }

    public String getCodigoAgt() { return codigoAgt; }
    public void setCodigoAgt(String codigoAgt) { this.codigoAgt = codigoAgt; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
}