package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.ColaboradorSubsidio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ColaboradorSubsidioRepository extends JpaRepository<ColaboradorSubsidio, Long> {
    List<ColaboradorSubsidio> findByColaboradorId(Long colaboradorId);
    void deleteByColaboradorId(Long colaboradorId);
}
