package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuario")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String login;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String role;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // Estabelecimentos visíveis para este usuário
    @ManyToMany
    @JoinTable(name = "usuario_estabelecimento",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "estabelecimento_id"))
    private Set<Estabelecimento> estabelecimentos = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_permissoes", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "permissao")
    private Set<String> permissoes = new HashSet<>();

    private int tentativasLogin = 0;

    private LocalDateTime bloqueadoAte;

    private boolean ativo = true;

    // Getters e Setters
    public Set<String> getPermissoes() { return permissoes; }
    public void setPermissoes(Set<String> permissoes) { this.permissoes = permissoes; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Set<Estabelecimento> getEstabelecimentos() { return estabelecimentos; }
    public void setEstabelecimentos(Set<Estabelecimento> estabelecimentos) { this.estabelecimentos = estabelecimentos; }

    public int getTentativasLogin() { return tentativasLogin; }
    public void setTentativasLogin(int tentativasLogin) { this.tentativasLogin = tentativasLogin; }

    public LocalDateTime getBloqueadoAte() { return bloqueadoAte; }
    public void setBloqueadoAte(LocalDateTime bloqueadoAte) { this.bloqueadoAte = bloqueadoAte; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}