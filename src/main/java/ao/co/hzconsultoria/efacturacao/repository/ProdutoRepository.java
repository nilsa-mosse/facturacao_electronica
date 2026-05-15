package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Produto;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    @Cacheable("produtos_por_empresa")
    List<Produto> findByEmpresa_Id(Long empresaId);

    Page<Produto> findByEmpresa_Id(Long empresaId, Pageable pageable);
    
    Produto findByCodigoBarraAndEmpresa_Id(String codigoBarra, Long empresaId);
    
    Page<Produto> findByCategoria_IdAndEmpresa_Id(Long categoriaId, Long empresaId, Pageable pageable);
    @Cacheable("produtos_por_categoria")
    List<Produto> findByCategoria_IdAndEmpresa_Id(Long categoriaId, Long empresaId);
    
    List<Produto> findByNomeContainingIgnoreCaseAndEmpresa_Id(String nome, Long empresaId);
    
    List<Produto> findByNomeContainingIgnoreCase(String nome);
    
    List<Produto> findByNomeStartingWithIgnoreCaseAndEmpresa_Id(String nome, Long empresaId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Produto p WHERE (:empresaId IS NULL OR p.empresa.id = :empresaId) AND (p.quantidadeEstoque <= p.estoqueMinimo OR (p.estoqueMinimo = 0 AND p.quantidadeEstoque <= 5))")
    List<Produto> findProdutosComStockBaixo(@org.springframework.data.repository.query.Param("empresaId") Long empresaId);

    List<Produto> findByDataExpiracaoBeforeAndEmPromocaoFalse(java.time.LocalDate date);

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = {"produtos_por_empresa", "produtos_por_categoria"}, allEntries = true)
    <S extends Produto> S save(S entity);

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = {"produtos_por_empresa", "produtos_por_categoria"}, allEntries = true)
    void deleteById(Long id);
}