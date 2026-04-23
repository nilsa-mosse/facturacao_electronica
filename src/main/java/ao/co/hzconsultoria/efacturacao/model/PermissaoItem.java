package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

/**
 * Permissão granular por item (funcionalidade) dentro de um módulo.
 * Ex.: módulo VENDAS → items: NOVA_VENDA, HISTORICO_VENDAS, DEVOLUCOES, etc.
 */
@Entity
@Table(name = "permissao_item_usuario",
       uniqueConstraints = @UniqueConstraint(columnNames = {"modulo", "item", "usuario_id"}))
public class PermissaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome do módulo-pai (ex.: "VENDAS") */
    @Column(nullable = false)
    private String modulo;

    /** Chave do item (ex.: "NOVA_VENDA") */
    @Column(nullable = false)
    private String item;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private boolean ativo = true;

    public PermissaoItem() {}

    public PermissaoItem(String modulo, String item, User usuario, boolean ativo) {
        this.modulo = modulo;
        this.item = item;
        this.usuario = usuario;
        this.ativo = ativo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
