package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "caixa")
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "operador_id", nullable = false)
    private User operador;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "estabelecimento_id")
    private Estabelecimento estabelecimento;

    private LocalDateTime dataAbertura;
    private LocalDateTime dataFecho;

    private Double saldoInicial = 0.0;
    private Double saldoFinal = 0.0;
    private Double quebraCaixa = 0.0; // Diferença informada pelo operador vs sistema

    private Double totalFaturado = 0.0;
    private Double totalNumerario = 0.0;
    private Double totalMulticaixa = 0.0;

    private String estado; // ABERTO, FECHADO
    private String observacoes;

    // Construtores
    public Caixa() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getOperador() { return operador; }
    public void setOperador(User operador) { this.operador = operador; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Estabelecimento getEstabelecimento() { return estabelecimento; }
    public void setEstabelecimento(Estabelecimento estabelecimento) { this.estabelecimento = estabelecimento; }
    public LocalDateTime getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDateTime dataAbertura) { this.dataAbertura = dataAbertura; }
    public LocalDateTime getDataFecho() { return dataFecho; }
    public void setDataFecho(LocalDateTime dataFecho) { this.dataFecho = dataFecho; }
    public Double getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(Double saldoInicial) { this.saldoInicial = saldoInicial; }
    public Double getSaldoFinal() { return saldoFinal; }
    public void setSaldoFinal(Double saldoFinal) { this.saldoFinal = saldoFinal; }
    public Double getQuebraCaixa() { return quebraCaixa; }
    public void setQuebraCaixa(Double quebraCaixa) { this.quebraCaixa = quebraCaixa; }
    public Double getTotalFaturado() { return totalFaturado; }
    public void setTotalFaturado(Double totalFaturado) { this.totalFaturado = totalFaturado; }
    public Double getTotalNumerario() { return totalNumerario; }
    public void setTotalNumerario(Double totalNumerario) { this.totalNumerario = totalNumerario; }
    public Double getTotalMulticaixa() { return totalMulticaixa; }
    public void setTotalMulticaixa(Double totalMulticaixa) { this.totalMulticaixa = totalMulticaixa; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
