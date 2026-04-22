package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "serie")
public class Serie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String prefixo;

    @Column(nullable = false)
    private Integer proximoNumero = 1;

    @Column(nullable = false)
    private Integer ano;

    @Column(nullable = false)
    private boolean activo = true;

    private String descricao;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // Getters and Setters
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPrefixo() { return prefixo; }
    public void setPrefixo(String prefixo) { this.prefixo = prefixo; }

    public Integer getProximoNumero() { return proximoNumero; }
    public void setProximoNumero(Integer proximoNumero) { this.proximoNumero = proximoNumero; }

    public Integer getAno() { return ano; }
    public void setAno(Integer ano) { this.ano = ano; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
