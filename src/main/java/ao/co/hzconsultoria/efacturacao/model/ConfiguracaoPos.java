package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "configuracoes_pos")
public class ConfiguracaoPos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "empresa_id", unique = true)
    private Empresa empresa;

    private String larguraPapel = "80mm"; // 80mm, 58mm
    private Boolean abrirGavetaAuto = true;
    private String prefixoBalanca = "20"; // Código EAN-13 de balança de retalho

    @Column(name = "modo_restauracao_ativo")
    private Boolean modoRestauracaoAtivo = true;

    @Column(columnDefinition = "TEXT")
    private String atalhosTecladoJson; // Guarda mapeamento customizado de teclas se necessário

    public ConfiguracaoPos() {}

    public ConfiguracaoPos(Empresa empresa) {
        this.empresa = empresa;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getLarguraPapel() { return larguraPapel; }
    public void setLarguraPapel(String larguraPapel) { this.larguraPapel = larguraPapel; }

    public Boolean getAbrirGavetaAuto() { return abrirGavetaAuto; }
    public void setAbrirGavetaAuto(Boolean abrirGavetaAuto) { this.abrirGavetaAuto = abrirGavetaAuto; }

    public String getPrefixoBalanca() { return prefixoBalanca; }
    public void setPrefixoBalanca(String prefixoBalanca) { this.prefixoBalanca = prefixoBalanca; }

    public Boolean getModoRestauracaoAtivo() { return modoRestauracaoAtivo != null ? modoRestauracaoAtivo : true; }
    public void setModoRestauracaoAtivo(Boolean modoRestauracaoAtivo) { this.modoRestauracaoAtivo = modoRestauracaoAtivo; }

    // Métodos de compatibilidade
    public Boolean getModoRestauraçãoAtivo() { return getModoRestauracaoAtivo(); }
    public void setModoRestauraçãoAtivo(Boolean modo) { setModoRestauracaoAtivo(modo); }

    public String getAtalhosTecladoJson() { return atalhosTecladoJson; }
    public void setAtalhosTecladoJson(String atalhosTecladoJson) { this.atalhosTecladoJson = atalhosTecladoJson; }
}
