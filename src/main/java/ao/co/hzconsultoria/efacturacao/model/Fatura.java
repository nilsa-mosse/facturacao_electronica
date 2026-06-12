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
    private Date dataVencimento;
    private Boolean enviada_agt = false;
    private String status;
    private Double total;
    private Double iva;
    private Double valorPago = 0.0; // Total pago até ao momento
    private Double valorEmAberto = 0.0; // Total em aberto
    private String hash;
    private String hashControl; // Versão da chave privada
    private String previousHash; // Hash do documento anterior
    private Date systemEntryDate; // Data de gravação no sistema
    private String invoiceStatus; // N - Normal, A - Anulada, S - Auto-faturação
    private String codigoAgt;
    private String tipoDocumento; // FT, FR, FP (Pro-forma), NC (Nota Crédito), ND (Nota Débito)
    private Boolean validadaAgt = false; // Flag para indicar que foi validada e é imutável
    private Boolean impresso = false;
    private Date dataImpressao;
    private Date dataEmail;
    private Boolean emailEnviado = false;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;
    
    @ManyToOne
    @JoinColumn(name = "fatura_referencia_id")
    private Fatura faturaReferencia; // Referência para NC ou ND

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

    // ...existing getters and setters...

    public String getInvoiceStatus() { return invoiceStatus; }
    public void setInvoiceStatus(String invoiceStatus) { this.invoiceStatus = invoiceStatus; }
    
    public Date getDataVencimento() { return dataVencimento; }
    public void setDataVencimento(Date dataVencimento) { this.dataVencimento = dataVencimento; }
    
    public Double getValorPago() { return valorPago; }
    public void setValorPago(Double valorPago) { this.valorPago = valorPago; }
    
    public Double getValorEmAberto() { return valorEmAberto; }
    public void setValorEmAberto(Double valorEmAberto) { this.valorEmAberto = valorEmAberto; }
    
    public Boolean getValidadaAgt() { return validadaAgt; }
    public void setValidadaAgt(Boolean validadaAgt) { this.validadaAgt = validadaAgt; }
    
    public Boolean getImpresso() { return impresso; }
    public void setImpresso(Boolean impresso) { this.impresso = impresso; }
    
    public Date getDataImpressao() { return dataImpressao; }
    public void setDataImpressao(Date dataImpressao) { this.dataImpressao = dataImpressao; }
    
    public Date getDataEmail() { return dataEmail; }
    public void setDataEmail(Date dataEmail) { this.dataEmail = dataEmail; }
    
    public Boolean getEmailEnviado() { return emailEnviado; }
    public void setEmailEnviado(Boolean emailEnviado) { this.emailEnviado = emailEnviado; }
    
    public Fatura getFaturaReferencia() { return faturaReferencia; }
    public void setFaturaReferencia(Fatura faturaReferencia) { this.faturaReferencia = faturaReferencia; }
}