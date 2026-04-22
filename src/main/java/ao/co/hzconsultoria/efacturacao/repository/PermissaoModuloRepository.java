package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.PermissaoModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissaoModuloRepository extends JpaRepository<PermissaoModulo, Long> {
    List<PermissaoModulo> findByUsuario_Id(Long usuarioId);
    Optional<PermissaoModulo> findByModuloAndUsuario_Id(String modulo, Long usuarioId);
}
