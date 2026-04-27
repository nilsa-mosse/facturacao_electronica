package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConfiguracaoEmpresaRepository extends JpaRepository<ConfiguracaoEmpresa, Long> {
    Optional<ConfiguracaoEmpresa> findByEmpresa(Empresa empresa);

    Optional<ConfiguracaoEmpresa> findByEmpresa_Id(Long empresaId);
}
