package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.CarrinhoService;
import ao.co.hzconsultoria.efacturacao.service.VendaService;
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

    @Autowired
    private VendaService vendaService;

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
            // Save the item to the database
            carrinhoService.salvarItemNoBanco(carrinho, produto, quantidade);
        }
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("produtos", produtoRepository.findAll(pageable).getContent());
        model.addAttribute("carrinho", carrinho);
        return "adicionarProduto";
    }

    @PostMapping("/finalizarVenda")
    public String finalizarVenda(Model model) {
        // Convert Carrinho to Compra
        Compra compra = new Compra();
        compra.setItens(carrinho.getItens().stream().map(item -> {
            ItemCompra itemCompra = new ItemCompra();
            itemCompra.setNomeProduto(item.getProduto().getNome());
            itemCompra.setQuantidade(item.getQuantidade());
            itemCompra.setPreco(item.getProduto().getPreco());
            itemCompra.setSubtotal(item.getQuantidade() * item.getProduto().getPreco());
            itemCompra.setCompra(compra); // Vincula o item à compra
            return itemCompra;
        }).collect(Collectors.toList()));

        // Finalize the purchase using VendaService
        vendaService.finalizarVenda(compra);

        // Clear the cart after saving
        carrinho = new Carrinho();

        // Redirect to the home page
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