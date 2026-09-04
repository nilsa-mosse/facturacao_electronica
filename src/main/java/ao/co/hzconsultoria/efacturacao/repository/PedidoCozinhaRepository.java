package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.PedidoCozinha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoCozinhaRepository extends JpaRepository<PedidoCozinha, Long> {
    List<PedidoCozinha> findByEmpresaIdAndStatusNotOrderByDataHoraAsc(Long empresaId, String status);
    List<PedidoCozinha> findByEmpresaIdOrderByDataHoraDesc(Long empresaId);
    List<PedidoCozinha> findByMesaIdAndStatusNot(Long mesaId, String status);
}
