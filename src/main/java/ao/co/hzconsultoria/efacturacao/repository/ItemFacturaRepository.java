package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.ItemFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemFacturaRepository extends JpaRepository<ItemFactura, Long> {
    List<ItemFactura> findByFacturaId(Long facturaId);
}
