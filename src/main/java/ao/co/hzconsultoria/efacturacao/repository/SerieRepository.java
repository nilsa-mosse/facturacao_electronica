package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Serie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SerieRepository extends JpaRepository<Serie, Long> {
    List<Serie> findByEmpresa_Id(Long empresaId);
    List<Serie> findByActivoTrueAndEmpresa_Id(Long empresaId);
}
