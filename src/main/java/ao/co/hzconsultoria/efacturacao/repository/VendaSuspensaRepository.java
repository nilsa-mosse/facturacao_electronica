package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.VendaSuspensa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VendaSuspensaRepository extends JpaRepository<VendaSuspensa, Long> {
    List<VendaSuspensa> findByEmpresa_IdOrderByDataHoraDesc(Long empresaId);
}
