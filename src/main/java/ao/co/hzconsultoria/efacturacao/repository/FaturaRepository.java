package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Compra;
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
            "WHERE f.empresa.id = :empresaId AND " +
            "(:status IS NULL OR f.status = :status) AND " +
            "(:startDate IS NULL OR f.dataEmissao >= :startDate) AND " +
            "(:endDate IS NULL OR f.dataEmissao <= :endDate) AND " +
            "(:nif IS NULL OR cl.nif LIKE CONCAT('%', :nif, '%')) " +
            "ORDER BY f.dataEmissao DESC")
    List<Fatura> findByFilters(@Param("empresaId") Long empresaId,
            @Param("status") String status,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("nif") String nif);

    List<Fatura> findByEmpresa_Id(Long empresaId);
    List<Fatura> findByCompraAndEmpresa_Id(Compra compra, Long empresaId);

    List<Fatura> findByCompra(Compra compra);

    @Query(value = "SELECT * FROM fatura WHERE tipo_documento = :tipo AND empresa_id = :empresaId ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Fatura findLastByType(@Param("tipo") String tipo, @Param("empresaId") Long empresaId);

    @Query(value = "SELECT COUNT(*) FROM fatura WHERE tipo_documento = :tipo AND YEAR(data_emissao) = :ano AND empresa_id = :empresaId", nativeQuery = true)
    long countByTypeAndYear(@Param("tipo") String tipo, @Param("ano") int ano, @Param("empresaId") Long empresaId);
}
