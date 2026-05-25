package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "escalao_irt")
public class EscalaoIrt implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "limite_inferior", nullable = false)
    private double limiteInferior;

    @Column(name = "limite_superior")
    private Double limiteSuperior; // null se for o último escalão (sem limite)

    @Column(name = "parcela_fixa", nullable = false)
    private double parcelaFixa;

    @Column(name = "taxa_excesso", nullable = false)
    private double taxaExcesso; // em percentagem, ex: 10.0 para 10% (0.10)

    public EscalaoIrt() {}

    public EscalaoIrt(Empresa empresa, double limiteInferior, Double limiteSuperior, double parcelaFixa, double taxaExcesso) {
        this.empresa = empresa;
        this.limiteInferior = limiteInferior;
        this.limiteSuperior = limiteSuperior;
        this.parcelaFixa = parcelaFixa;
        this.taxaExcesso = taxaExcesso;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public double getLimiteInferior() { return limiteInferior; }
    public void setLimiteInferior(double limiteInferior) { this.limiteInferior = limiteInferior; }

    public Double getLimiteSuperior() { return limiteSuperior; }
    public void setLimiteSuperior(Double limiteSuperior) { this.limiteSuperior = limiteSuperior; }

    public double getParcelaFixa() { return parcelaFixa; }
    public void setParcelaFixa(double parcelaFixa) { this.parcelaFixa = parcelaFixa; }

    public double getTaxaExcesso() { return taxaExcesso; }
    public void setTaxaExcesso(double taxaExcesso) { this.taxaExcesso = taxaExcesso; }
}
