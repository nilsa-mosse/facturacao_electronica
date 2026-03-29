package ao.co.hzconsultoria.efacturacao.repository;
 
import ao.co.hzconsultoria.efacturacao.model.RegimeFiscal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
 
@Repository
public interface RegimeFiscalRepository extends JpaRepository<RegimeFiscal, Long> {
    Optional<RegimeFiscal> findByCodigo(String codigo);
}
