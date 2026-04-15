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
    Produto findByCodigoBarra(String codigoBarra);
    
    Page<Produto> findAll(Pageable pageable);
    
    Page<Produto> findByCategoria_Nome(String nome, Pageable pageable);
    Page<Produto> findByCategoria_Id(Long id, Pageable pageable);
    
    List<Produto> findByNomeStartingWithIgnoreCase(String nome);

    @org.springframework.data.jpa.repository.Query("SELECT p FROM Produto p WHERE p.quantidadeEstoque <= p.estoqueMinimo")
    List<Produto> findProdutosComStockBaixo();
}