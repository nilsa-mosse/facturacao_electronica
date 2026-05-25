package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Subsidio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubsidioRepository extends JpaRepository<Subsidio, Long> {
    List<Subsidio> findByEmpresaId(Long empresaId);
}
