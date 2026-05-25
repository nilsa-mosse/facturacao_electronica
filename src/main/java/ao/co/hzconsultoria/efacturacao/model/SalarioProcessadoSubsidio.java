package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "salario_processado_subsidio")
public class SalarioProcessadoSubsidio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salario_processado_id", nullable = false)
    private SalarioProcessado salarioProcessado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subsidio_id", nullable = false)
    private Subsidio subsidio;

    // Valor efectivamente calculado neste mês para este subsídio
    @Column(nullable = false)
    private double valor;

    public SalarioProcessadoSubsidio() {}

    public SalarioProcessadoSubsidio(SalarioProcessado sp, Subsidio subsidio, double valor) {
        this.salarioProcessado = sp;
        this.subsidio = subsidio;
        this.valor = valor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SalarioProcessado getSalarioProcessado() { return salarioProcessado; }
    public void setSalarioProcessado(SalarioProcessado sp) { this.salarioProcessado = sp; }

    public Subsidio getSubsidio() { return subsidio; }
    public void setSubsidio(Subsidio subsidio) { this.subsidio = subsidio; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}
