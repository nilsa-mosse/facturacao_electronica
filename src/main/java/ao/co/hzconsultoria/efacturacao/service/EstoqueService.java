package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.dto.EstoqueDTO;
import ao.co.hzconsultoria.efacturacao.model.Estoque;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.model.Estabelecimento;
import ao.co.hzconsultoria.efacturacao.repository.EstoqueRepository;
import ao.co.hzconsultoria.efacturacao.repository.EstabelecimentoRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EstoqueService {

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    /**
     * Busca disponibilidade do produto em estabelecimentos visíveis ou em todo o grupo.
     * @param produtoId id do produto
     * @param showZero incluir estabelecimentos com quantidade = 0
     * @param includeAll se true pesquisa todos os estoques do produto (independentemente da visibilidade das lojas)
     * @return lista de DTOs
     */
    public List<EstoqueDTO> buscarDisponibilidadeProduto(Long produtoId, boolean showZero, boolean includeAll) {
        Produto produto = produtoRepository.findById(produtoId).orElse(null);
        if (produto == null) return new ArrayList<>();

        List<Estoque> estoques;
        if (includeAll) {
            // busca todos os estoques do produto (ignorando visibilidade das lojas)
            estoques = estoqueRepository.findByProduto(produto);
        } else {
            List<Estabelecimento> visiveis = estabelecimentoRepository.findByVisivelTrue();
            if (visiveis.isEmpty()) return new ArrayList<>();
            estoques = estoqueRepository.findByProdutoAndEstabelecimentoIn(produto, visiveis);
        }

        List<EstoqueDTO> dtos = estoques.stream()
                .filter(e -> showZero || (e.getQuantidade() != null && e.getQuantidade() > 0))
                .map(e -> new EstoqueDTO(
                        e.getEstabelecimento().getId(),
                        e.getEstabelecimento().getNome(),
                        e.getEstabelecimento().getEndereco(),
                        e.getEstabelecimento().getTipo(),
                        e.getQuantidade(),
                        e.getEstabelecimento().getVisivel()
                ))
                .collect(Collectors.toList());

        return dtos;
    }
}