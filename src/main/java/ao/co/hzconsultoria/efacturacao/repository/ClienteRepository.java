package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Cliente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByEmpresa_Id(Long empresaId);
    Optional<Cliente> findByNifAndEmpresa_Id(String nif, Long empresaId);

    @Query("SELECT c FROM Cliente c WHERE (:empresaId IS NULL OR c.empresa.id = :empresaId) " +
           "AND (:termo IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "     OR LOWER(c.nif) LIKE LOWER(CONCAT('%', :termo, '%')) " +
           "     OR (c.telefone IS NOT NULL AND LOWER(c.telefone) LIKE LOWER(CONCAT('%', :termo, '%')))) " +
           "ORDER BY c.nome ASC")
    List<Cliente> pesquisarClientesPos(@Param("empresaId") Long empresaId, @Param("termo") String termo, Pageable pageable);
}
