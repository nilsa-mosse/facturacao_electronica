package ao.co.hzconsultoria.efacturacao.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class EventoTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String status; // EM_TRANSITO, ENTREGUE, RETORNADO, etc.
    private LocalDateTime dataHora;
    private String localizacao;
    private String observacao;

    @ManyToOne
    @JsonIgnore
    private GuiaRemessa guiaRemessa;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public GuiaRemessa getGuiaRemessa() { return guiaRemessa; }
    public void setGuiaRemessa(GuiaRemessa guiaRemessa) { this.guiaRemessa = guiaRemessa; }
}
