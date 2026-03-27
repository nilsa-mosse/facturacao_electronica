package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "metodo_pagamento")
public class MetodoPagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome; // Ex: Numerário, Transferência, Multicaixa

    @Column(nullable = false, length = 10)
    private String codigoAgt; // Código da AGT: NU, TB, MC, CC

    @Column(nullable = false)
    private boolean activo = true;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigoAgt() { return codigoAgt; }
    public void setCodigoAgt(String codigoAgt) { this.codigoAgt = codigoAgt; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
