package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.CarrinhoService;
import ao.co.hzconsultoria.efacturacao.service.VendaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired(required = false)
    private ao.co.hzconsultoria.efacturacao.service.FaturaService faturaService;

    @Autowired
    private VendaService vendaService;

    @GetMapping("/")
    public String index(
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "pesquisa", required = false) String pesquisa,
            Model model
    ) {
        List<Produto> produtos;
        if (pesquisa != null && !pesquisa.isEmpty()) {
            produtos = produtoRepository.findAll().stream()
                .filter(p -> p.getNome().toLowerCase().contains(pesquisa.toLowerCase()) || 
                             (p.getCodigoBarra() != null && p.getCodigoBarra().contains(pesquisa)))
                .collect(Collectors.toList());
        } else if (categoriaId != null) {
            produtos = produtoRepository.findAll().stream()
                .filter(p -> p.getCategoria() != null && p.getCategoria().getId().equals(categoriaId))
                .collect(Collectors.toList());
        } else {
            produtos = produtoRepository.findAll(PageRequest.of(0, 24)).getContent();
        }

        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", categoriaRepository.findAll());
        model.addAttribute("clientes", clienteRepository.findAll());
        model.addAttribute("carrinho", carrinho);
        model.addAttribute("categoriaSelecionada", categoriaId);
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
    public String finalizarVenda(RedirectAttributes redirectAttributes) {
        if (carrinho.getItens().isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "O carrinho está vazio.");
            return "redirect:/";
        }
        // Convert Carrinho to Compra
        Compra compra = new Compra();
        compra.setCliente(carrinho.getCliente());
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

        redirectAttributes.addFlashAttribute("mensagemSucesso", "Venda finalizada com sucesso!");
        // Redirect to the home page
        return "redirect:/";
    }

    @GetMapping("/buscarProduto")
    public String buscarProduto(@RequestParam String codigoBarra, Model model) {
        Produto encontrado = produtoRepository.findByCodigoBarra(codigoBarra);
        model.addAttribute("produtos", encontrado != null ? java.util.Arrays.asList(encontrado) : java.util.Collections.emptyList());
        model.addAttribute("carrinho", carrinho);
        return "index";
    }

    @PostMapping("/api/removerProduto")
    @ResponseBody
    public Carrinho removerProdutoApi(@RequestParam Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId).orElse(null);
        if (produto != null) {
            carrinhoService.removerProduto(carrinho, produto);
        }
        return carrinho;
    }

    @PostMapping("/api/definirCliente")
    @ResponseBody
    public Carrinho definirClienteApi(@RequestParam Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        carrinho.setCliente(cliente);
        return carrinho;
    }

    @PostMapping("/api/limparCarrinho")
    @ResponseBody
    public Carrinho limparCarrinhoApi() {
        carrinho = new Carrinho();
        return carrinho;
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