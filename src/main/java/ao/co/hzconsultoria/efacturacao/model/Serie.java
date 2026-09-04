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

    // Campos de Integração AGT (Solicitação de Série v1.2)
    private String codigoSerieAgt;
    private String tipoDocumento = "FT";
    private String numeroEstabelecimento = "SEDE";
    private String indicadorContingencia = "N";
    private Long quantidadeAutorizada;
    private String estadoAgt = "PENDENTE";

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

    public String getCodigoSerieAgt() { return codigoSerieAgt; }
    public void setCodigoSerieAgt(String codigoSerieAgt) { this.codigoSerieAgt = codigoSerieAgt; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroEstabelecimento() { return numeroEstabelecimento; }
    public void setNumeroEstabelecimento(String numeroEstabelecimento) { this.numeroEstabelecimento = numeroEstabelecimento; }

    public String getIndicadorContingencia() { return indicadorContingencia; }
    public void setIndicadorContingencia(String indicadorContingencia) { this.indicadorContingencia = indicadorContingencia; }

    public Long getQuantidadeAutorizada() { return quantidadeAutorizada; }
    public void setQuantidadeAutorizada(Long quantidadeAutorizada) { this.quantidadeAutorizada = quantidadeAutorizada; }

    public String getEstadoAgt() { return estadoAgt; }
    public void setEstadoAgt(String estadoAgt) { this.estadoAgt = estadoAgt; }
}
