package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.FolhaProcessamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolhaProcessamentoRepository extends JpaRepository<FolhaProcessamento, Long> {
    List<FolhaProcessamento> findByEmpresa_Id(Long empresaId);
    Optional<FolhaProcessamento> findByMesAndAnoAndEmpresa_Id(int mes, int ano, Long empresaId);
}
