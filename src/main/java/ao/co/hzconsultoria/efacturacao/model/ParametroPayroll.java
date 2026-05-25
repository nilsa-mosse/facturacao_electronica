package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "parametro_payroll")
public class ParametroPayroll implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @Column(name = "taxa_inss_trabalhador", nullable = false)
    private double taxaInssTrabalhador = 3.0; // em percentagem, ex: 3.0 para 3%

    @Column(name = "taxa_inss_empresa", nullable = false)
    private double taxaInssEmpresa = 8.0; // em percentagem, ex: 8.0 para 8%

    @Column(name = "desconto_irt_dependente", nullable = false)
    private double descontoIrtDependente = 10.0; // em percentagem, ex: 10.0 para 10%

    @Column(name = "dias_padrao_processamento", nullable = false)
    private int diasPadraoProcessamento = 30;

    public ParametroPayroll() {}

    public ParametroPayroll(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public double getTaxaInssTrabalhador() { return taxaInssTrabalhador; }
    public void setTaxaInssTrabalhador(double taxaInssTrabalhador) { this.taxaInssTrabalhador = taxaInssTrabalhador; }

    public double getTaxaInssEmpresa() { return taxaInssEmpresa; }
    public void setTaxaInssEmpresa(double taxaInssEmpresa) { this.taxaInssEmpresa = taxaInssEmpresa; }

    public double getDescontoIrtDependente() { return descontoIrtDependente; }
    public void setDescontoIrtDependente(double descontoIrtDependente) { this.descontoIrtDependente = descontoIrtDependente; }

    public int getDiasPadraoProcessamento() { return diasPadraoProcessamento; }
    public void setDiasPadraoProcessamento(int diasPadraoProcessamento) { this.diasPadraoProcessamento = diasPadraoProcessamento; }
}
