package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Registo de cada publicação feita no Facebook.
 * Permite rastrear o histórico e status de todas as publicações.
 */
@Entity
@Table(name = "publicacao_facebook")
public class PublicacaoFacebook implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "produto_id")
    private Produto produto;

    /** Tipo de publicação: PRODUTO, PROMOCAO, MANUAL, CATALOGO */
    @Column(name = "tipo_publicacao", length = 50)
    private String tipoPublicacao;

    /** Mensagem publicada */
    @Column(name = "mensagem", length = 5000)
    private String mensagem;

    /** ID do post no Facebook retornado pela API */
    @Column(name = "facebook_post_id", length = 200)
    private String facebookPostId;

    /** Status da publicação: SUCESSO, FALHA, PENDENTE */
    @Column(name = "status", length = 50)
    private String status;

    /** Resposta da API do Facebook (para diagnóstico) */
    @Column(name = "resposta_api", length = 2000)
    private String respostaApi;

    /** Data e hora da publicação */
    @Column(name = "data_publicacao")
    private LocalDateTime dataPublicacao;

    /** Se foi feita manualmente ou automaticamente */
    @Column(name = "publicacao_manual")
    private boolean publicacaoManual = false;

    /** URL da imagem publicada (se houver) */
    @Column(name = "url_imagem", length = 500)
    private String urlImagem;

    // ────── Getters e Setters ──────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public String getTipoPublicacao() { return tipoPublicacao; }
    public void setTipoPublicacao(String tipoPublicacao) { this.tipoPublicacao = tipoPublicacao; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public String getFacebookPostId() { return facebookPostId; }
    public void setFacebookPostId(String facebookPostId) { this.facebookPostId = facebookPostId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRespostaApi() { return respostaApi; }
    public void setRespostaApi(String respostaApi) { this.respostaApi = respostaApi; }

    public LocalDateTime getDataPublicacao() { return dataPublicacao; }
    public void setDataPublicacao(LocalDateTime dataPublicacao) { this.dataPublicacao = dataPublicacao; }

    public boolean isPublicacaoManual() { return publicacaoManual; }
    public void setPublicacaoManual(boolean publicacaoManual) { this.publicacaoManual = publicacaoManual; }

    public String getUrlImagem() { return urlImagem; }
    public void setUrlImagem(String urlImagem) { this.urlImagem = urlImagem; }
}
