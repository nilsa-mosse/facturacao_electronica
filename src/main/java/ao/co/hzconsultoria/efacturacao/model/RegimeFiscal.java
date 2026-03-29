package ao.co.hzconsultoria.efacturacao.model;
 
import javax.persistence.*;
 
@Entity
@Table(name = "regime_fiscal")
public class RegimeFiscal {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @Column(nullable = false, unique = true)
    private String nome;
 
    @Column(nullable = false, unique = true)
    private String codigo; // E.g. GERAL, SIMPLIFICADO
 
    private String descricao;
    private String icone; // FontAwesome icon class
 
    public RegimeFiscal() {}
 
    public RegimeFiscal(String nome, String codigo, String descricao, String icone) {
        this.nome = nome;
        this.codigo = codigo;
        this.descricao = descricao;
        this.icone = icone;
    }
 
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
 
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
 
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
 
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
 
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
}
