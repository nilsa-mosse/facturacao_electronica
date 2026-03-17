package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ListarProdutosController {
    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/produtos/listar")
    public String listarProdutos(@RequestParam(value = "cat", required = false) Long cat,
                                 @RequestParam(value = "busca", required = false) String busca,
                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                 Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Produto> produtosPage;
        if (cat != null) {
            produtosPage = produtoRepository.findByCategoria_Id(cat, pageable);
        } else {
            produtosPage = produtoRepository.findAll(pageable);
        }
        List<Produto> produtos = produtosPage.getContent();
        if (busca != null && !busca.isEmpty()) {
            produtos = produtos.stream()
                .filter(p -> p.getNome().toLowerCase().contains(busca.toLowerCase()))
                .collect(Collectors.toList());
        }
        List<Categoria> categorias = categoriaRepository.findAll(PageRequest.of(0, 50)).getContent();
        model.addAttribute("produtos", produtos);
        model.addAttribute("produtosPage", produtosPage);
        model.addAttribute("categorias", categorias);
        model.addAttribute("categoriaSelecionada", cat);
        model.addAttribute("busca", busca);
        return "listarProdutos";
    }
}
