package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "permissao_modulo_usuario")
public class PermissaoModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String modulo; // DASHBOARD, VENDAS, STOCK, ENTIDADES, FACTURACAO, FINANCEIRO, ADMINISTRACAO

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private User usuario;

    @Column(nullable = false)
    private boolean ativo = true;

    public PermissaoModulo() {}

    public PermissaoModulo(String modulo, User usuario, boolean ativo) {
        this.modulo = modulo;
        this.usuario = usuario;
        this.ativo = ativo;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getModulo() { return modulo; }
    public void setModulo(String modulo) { this.modulo = modulo; }

    public User getUsuario() { return usuario; }
    public void setUsuario(User usuario) { this.usuario = usuario; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
