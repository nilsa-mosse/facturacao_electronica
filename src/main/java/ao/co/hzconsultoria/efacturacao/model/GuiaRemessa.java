package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class GuiaRemessa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroGuia;
    private LocalDateTime dataEmissao;
    
    @ManyToOne
    private Cliente cliente;

    @OneToMany(mappedBy = "guiaRemessa", cascade = CascadeType.ALL)
    private List<ItemGuiaRemessa> itens;

    private String localCarga;
    private String localDescarga;
    private String matriculaViatura;
    private String motorista;
    
    @ManyToOne
    private Compra faturaOrigem; // Vínculo opcional com a fatura

    private String status = "ATIVA"; // ATIVA, FECHADA, ANULADA, SUBSTITUIDA
    private String motivoAnulacao;

    @ManyToOne
    private GuiaRemessa guiaReferencia; // Referência para a guia original em caso de substituição

    private String trackingStatus = "EM_PROCESSAMENTO"; // EM_PROCESSAMENTO, EM_TRANSITO, ENTREGUE, RETORNADO

    @OneToMany(mappedBy = "guiaRemessa", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<EventoTracking> eventosTracking;

    @Column(length = 255)
    private String hashAgt;

    @Column(length = 100)
    private String codigoValidacao;

    private LocalDateTime dataValidacaoAgt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(String numeroGuia) { this.numeroGuia = numeroGuia; }
    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public List<ItemGuiaRemessa> getItens() { return itens; }
    public void setItens(List<ItemGuiaRemessa> itens) { this.itens = itens; }
    public String getLocalCarga() { return localCarga; }
    public void setLocalCarga(String localCarga) { this.localCarga = localCarga; }
    public String getLocalDescarga() { return localDescarga; }
    public void setLocalDescarga(String localDescarga) { this.localDescarga = localDescarga; }
    public String getMatriculaViatura() { return matriculaViatura; }
    public void setMatriculaViatura(String matriculaViatura) { this.matriculaViatura = matriculaViatura; }
    public String getMotorista() { return motorista; }
    public void setMotorista(String motorista) { this.motorista = motorista; }
    public Compra getFaturaOrigem() { return faturaOrigem; }
    public void setFaturaOrigem(Compra faturaOrigem) { this.faturaOrigem = faturaOrigem; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMotivoAnulacao() { return motivoAnulacao; }
    public void setMotivoAnulacao(String motivoAnulacao) { this.motivoAnulacao = motivoAnulacao; }

    public GuiaRemessa getGuiaReferencia() { return guiaReferencia; }
    public void setGuiaReferencia(GuiaRemessa guiaReferencia) { this.guiaReferencia = guiaReferencia; }

    public String getTrackingStatus() { return trackingStatus; }
    public void setTrackingStatus(String trackingStatus) { this.trackingStatus = trackingStatus; }

    public java.util.List<EventoTracking> getEventosTracking() { return eventosTracking; }
    public void setEventosTracking(java.util.List<EventoTracking> eventosTracking) { this.eventosTracking = eventosTracking; }

    public String getHashAgt() { return hashAgt; }
    public void setHashAgt(String hashAgt) { this.hashAgt = hashAgt; }

    public String getCodigoValidacao() { return codigoValidacao; }
    public void setCodigoValidacao(String codigoValidacao) { this.codigoValidacao = codigoValidacao; }

    public LocalDateTime getDataValidacaoAgt() { return dataValidacaoAgt; }
    public void setDataValidacaoAgt(LocalDateTime dataValidacaoAgt) { this.dataValidacaoAgt = dataValidacaoAgt; }
}
