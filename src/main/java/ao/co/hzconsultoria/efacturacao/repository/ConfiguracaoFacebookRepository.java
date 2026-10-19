package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoFacebook;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoFacebookRepository extends JpaRepository<ConfiguracaoFacebook, Long> {
    Optional<ConfiguracaoFacebook> findByEmpresa(Empresa empresa);
    Optional<ConfiguracaoFacebook> findByEmpresaId(Long empresaId);
}
