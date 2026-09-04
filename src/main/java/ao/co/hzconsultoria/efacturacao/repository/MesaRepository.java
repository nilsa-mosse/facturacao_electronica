package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MesaRepository extends JpaRepository<Mesa, Long> {
    List<Mesa> findByEmpresaIdOrderByNumeroMesaAsc(Long empresaId);
    List<Mesa> findByEmpresaIdAndStatus(Long empresaId, String status);
    Optional<Mesa> findByIdAndEmpresaId(Long id, Long empresaId);
    long countByEmpresaId(Long empresaId);
}
