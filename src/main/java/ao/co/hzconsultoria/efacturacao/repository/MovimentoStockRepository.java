package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.MovimentoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovimentoStockRepository extends JpaRepository<MovimentoStock, Long> {
    List<MovimentoStock> findByProdutoIdOrderByDataMovimentoDesc(Long produtoId);
    List<MovimentoStock> findAllByOrderByDataMovimentoDesc();
}
