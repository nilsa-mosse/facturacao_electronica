package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dataCompra;

    private Double total;

    @ManyToOne
    private Cliente cliente;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL)
    private List<ItemCompra> itens;

    private String status = "EMITIDA"; // EMITIDA, CANCELADA, SUBSTITUIDA
    private String tipoDocumento; // FT, FR, FP

    // Dados do cliente para a factura (pode ser FK ou Consumidor Final)
    private String nomeCliente;
    private String nifCliente;
    private String formaPagamento; // CASH, TPA, TRANSFERENCIA, CASH+TPA, etc.

    // Detalhes MULTICAIXA (Angola)
    private Double valorPagoCash;
    private Double valorPagoMulticaixa;
    private String bancoMulticaixa; // BFA, BAI, BCI, etc.
    private String referenciaMulticaixa; // Número do borderô/transação
    private Double comissaoMulticaixa; // Valor da taxa (padrão 1.5%)
    private Double valorLiquidoMulticaixa; // Valor líquido que entra na conta

    // Campos transientes para transporte (Guia de Remessa no POS)
    @Transient
    private String motorista;
    @Transient
    private String matriculaViatura;
    @Transient
    private String localCarga;
    @Transient
    private String localDescarga;

    private String motivoAnulacao;

    @ManyToOne(fetch = FetchType.LAZY)
    private Compra faturaReferencia;

    // Getters and Setters
    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataCompra() {
        return dataCompra;
    }

    public void setDataCompra(LocalDateTime dataCompra) {
        this.dataCompra = dataCompra;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<ItemCompra> getItens() {
        return itens;
    }

    public void setItens(List<ItemCompra> itens) {
        this.itens = itens;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getNifCliente() {
        return nifCliente;
    }

    public void setNifCliente(String nifCliente) {
        this.nifCliente = nifCliente;
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public Double getValorPagoCash() {
        return valorPagoCash;
    }

    public void setValorPagoCash(Double valorPagoCash) {
        this.valorPagoCash = valorPagoCash;
    }

    public Double getValorPagoMulticaixa() {
        return valorPagoMulticaixa;
    }

    public void setValorPagoMulticaixa(Double valorPagoMulticaixa) {
        this.valorPagoMulticaixa = valorPagoMulticaixa;
    }

    public String getBancoMulticaixa() {
        return bancoMulticaixa;
    }

    public void setBancoMulticaixa(String bancoMulticaixa) {
        this.bancoMulticaixa = bancoMulticaixa;
    }

    public String getReferenciaMulticaixa() {
        return referenciaMulticaixa;
    }

    public void setReferenciaMulticaixa(String referenciaMulticaixa) {
        this.referenciaMulticaixa = referenciaMulticaixa;
    }

    public Double getComissaoMulticaixa() {
        return comissaoMulticaixa;
    }

    public void setComissaoMulticaixa(Double comissaoMulticaixa) {
        this.comissaoMulticaixa = comissaoMulticaixa;
    }

    public Double getValorLiquidoMulticaixa() {
        return valorLiquidoMulticaixa;
    }

    public void setValorLiquidoMulticaixa(Double valorLiquidoMulticaixa) {
        this.valorLiquidoMulticaixa = valorLiquidoMulticaixa;
    }

    public String getMotorista() {
        return motorista;
    }

    public void setMotorista(String motorista) {
        this.motorista = motorista;
    }

    public String getMatriculaViatura() {
        return matriculaViatura;
    }

    public void setMatriculaViatura(String matriculaViatura) {
        this.matriculaViatura = matriculaViatura;
    }

    public String getLocalCarga() {
        return localCarga;
    }

    public void setLocalCarga(String localCarga) {
        this.localCarga = localCarga;
    }

    public String getLocalDescarga() {
        return localDescarga;
    }

    public void setLocalDescarga(String localDescarga) {
        this.localDescarga = localDescarga;
    }

    public String getMotivoAnulacao() {
        return motivoAnulacao;
    }

    public void setMotivoAnulacao(String motivoAnulacao) {
        this.motivoAnulacao = motivoAnulacao;
    }

    public Compra getFaturaReferencia() {
        return faturaReferencia;
    }

    public void setFaturaReferencia(Compra faturaReferencia) {
        this.faturaReferencia = faturaReferencia;
    }
}