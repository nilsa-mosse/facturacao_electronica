package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoPos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoPosRepository extends JpaRepository<ConfiguracaoPos, Long> {
    Optional<ConfiguracaoPos> findByEmpresaId(Long empresaId);
}
