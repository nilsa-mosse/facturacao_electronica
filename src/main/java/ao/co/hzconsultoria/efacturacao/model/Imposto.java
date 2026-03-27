package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "imposto")
public class Imposto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private BigDecimal percentagem;

    private String codigoAgt;
    private String tipo;

    @Column(length = 255)
    private String motivoIsencao;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getPercentagem() {
        return percentagem;
    }

    public void setPercentagem(BigDecimal percentagem) {
        this.percentagem = percentagem;
    }

    public String getCodigoAgt() {
        return codigoAgt;
    }

    public void setCodigoAgt(String codigoAgt) {
        this.codigoAgt = codigoAgt;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMotivoIsencao() {
        return motivoIsencao;
    }

    public void setMotivoIsencao(String motivoIsencao) {
        this.motivoIsencao = motivoIsencao;
    }
}
