package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Caixa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CaixaRepository extends JpaRepository<Caixa, Long> {
    Optional<Caixa> findFirstByOperador_IdAndEstadoOrderByIdDesc(Long operadorId, String estado);
    List<Caixa> findByEmpresa_IdOrderByDataAberturaDesc(Long empresaId);
    List<Caixa> findByOperador_IdOrderByDataAberturaDesc(Long operadorId);
}
