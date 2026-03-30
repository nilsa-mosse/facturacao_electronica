package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.ItemGuiaRemessa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemGuiaRemessaRepository extends JpaRepository<ItemGuiaRemessa, Long> {
}
