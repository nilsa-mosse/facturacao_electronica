package ao.co.hzconsultoria.efacturacao.repository;

import ao.co.hzconsultoria.efacturacao.model.Estoque;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
    List<Estoque> findByProdutoAndEstabelecimentoIn(Produto produto, List<Estabelecimento> estabelecimentos);
    List<Estoque> findByProduto(Produto produto);
    java.util.Optional<Estoque> findByProdutoAndEstabelecimento(Produto produto, Estabelecimento estabelecimento);
}
