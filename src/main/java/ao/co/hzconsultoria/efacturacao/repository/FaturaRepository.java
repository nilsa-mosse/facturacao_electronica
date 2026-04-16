package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Date;
import java.util.List;

@Repository
public interface FaturaRepository extends JpaRepository<Fatura, Long> {

    @Query("SELECT f FROM Fatura f " +
           "LEFT JOIN f.compra c " +
           "LEFT JOIN c.cliente cl " +
           "WHERE (:status IS NULL OR f.status = :status) AND " +
           "(:startDate IS NULL OR f.dataEmissao >= :startDate) AND " +
           "(:endDate IS NULL OR f.dataEmissao <= :endDate) AND " +
           "(:nif IS NULL OR cl.nif LIKE CONCAT('%', :nif, '%')) " +
           "ORDER BY f.dataEmissao DESC")
    List<Fatura> findByFilters(@Param("status") String status, 
                              @Param("startDate") Date startDate, 
                              @Param("endDate") Date endDate, 
                              @Param("nif") String nif);

    List<Fatura> findByCompra(Compra compra);
}
