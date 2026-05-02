package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    Categoria findByNomeAndEmpresa_Id(String nome, Long empresaId);
    Page<Categoria> findByEmpresa_Id(Long empresaId, Pageable pageable);

    @Cacheable("categorias_por_empresa")
    List<Categoria> findByEmpresa_Id(Long empresaId);

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "categorias_por_empresa", allEntries = true)
    <S extends Categoria> S save(S entity);
}