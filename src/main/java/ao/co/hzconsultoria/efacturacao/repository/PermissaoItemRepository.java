package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.PermissaoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissaoItemRepository extends JpaRepository<PermissaoItem, Long> {
    List<PermissaoItem> findByUsuario_Id(Long usuarioId);
    List<PermissaoItem> findByModuloAndUsuario_Id(String modulo, Long usuarioId);
    Optional<PermissaoItem> findByModuloAndItemAndUsuario_Id(String modulo, String item, Long usuarioId);
}
