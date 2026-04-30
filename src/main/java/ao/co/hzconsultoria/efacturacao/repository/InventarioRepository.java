package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    List<Inventario> findByEmpresa_Id(Long empresaId);
    List<Inventario> findByEmpresa_IdOrderByCreatedAtDesc(Long empresaId);
    // Find inventories by company, specific type and any of the provided states
    List<Inventario> findByEmpresa_IdAndTipoAndEstadoIn(Long empresaId, Inventario.TipoInventario tipo, java.util.List<Inventario.EstadoInventario> estados);
}