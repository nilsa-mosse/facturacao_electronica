package ao.co.hzconsultoria.efacturacao.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.time.LocalDate;


@Entity
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    private String descricao;
    private double preco;
    private Double quantidadeEstoque;
    private Double estoqueMinimo = 0.0;
    private String imagem;
    private String codigoBarra;
    @Lob
    @Column(columnDefinition="LONGBLOB")
    private byte[] imagemBlob;
    private Double ivaPercentual;
    @ManyToOne
    @JoinColumn(name = "categoria_id")
    @JsonBackReference
    private Categoria categoria;
    @ManyToOne
    @JoinColumn(name = "estado_id")
    private Estado estado;

    private LocalDate dataFabrico;
    private LocalDate dataExpiracao;
    private boolean emPromocao = false;
    private String unidadeMedida;
    private Double precoOriginal;



    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // Getters e Setters
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Estado getEstado() {
        // A matemática do stock tem supremacia absoluta sobre a chave gravada, vacinando corrupção.
        if (this.quantidadeEstoque != null && this.quantidadeEstoque <= 0) {
            return new Estado(2L, "INDISPONÍVEL");
        }
        if (estado == null) {
            return new Estado(1L, "DISPONÍVEL");
        }
        return estado;
    }

    @Transient
    public boolean isDisponivel() {
        Estado currentEstado = this.getEstado();
        // A lógica instruída: se ID for 2, fica indisponível (logo não é disponível)
        return currentEstado != null && currentEstado.getId() != null && currentEstado.getId() == 1L;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

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

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public Double getQuantidadeEstoque() {
        return quantidadeEstoque;
    }

    public void setQuantidadeEstoque(Double quantidadeEstoque) {
        this.quantidadeEstoque = quantidadeEstoque;
        // Auto-resolve o estado baseando-se no valor matemático sempre que o stock for mutado.
        if (this.quantidadeEstoque != null && this.quantidadeEstoque <= 0) {
            this.estado = new Estado(2L, "INDISPONÍVEL");
        } else if (this.quantidadeEstoque != null && this.quantidadeEstoque > 0) {
            this.estado = new Estado(1L, "DISPONÍVEL");
        }
    }

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public String getCodigoBarra() {
        return codigoBarra;
    }

    public void setCodigoBarra(String codigoBarra) {
        this.codigoBarra = codigoBarra;
    }

    public byte[] getImagemBlob() {
        return imagemBlob;
    }

    public void setImagemBlob(byte[] imagemBlob) {
        this.imagemBlob = imagemBlob;
    }

    public Double getIvaPercentual() {
        return ivaPercentual;
    }

    public void setIvaPercentual(Double ivaPercentual) {
        this.ivaPercentual = ivaPercentual;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Double getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(Double estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public LocalDate getDataFabrico() {
        return dataFabrico;
    }

    public void setDataFabrico(LocalDate dataFabrico) {
        this.dataFabrico = dataFabrico;
    }

    public LocalDate getDataExpiracao() {
        return dataExpiracao;
    }

    public void setDataExpiracao(LocalDate dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }

    public boolean isEmPromocao() {
        return emPromocao;
    }

    public void setEmPromocao(boolean emPromocao) {
        this.emPromocao = emPromocao;
    }

    public String getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(String unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public Double getPrecoOriginal() {
        return precoOriginal;
    }

    public void setPrecoOriginal(Double precoOriginal) {
        this.precoOriginal = precoOriginal;
    }

    @Transient
    public boolean isPertoDeExpirar() {
        if (dataExpiracao == null) return false;
        return dataExpiracao.isBefore(java.time.LocalDate.now().plusMonths(1));
    }

}