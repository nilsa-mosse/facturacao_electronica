package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.CarrinhoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class CarrinhoController {
    private Carrinho carrinho = new Carrinho();
    private CarrinhoService carrinhoService = new CarrinhoService();

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired(required = false)
    private ao.co.hzconsultoria.efacturacao.service.FaturaService faturaService;

    @GetMapping("/")
    public String index(Model model) {
        long start = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(0, 20); // primeira página, 20 itens
        List<Produto> produtos = produtoRepository.findAll(pageable).getContent();
        List<Categoria> categorias = categoriaRepository.findAll(pageable).getContent();
        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("carrinho", carrinho);
        model.addAttribute("categoriaSelecionada", null); // Ensure variable is always present
        long end = System.currentTimeMillis();
        System.out.println("[PERF] Tempo de resposta do endpoint '/' = " + (end - start) + " ms");
        return "index";
    }

    @PostMapping("/adicionarProduto")
    public String adicionarProduto(@RequestParam Long produtoId, @RequestParam int quantidade, Model model) {
        Produto produto = produtoRepository.findById(produtoId).orElse(null);
        if (produto != null) {
            carrinhoService.adicionarProduto(carrinho, produto, quantidade);
        }
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("produtos", produtoRepository.findAll(pageable).getContent());
        model.addAttribute("carrinho", carrinho);
        return "adicionarProduto";
    }

    @PostMapping("/finalizarVenda")
    public String finalizarVenda(Model model) {
        // Gera a fatura se o serviço estiver disponível
        if (faturaService != null) {
            faturaService.gerarFatura(carrinho);
        }
        carrinho = new Carrinho(); // Limpa carrinho após venda
        // Redireciona para a página inicial
        return "index";
    }

    @GetMapping("/buscarProduto")
    public String buscarProduto(@RequestParam String codigoBarra, Model model) {
        Produto encontrado = produtoRepository.findByCodigoBarra(codigoBarra);
        model.addAttribute("produtos", encontrado != null ? java.util.Arrays.asList(encontrado) : java.util.Collections.emptyList());
        model.addAttribute("carrinho", carrinho);
        return "index";
    }

    @GetMapping("/index")
    public String index(@RequestParam(value = "categoriaSelecionada", required = false) String categoriaSelecionada, Model model) {
        Pageable pageable = PageRequest.of(0, 20);
        List<Produto> produtos = produtoRepository.findAll(pageable).getContent();
        List<Categoria> categorias = categoriaRepository.findAll(pageable).getContent();
        if (categoriaSelecionada != null && !categoriaSelecionada.isEmpty()) {
            produtos = produtos.stream()
                .filter(p -> p.getCategoria() != null && categoriaSelecionada.equals(p.getCategoria().getNome()))
                .collect(Collectors.toList());
        }
        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", categorias);
        model.addAttribute("categoriaSelecionada", categoriaSelecionada);
        model.addAttribute("carrinho", carrinho);
        return "index";
    }

    @PostMapping("/api/adicionarProduto")
    @ResponseBody
    public Carrinho adicionarProdutoApi(@RequestParam Long produtoId, @RequestParam int quantidade) {
        Produto produto = produtoRepository.findById(produtoId).orElse(null);
        if (produto != null) {
            carrinhoService.adicionarProduto(carrinho, produto, quantidade);
        }
        return carrinho;
    }
}