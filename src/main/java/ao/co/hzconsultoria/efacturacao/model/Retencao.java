package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "retencao")
public class Retencao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private BigDecimal percentagem;

    private boolean activo = true;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getPercentagem() { return percentagem; }
    public void setPercentagem(BigDecimal percentagem) { this.percentagem = percentagem; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
