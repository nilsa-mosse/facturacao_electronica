package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;

@Entity
@Table(name = "colaborador")
public class Colaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String nif;
    private String email;
    private String telefone;
    private String endereco;
    private String iban;

    @Column(name = "salario_base")
    private double salarioBase;

    @Column(name = "data_admissao")
    @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataAdmissao;

    private String cargo;
    private String habilitacoes;
    private int dependentes;

    @Column(name = "tipo_contrato")
    private String tipoContrato;

    // Relação com Departamento (substituindo o campo String)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departamento_id")
    private Departamento departamento;

    @ManyToOne
    @JoinColumn(name = "empresa_id")
    private Empresa empresa;

    // Subsídios dinâmicos atribuídos a este colaborador
    @OneToMany(mappedBy = "colaborador", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<ColaboradorSubsidio> subsidios = new LinkedHashSet<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getNif() { return nif; }
    public void setNif(String nif) { this.nif = nif; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }

    public double getSalarioBase() { return salarioBase; }
    public void setSalarioBase(double salarioBase) { this.salarioBase = salarioBase; }

    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate dataAdmissao) { this.dataAdmissao = dataAdmissao; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public Departamento getDepartamento() { return departamento; }
    public void setDepartamento(Departamento departamento) { this.departamento = departamento; }

    /** Retrocompatibilidade — retorna o nome do departamento como String */
    public String getDepartamentoNome() {
        return departamento != null ? departamento.getNome() : "-";
    }

    public String getHabilitacoes() { return habilitacoes; }
    public void setHabilitacoes(String habilitacoes) { this.habilitacoes = habilitacoes; }

    public int getDependentes() { return dependentes; }
    public void setDependentes(int dependentes) { this.dependentes = dependentes; }

    public String getTipoContrato() { return tipoContrato; }
    public void setTipoContrato(String tipoContrato) { this.tipoContrato = tipoContrato; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }

    public Set<ColaboradorSubsidio> getSubsidios() { return subsidios; }
    public void setSubsidios(Set<ColaboradorSubsidio> subsidios) { this.subsidios = subsidios; }
}
