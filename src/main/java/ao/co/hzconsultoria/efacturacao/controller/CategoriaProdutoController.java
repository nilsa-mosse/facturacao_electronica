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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CategoriaProdutoController {
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/produtos")
    public String produtos(@RequestParam(value = "busca", required = false) String busca,
                          @RequestParam(value = "cat", required = false) Long cat,
                          @RequestParam(value = "page", defaultValue = "0") int page,
                          Model model) {
        Pageable pageable = PageRequest.of(page, 10);
        Page<Produto> produtosPage;
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (cat != null && empresaId != null) {
            produtosPage = produtoRepository.findByCategoria_IdAndEmpresa_Id(cat, empresaId, pageable);
        } else if (empresaId != null) {
            produtosPage = produtoRepository.findByEmpresa_Id(empresaId, pageable);
        } else {
            produtosPage = produtoRepository.findAll(pageable);
        }
        List<Produto> produtos = produtosPage.getContent();
        if (busca != null && !busca.isEmpty()) {
            produtos = produtos.stream()
                .filter(p -> p.getNome().toLowerCase().contains(busca.toLowerCase()) ||
                             (p.getDescricao() != null && p.getDescricao().toLowerCase().contains(busca.toLowerCase())) ||
                             (p.getCodigoBarra() != null && p.getCodigoBarra().contains(busca)))
                .collect(Collectors.toList());
        }
        model.addAttribute("produtos", produtos);
        model.addAttribute("produtosPage", produtosPage);
        model.addAttribute("categoriaSelecionada", cat);
        model.addAttribute("title", "Catálogo de Produtos");
        model.addAttribute("content", "produtos :: content");
        return "layout";
    }

    @ModelAttribute("categorias")
    public List<Categoria> categorias() {
        Pageable pageable = PageRequest.of(0, 20);
        return categoriaRepository.findAll(pageable).getContent();
    }
}