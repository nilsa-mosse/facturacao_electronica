package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.ParametroPayroll;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParametroPayrollRepository extends JpaRepository<ParametroPayroll, Long> {
    Optional<ParametroPayroll> findByEmpresaId(Long empresaId);
}
