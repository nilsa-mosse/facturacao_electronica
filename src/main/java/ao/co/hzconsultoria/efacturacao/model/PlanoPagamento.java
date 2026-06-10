package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "plano_pagamento")
public class PlanoPagamento implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum TipoPeriodo {
        TRIMESTRAL, SEMESTRAL, ANUAL
    }

    public enum StatusPlano {
        ATIVO, INATIVO, DESCONTINUADO
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(length = 1000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPeriodo periodo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "preco_original", precision = 12, scale = 2)
    private BigDecimal precoOriginal;

    @Column(name = "percentagem_desconto")
    private Integer percentagemDesconto;

    @Column(name = "max_utilizadores")
    private Integer maxUtilizadores;

    @Column(name = "max_empresas")
    private Integer maxEmpresas;

    @Column(name = "max_produtos")
    private Integer maxProdutos;

    @Column(name = "max_faturas_mes")
    private Integer maxFaturasMes;

    private String moeda;

    @Column(name = "inclui_suporte")
    private boolean incluiSuporte;

    @Column(name = "inclui_backup")
    private boolean incluiBackup;

    @Column(name = "inclui_api")
    private boolean incluiApi;

    @Column(name = "inclui_relatorios")
    private boolean incluiRelatorios;

    @Column(name = "inclui_agt")
    private boolean incluiAgt;

    @Column(name = "inclui_rh")
    private boolean incluiRh;

    @Column(name = "inclui_multiempresa")
    private boolean incluiMultiempresa;

    /** Funcionalidades adicionais (lista separada por vírgula) */
    @Column(name = "funcionalidades_extra", length = 2000)
    private String funcionalidadesExtra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusPlano status = StatusPlano.ATIVO;

    @Column(name = "destaque")
    private boolean destaque;

    @Column(name = "cor_badge")
    private String corBadge;

    @Column(name = "icone")
    private String icone;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
        if (this.moeda == null)
            this.moeda = "AOA";
    }

    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    // ── Getters & Setters ───────────────────────────────────────────────────

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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public TipoPeriodo getPeriodo() {
        return periodo;
    }

    public void setPeriodo(TipoPeriodo periodo) {
        this.periodo = periodo;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public BigDecimal getPrecoOriginal() {
        return precoOriginal;
    }

    public void setPrecoOriginal(BigDecimal precoOriginal) {
        this.precoOriginal = precoOriginal;
    }

    public Integer getPercentagemDesconto() {
        return percentagemDesconto;
    }

    public void setPercentagemDesconto(Integer percentagemDesconto) {
        this.percentagemDesconto = percentagemDesconto;
    }

    public Integer getMaxUtilizadores() {
        return maxUtilizadores;
    }

    public void setMaxUtilizadores(Integer maxUtilizadores) {
        this.maxUtilizadores = maxUtilizadores;
    }

    public Integer getMaxEmpresas() {
        return maxEmpresas;
    }

    public void setMaxEmpresas(Integer maxEmpresas) {
        this.maxEmpresas = maxEmpresas;
    }

    public Integer getMaxProdutos() {
        return maxProdutos;
    }

    public void setMaxProdutos(Integer maxProdutos) {
        this.maxProdutos = maxProdutos;
    }

    public Integer getMaxFaturasMes() {
        return maxFaturasMes;
    }

    public void setMaxFaturasMes(Integer maxFaturasMes) {
        this.maxFaturasMes = maxFaturasMes;
    }

    public String getMoeda() {
        return moeda;
    }

    public void setMoeda(String moeda) {
        this.moeda = moeda;
    }

    public boolean isIncluiSuporte() {
        return incluiSuporte;
    }

    public void setIncluiSuporte(boolean incluiSuporte) {
        this.incluiSuporte = incluiSuporte;
    }

    public boolean isIncluiBackup() {
        return incluiBackup;
    }

    public void setIncluiBackup(boolean incluiBackup) {
        this.incluiBackup = incluiBackup;
    }

    public boolean isIncluiApi() {
        return incluiApi;
    }

    public void setIncluiApi(boolean incluiApi) {
        this.incluiApi = incluiApi;
    }

    public boolean isIncluiRelatorios() {
        return incluiRelatorios;
    }

    public void setIncluiRelatorios(boolean incluiRelatorios) {
        this.incluiRelatorios = incluiRelatorios;
    }

    public boolean isIncluiAgt() {
        return incluiAgt;
    }

    public void setIncluiAgt(boolean incluiAgt) {
        this.incluiAgt = incluiAgt;
    }

    public boolean isIncluiRh() {
        return incluiRh;
    }

    public void setIncluiRh(boolean incluiRh) {
        this.incluiRh = incluiRh;
    }

    public boolean isIncluiMultiempresa() {
        return incluiMultiempresa;
    }

    public void setIncluiMultiempresa(boolean incluiMultiempresa) {
        this.incluiMultiempresa = incluiMultiempresa;
    }

    public String getFuncionalidadesExtra() {
        return funcionalidadesExtra;
    }

    public void setFuncionalidadesExtra(String funcionalidadesExtra) {
        this.funcionalidadesExtra = funcionalidadesExtra;
    }

    public StatusPlano getStatus() {
        return status;
    }

    public void setStatus(StatusPlano status) {
        this.status = status;
    }

    public boolean isDestaque() {
        return destaque;
    }

    public void setDestaque(boolean destaque) {
        this.destaque = destaque;
    }

    public String getCorBadge() {
        return corBadge;
    }

    public void setCorBadge(String corBadge) {
        this.corBadge = corBadge;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    /** Retorna o label amigável do período */
    public String getPeriodoLabel() {
        if (periodo == null)
            return "";
        switch (periodo) {
            case TRIMESTRAL:
                return "Trimestral (3 meses)";
            case SEMESTRAL:
                return "Semestral (6 meses)";
            case ANUAL:
                return "Anual (12 meses)";
            default:
                return "";
        }
    }
}
