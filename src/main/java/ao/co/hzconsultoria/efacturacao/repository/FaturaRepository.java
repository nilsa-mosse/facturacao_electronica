package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {
}
