package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Configurações de integração com Facebook para publicação automática de produtos.
 * Permite configurar o Page Access Token, Page ID, e agendamentos de publicação.
 */
@Entity
@Table(name = "configuracao_facebook")
public class ConfiguracaoFacebook implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    /** Token de acesso da página Facebook (Page Access Token) */
    @Column(name = "page_access_token", length = 1000)
    private String pageAccessToken;

    /** ID da página Facebook onde serão feitas as publicações */
    @Column(name = "page_id", length = 100)
    private String pageId;

    /** Nome da página Facebook (apenas para exibição) */
    @Column(name = "page_name", length = 255)
    private String pageName;

    /** Se a integração com Facebook está activa */
    @Column(name = "ativo")
    private boolean ativo = false;

    /** Se deve publicar automaticamente os produtos em promoção */
    @Column(name = "publicar_promocoes_auto")
    private boolean publicarPromocoesAuto = false;

    /** Se deve publicar produtos com stock baixo para estimular vendas */
    @Column(name = "publicar_stock_baixo")
    private boolean publicarStockBaixo = false;

    /** Se deve publicar novos produtos automaticamente */
    @Column(name = "publicar_novos_produtos")
    private boolean publicarNovosProdutos = false;

    /** Mensagem/template padrão para publicações de produtos */
    @Column(name = "template_mensagem_produto", length = 2000)
    private String templateMensagemProduto = "✨ PRODUTO DISPONÍVEL NO SISTEMA KwanzaERP! ✨\n\n🛍️ Artigo: {nome}\n📂 Categoria: {categoria}\n💰 Preço: Sob Consulta\n📦 Stock: {stock} unidades\n\nℹ️ Detalhes do Produto:\n{descricao}\n\n📍 Entregas rápidas em Luanda e envio para todas as províncias!\n📲 Contacte-nos já (WhatsApp e Chamada Normal Unitel):\n📞 934 068 121";

    /** Mensagem/template para publicações de promoções */
    @Column(name = "template_mensagem_promocao", length = 2000)
    private String templateMensagemPromocao = "🔥 OPORTUNIDADE ESPECIAL NO KwanzaERP! 🔥\n\n🛍️ Artigo: {nome}\n💥 Condições promocionais exclusivas!\n💰 Preço: Sob Consulta\n\nℹ️ Informações:\n{descricao}\n\n⏰ Oferta válida até durar o stock existente!\n📲 Garanta já as suas condições especiais (WhatsApp e Chamada Normal Unitel):\n📞 934 068 121";

    /** Hashtags a adicionar a todas as publicações */
    @Column(name = "hashtags", length = 500)
    private String hashtags = "#Angola #Luanda #Compras #Ofertas";

    /** Expressão CRON para agendamento de publicações automáticas */
    @Column(name = "cron_expressao", length = 100)
    private String cronExpressao = "0 0 9 * * ?"; // Todos os dias às 9h

    /** Data da última publicação feita automaticamente */
    @Column(name = "data_ultima_publicacao")
    private LocalDateTime dataUltimaPublicacao;

    /** Total de publicações feitas com sucesso */
    @Column(name = "total_publicacoes_sucesso")
    private int totalPublicacoesSucesso = 0;

    /** Total de publicações com falha */
    @Column(name = "total_publicacoes_falha")
    private int totalPublicacoesFalha = 0;

    /** Log da última operação */
    @Column(name = "ultimo_log", length = 2000)
    private String ultimoLog;

    /** URL do App ID do Facebook Developer */
    @Column(name = "app_id", length = 200)
    private String appId;

    /** App Secret do Facebook Developer */
    @Column(name = "app_secret", length = 200)
    private String appSecret;

    /** Número máximo de publicações por dia para não spammar */
    @Column(name = "max_publicacoes_por_dia")
    private int maxPublicacoesPorDia = 3;

    /** Publicações feitas hoje */
    @Column(name = "publicacoes_hoje")
    private int publicacoesHoje = 0;

    /** Data de reset do contador diário */
    @Column(name = "data_reset_contador")
    private LocalDateTime dataResetContador;

    // ────── Getters e Setters ──────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public String getPageAccessToken() { return pageAccessToken; }
    public void setPageAccessToken(String pageAccessToken) { this.pageAccessToken = pageAccessToken; }

    public String getPageId() { return pageId; }
    public void setPageId(String pageId) { this.pageId = pageId; }

    public String getPageName() { return pageName; }
    public void setPageName(String pageName) { this.pageName = pageName; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public boolean isPublicarPromocoesAuto() { return publicarPromocoesAuto; }
    public void setPublicarPromocoesAuto(boolean publicarPromocoesAuto) { this.publicarPromocoesAuto = publicarPromocoesAuto; }

    public boolean isPublicarStockBaixo() { return publicarStockBaixo; }
    public void setPublicarStockBaixo(boolean publicarStockBaixo) { this.publicarStockBaixo = publicarStockBaixo; }

    public boolean isPublicarNovosProdutos() { return publicarNovosProdutos; }
    public void setPublicarNovosProdutos(boolean publicarNovosProdutos) { this.publicarNovosProdutos = publicarNovosProdutos; }

    public String getTemplateMensagemProduto() { return templateMensagemProduto; }
    public void setTemplateMensagemProduto(String templateMensagemProduto) { this.templateMensagemProduto = templateMensagemProduto; }

    public String getTemplateMensagemPromocao() { return templateMensagemPromocao; }
    public void setTemplateMensagemPromocao(String templateMensagemPromocao) { this.templateMensagemPromocao = templateMensagemPromocao; }

    public String getHashtags() { return hashtags; }
    public void setHashtags(String hashtags) { this.hashtags = hashtags; }

    public String getCronExpressao() { return cronExpressao; }
    public void setCronExpressao(String cronExpressao) { this.cronExpressao = cronExpressao; }

    public LocalDateTime getDataUltimaPublicacao() { return dataUltimaPublicacao; }
    public void setDataUltimaPublicacao(LocalDateTime dataUltimaPublicacao) { this.dataUltimaPublicacao = dataUltimaPublicacao; }

    public int getTotalPublicacoesSucesso() { return totalPublicacoesSucesso; }
    public void setTotalPublicacoesSucesso(int totalPublicacoesSucesso) { this.totalPublicacoesSucesso = totalPublicacoesSucesso; }

    public int getTotalPublicacoesFalha() { return totalPublicacoesFalha; }
    public void setTotalPublicacoesFalha(int totalPublicacoesFalha) { this.totalPublicacoesFalha = totalPublicacoesFalha; }

    public String getUltimoLog() { return ultimoLog; }
    public void setUltimoLog(String ultimoLog) { this.ultimoLog = ultimoLog; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String appSecret) { this.appSecret = appSecret; }

    public int getMaxPublicacoesPorDia() { return maxPublicacoesPorDia; }
    public void setMaxPublicacoesPorDia(int maxPublicacoesPorDia) { this.maxPublicacoesPorDia = maxPublicacoesPorDia; }

    public int getPublicacoesHoje() { return publicacoesHoje; }
    public void setPublicacoesHoje(int publicacoesHoje) { this.publicacoesHoje = publicacoesHoje; }

    public LocalDateTime getDataResetContador() { return dataResetContador; }
    public void setDataResetContador(LocalDateTime dataResetContador) { this.dataResetContador = dataResetContador; }
}
