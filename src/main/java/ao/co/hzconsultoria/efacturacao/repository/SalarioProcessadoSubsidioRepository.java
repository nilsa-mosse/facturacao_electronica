package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.SalarioProcessadoSubsidio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SalarioProcessadoSubsidioRepository extends JpaRepository<SalarioProcessadoSubsidio, Long> {
    List<SalarioProcessadoSubsidio> findBySalarioProcessadoId(Long salarioProcessadoId);
    void deleteBySalarioProcessadoId(Long salarioProcessadoId);
}
