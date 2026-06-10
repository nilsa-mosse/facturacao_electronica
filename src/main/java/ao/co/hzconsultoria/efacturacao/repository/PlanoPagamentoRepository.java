package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.PlanoPagamento;
import ao.co.hzconsultoria.efacturacao.model.PlanoPagamento.TipoPeriodo;
import ao.co.hzconsultoria.efacturacao.model.PlanoPagamento.StatusPlano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanoPagamentoRepository extends JpaRepository<PlanoPagamento, Long> {

    List<PlanoPagamento> findByPeriodoOrderByPrecoAsc(TipoPeriodo periodo);

    List<PlanoPagamento> findByPeriodoAndStatusOrderByPrecoAsc(TipoPeriodo periodo, StatusPlano status);

    List<PlanoPagamento> findByStatusOrderByPeriodoAscPrecoAsc(StatusPlano status);

    List<PlanoPagamento> findByDestaqueTrue();

    long countByPeriodo(TipoPeriodo periodo);

    long countByStatus(StatusPlano status);
}
