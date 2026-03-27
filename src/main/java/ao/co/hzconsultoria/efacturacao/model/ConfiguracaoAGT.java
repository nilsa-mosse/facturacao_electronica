package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;

@Entity
@Table(name = "configuracao_agt")
public class ConfiguracaoAGT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String urlApi;

    @Column(nullable = false)
    private String token;

    private String modo; // PRODUCAO ou HOMOLOGACAO

    private boolean reenvioAutomatico = false;

    private Integer tentativasReenvio = 3;

    private String nifCertificado;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUrlApi() { return urlApi; }
    public void setUrlApi(String urlApi) { this.urlApi = urlApi; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getModo() { return modo; }
    public void setModo(String modo) { this.modo = modo; }

    public boolean isReenvioAutomatico() { return reenvioAutomatico; }
    public void setReenvioAutomatico(boolean reenvioAutomatico) { this.reenvioAutomatico = reenvioAutomatico; }

    public Integer getTentativasReenvio() { return tentativasReenvio; }
    public void setTentativasReenvio(Integer tentativasReenvio) { this.tentativasReenvio = tentativasReenvio; }

    public String getNifCertificado() { return nifCertificado; }
    public void setNifCertificado(String nifCertificado) { this.nifCertificado = nifCertificado; }
}
