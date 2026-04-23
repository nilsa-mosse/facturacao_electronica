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
    List<Produto> findByEmpresa_Id(Long empresaId);
    Page<Produto> findByEmpresa_Id(Long empresaId, Pageable pageable);
    
    Produto findByCodigoBarraAndEmpresa_Id(String codigoBarra, Long empresaId);
    
    Page<Produto> findByCategoria_IdAndEmpresa_Id(Long categoriaId, Long empresaId, Pageable pageable);
    List<Produto> findByCategoria_IdAndEmpresa_Id(Long categoriaId, Long empresaId);
    
    List<Produto> findByNomeContainingIgnoreCaseAndEmpresa_Id(String nome, Long empresaId);
    
    List<Produto> findByNomeStartingWithIgnoreCaseAndEmpresa_Id(String nome, Long empresaId);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Produto p WHERE p.empresa.id = :empresaId AND p.quantidadeEstoque <= p.estoqueMinimo")
    List<Produto> findProdutosComStockBaixo(Long empresaId);

    List<Produto> findByDataExpiracaoBeforeAndEmPromocaoFalse(java.time.LocalDate date);
}