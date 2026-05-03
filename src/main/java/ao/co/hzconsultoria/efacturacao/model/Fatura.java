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
    private Boolean enviada_agt = false;
    private String status;
    private Double total;
    private Double iva;
    private String hash;
    private String hashControl; // Versão da chave privada
    private String previousHash; // Hash do documento anterior
    private Date systemEntryDate; // Data de gravação no sistema
    private String invoiceStatus; // N - Normal, A - Anulada, S - Auto-faturação
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

    public Boolean isEnviadaAGT() { return enviada_agt; }
    public void setEnviadaAGT(Boolean enviadaAGT) { this.enviada_agt = enviadaAGT; }

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

    public String getHashControl() { return hashControl; }
    public void setHashControl(String hashControl) { this.hashControl = hashControl; }

    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }

    public Date getSystemEntryDate() { return systemEntryDate; }
    public void setSystemEntryDate(Date systemEntryDate) { this.systemEntryDate = systemEntryDate; }

    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
}