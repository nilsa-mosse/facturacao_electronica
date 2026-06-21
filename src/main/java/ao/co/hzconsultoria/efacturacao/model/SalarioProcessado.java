package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.util.Set;
import java.util.LinkedHashSet;

@Entity
@Table(name = "salario_processado")
public class SalarioProcessado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "folha_id")
    private FolhaProcessamento folhaProcessamento;

    @ManyToOne
    @JoinColumn(name = "colaborador_id")
    private Colaborador colaborador;

    @Column(name = "salario_base")
    private double salarioBase;

    // Subsídio de Férias e Natal mantêm-se como campos de fórmula (calculados
    // internamente)
    @Column(name = "subsidio_ferias")
    private double subsidioFerias;

    @Column(name = "subsidio_natal")
    private double subsidioNatal;

    @Column(name = "rendimento_iliquido")
    private double rendimentoIliquido;

    @Column(name = "desconto_seguranca_social")
    private double descontoSegurancaSocial;

    @Column(name = "desconto_irt")
    private double descontoIrt;

    @Column(name = "encargo_empresa_seguranca_social")
    private double encargoEmpresaSegurancaSocial;

    @Column(name = "salario_liquido")
    private double salarioLiquido;

    // Subsídios dinâmicos calculados para este mês (snapshot histórico)
    @OneToMany(mappedBy = "salarioProcessado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<SalarioProcessadoSubsidio> subsidios = new LinkedHashSet<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FolhaProcessamento getFolhaProcessamento() {
        return folhaProcessamento;
    }

    public void setFolhaProcessamento(FolhaProcessamento folhaProcessamento) {
        this.folhaProcessamento = folhaProcessamento;
    }

    public Colaborador getColaborador() {
        return colaborador;
    }

    public void setColaborador(Colaborador colaborador) {
        this.colaborador = colaborador;
    }

    public double getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(double salarioBase) {
        this.salarioBase = salarioBase;
    }

    public double getSubsidioFerias() {
        return subsidioFerias;
    }

    public void setSubsidioFerias(double subsidioFerias) {
        this.subsidioFerias = subsidioFerias;
    }

    public double getSubsidioNatal() {
        return subsidioNatal;
    }

    public void setSubsidioNatal(double subsidioNatal) {
        this.subsidioNatal = subsidioNatal;
    }

    public double getRendimentoIliquido() {
        return rendimentoIliquido;
    }

    public void setRendimentoIliquido(double rendimentoIliquido) {
        this.rendimentoIliquido = rendimentoIliquido;
    }

    public double getDescontoSegurancaSocial() {
        return descontoSegurancaSocial;
    }

    public void setDescontoSegurancaSocial(double d) {
        this.descontoSegurancaSocial = d;
    }

    public double getDescontoIrt() {
        return descontoIrt;
    }

    public void setDescontoIrt(double descontoIrt) {
        this.descontoIrt = descontoIrt;
    }

    public double getEncargoEmpresaSegurancaSocial() {
        return encargoEmpresaSegurancaSocial;
    }

    public void setEncargoEmpresaSegurancaSocial(double v) {
        this.encargoEmpresaSegurancaSocial = v;
    }

    public double getSalarioLiquido() {
        return salarioLiquido;
    }

    public void setSalarioLiquido(double salarioLiquido) {
        this.salarioLiquido = salarioLiquido;
    }

    public Set<SalarioProcessadoSubsidio> getSubsidios() {
        return subsidios;
    }

    public void setSubsidios(Set<SalarioProcessadoSubsidio> subsidios) {
        this.subsidios = subsidios;
    }
}
