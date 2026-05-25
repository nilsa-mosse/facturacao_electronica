package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "colaborador_subsidio")
public class ColaboradorSubsidio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "colaborador_id", nullable = false)
    private Colaborador colaborador;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subsidio_id", nullable = false)
    private Subsidio subsidio;

    @Column(nullable = false)
    private double valor;

    public ColaboradorSubsidio() {}

    public ColaboradorSubsidio(Colaborador colaborador, Subsidio subsidio, double valor) {
        this.colaborador = colaborador;
        this.subsidio = subsidio;
        this.valor = valor;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Colaborador getColaborador() { return colaborador; }
    public void setColaborador(Colaborador colaborador) { this.colaborador = colaborador; }

    public Subsidio getSubsidio() { return subsidio; }
    public void setSubsidio(Subsidio subsidio) { this.subsidio = subsidio; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }
}
