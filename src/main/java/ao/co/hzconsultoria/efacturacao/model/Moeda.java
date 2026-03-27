package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "moeda")
public class Moeda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String sigla; // Ex: AOA, USD, EUR

    @Column(nullable = false)
    private String nome;

    @Column(precision = 10, scale = 4)
    private BigDecimal taxaCambio;

    @Column(nullable = false)
    private boolean padrao = false;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public BigDecimal getTaxaCambio() { return taxaCambio; }
    public void setTaxaCambio(BigDecimal taxaCambio) { this.taxaCambio = taxaCambio; }

    public boolean isPadrao() { return padrao; }
    public void setPadrao(boolean padrao) { this.padrao = padrao; }
}
