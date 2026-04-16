package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
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

    // Estabelecimentos visíveis para este usuário
    @ManyToMany
    @JoinTable(name = "usuario_estabelecimento",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "estabelecimento_id"))
    private Set<Estabelecimento> estabelecimentos = new HashSet<>();

    // Getters e Setters
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
}