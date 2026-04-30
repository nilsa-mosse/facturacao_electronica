package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "inventarios")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    private TipoInventario tipo;

    private String armazem;
    private String localizacao; // prateleira, corredor, zona

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataAbertura;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataPrevisaoFecho;

    private String responsavel;

    @Enumerated(EnumType.STRING)
    private EstadoInventario estado;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<ItemInventario> itens = new java.util.ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TipoInventario {
        GERAL, PARCIAL, ROTATIVO, EXTRAORDINARIO
    }

    public enum EstadoInventario {
        RASCUNHO, EM_CONTAGEM, EM_REVISAO, FINALIZADO, CANCELADO
    }

    // Calculated fields for the listing
    public int getTotalItens() {
        return itens != null ? itens.size() : 0;
    }

    public long getItensDivergentes() {
        if (itens == null) return 0;
        return itens.stream().filter(item -> !item.getDivergencia().equals(0.0)).count();
    }

    public double getValorDivergencias() {
        if (itens == null) return 0.0;
        return itens.stream().mapToDouble(ItemInventario::getValorDivergencia).sum();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public TipoInventario getTipo() { return tipo; }
    public void setTipo(TipoInventario tipo) { this.tipo = tipo; }

    public String getArmazem() { return armazem; }
    public void setArmazem(String armazem) { this.armazem = armazem; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public LocalDate getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(LocalDate dataAbertura) { this.dataAbertura = dataAbertura; }

    public LocalDate getDataPrevisaoFecho() { return dataPrevisaoFecho; }
    public void setDataPrevisaoFecho(LocalDate dataPrevisaoFecho) { this.dataPrevisaoFecho = dataPrevisaoFecho; }

    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) { this.responsavel = responsavel; }

    public EstadoInventario getEstado() { return estado; }
    public void setEstado(EstadoInventario estado) { this.estado = estado; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public java.util.List<ItemInventario> getItens() { return itens; }
    public void setItens(java.util.List<ItemInventario> itens) { this.itens = itens; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}