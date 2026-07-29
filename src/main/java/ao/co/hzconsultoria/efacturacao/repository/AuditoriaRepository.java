package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEmpresaIdOrderByDataHoraDesc(Long empresaId);

    Page<Auditoria> findByEmpresaIdOrderByDataHoraDesc(Long empresaId, Pageable pageable);

    Page<Auditoria> findByEmpresaIdAndDataHoraBetweenOrderByDataHoraDesc(
            Long empresaId, LocalDateTime inicio, LocalDateTime fim, Pageable pageable);
}
