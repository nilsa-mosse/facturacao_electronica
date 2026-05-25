package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.SalarioProcessado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalarioProcessadoRepository extends JpaRepository<SalarioProcessado, Long> {
    List<SalarioProcessado> findByFolhaProcessamento_Id(Long folhaId);
    Optional<SalarioProcessado> findByFolhaProcessamento_IdAndColaborador_Id(Long folhaId, Long colaboradorId);
}
