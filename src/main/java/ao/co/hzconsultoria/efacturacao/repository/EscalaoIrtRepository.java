package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.EscalaoIrt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EscalaoIrtRepository extends JpaRepository<EscalaoIrt, Long> {
    List<EscalaoIrt> findByEmpresaIdOrderByLimiteInferiorAsc(Long empresaId);
    void deleteByEmpresaId(Long empresaId);
}
