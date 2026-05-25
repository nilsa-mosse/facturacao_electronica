package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "subsidio")
public class Subsidio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    // Código contabilístico do subsídio (ex: "101", "102")
    private String codigo;

    // Limite de isenção de INSS (0 = totalmente sujeito, ex: 30000 para alimentação/transporte)
    @Column(name = "limite_isencao_inss")
    private double limiteIsencaoInss;

    // Limite de isenção de IRT (0 = totalmente sujeito)
    @Column(name = "limite_isencao_irt")
    private double limiteIsencaoIrt;

    // Indica se o subsídio é tributável no total para IRT
    @Column(name = "sujeito_irt")
    private boolean sujeitoIrt = true;

    // Indica se o subsídio é tributável no total para INSS
    @Column(name = "sujeito_inss")
    private boolean sujeitoInss = true;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    public Subsidio() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public double getLimiteIsencaoInss() { return limiteIsencaoInss; }
    public void setLimiteIsencaoInss(double limiteIsencaoInss) { this.limiteIsencaoInss = limiteIsencaoInss; }

    public double getLimiteIsencaoIrt() { return limiteIsencaoIrt; }
    public void setLimiteIsencaoIrt(double limiteIsencaoIrt) { this.limiteIsencaoIrt = limiteIsencaoIrt; }

    public boolean isSujeitoIrt() { return sujeitoIrt; }
    public void setSujeitoIrt(boolean sujeitoIrt) { this.sujeitoIrt = sujeitoIrt; }

    public boolean isSujeitoInss() { return sujeitoInss; }
    public void setSujeitoInss(boolean sujeitoInss) { this.sujeitoInss = sujeitoInss; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}
