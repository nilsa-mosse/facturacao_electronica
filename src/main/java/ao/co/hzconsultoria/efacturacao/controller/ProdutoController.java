package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ImpostoRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class ProdutoController {
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ImpostoRepository impostoRepository;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/cadastroProduto")
    public String cadastroProduto(Model model) {
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("produto", new Produto());
        model.addAttribute("categorias", categoriaRepository.findAll(pageable).getContent());
        model.addAttribute("impostos", impostoRepository.findAll());
        return "cadastroProduto";
    }

    @PostMapping("/salvarProduto")
    public String salvarProduto(@RequestParam("nome") String nome,
                                @RequestParam("descricao") String descricao,
                                @RequestParam("preco") double preco,
                                @RequestParam("quantidadeEstoque") Double quantidadeEstoque,
                                @RequestParam("codigoBarra") String codigoBarra,
                                @RequestParam("categoriaId") Long categoriaId,
                                @RequestParam(value = "imagem", required = false) MultipartFile imagem,
                                @RequestParam(value = "ivaPercentual", required = false) Double ivaPercentual,
                                Model model) throws IOException {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(quantidadeEstoque);
        produto.setCodigoBarra(codigoBarra);
        produto.setIvaPercentual(ivaPercentual);
        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        produto.setCategoria(categoria);
        // Salva primeiro para gerar o ID
        produtoRepository.save(produto);
        if (imagem != null && !imagem.isEmpty()) {
            produto.setImagemBlob(imagem.getBytes());
            produto.setImagem("/produto/imagem/" + produto.getId()); // Caminho correto
            produtoRepository.save(produto); // Salva novamente com imagem
        }
        model.addAttribute("mensagem", messageSource.getMessage("msg.produto.salvo", null, LocaleContextHolder.getLocale()));
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("categorias", categoriaRepository.findAll(pageable).getContent());
        model.addAttribute("impostos", impostoRepository.findAll());
        return "cadastroProduto";
    }

    @GetMapping(value = "/produto/imagem/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImagem(@PathVariable Long id) {
        Produto produto = produtoRepository.findById(id).orElse(null);
        if (produto != null && produto.getImagemBlob() != null) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(produto.getImagemBlob());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/produtos/detalhes/{id}")
    public String detalhesProduto(@PathVariable Long id, Model model) {
        Produto produto = produtoRepository.findById(id).orElse(new Produto());
        Pageable pageable = PageRequest.of(0, 50);
        model.addAttribute("produto", produto);
        model.addAttribute("categorias", categoriaRepository.findAll(pageable).getContent());
        model.addAttribute("impostos", impostoRepository.findAll());
        return "detalhesProduto";
    }

    @PostMapping("/produtos/editar")
    public String editarProduto(
            @RequestParam("id") Long id,
            @RequestParam("nome") String nome,
            @RequestParam("descricao") String descricao,
            @RequestParam("preco") double preco,
            @RequestParam("quantidadeEstoque") Double quantidadeEstoque,
            @RequestParam("codigoBarra") String codigoBarra,
            @RequestParam("categoriaId") Long categoriaId,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem,
            @RequestParam(value = "ivaPercentual", required = false) Double ivaPercentual,
            RedirectAttributes redirectAttributes) throws IOException {

        // Buscar o produto pelo ID
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + id));

        // Atualizar campos
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(quantidadeEstoque);
        produto.setCodigoBarra(codigoBarra);
        produto.setIvaPercentual(ivaPercentual);

        // Atualizar categoria
        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        produto.setCategoria(categoria);

        // Atualizar imagem se houver upload
        if (imagem != null && !imagem.isEmpty()) {
            produto.setImagemBlob(imagem.getBytes());
            produto.setImagem("/produto/imagem/" + produto.getId());
        }

        // Salvar alterações no banco
        produtoRepository.save(produto);

        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.produto.atualizado", null, LocaleContextHolder.getLocale()));
        // Redirecionar para a listagem com os dados atualizados
        return "redirect:/produtos/listar";
    }

    @GetMapping("/produtos/apagar/{id}")
    public String apagarProduto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        produtoRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.produto.apagado", null, LocaleContextHolder.getLocale()));
        return "redirect:/produtos/listar";
    }
    @GetMapping("/api/produtos/pesquisar")
    public ResponseEntity<java.util.List<Produto>> pesquisarProdutos(@RequestParam("nome") String nome) {
        if (nome == null || nome.length() < 1) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        return ResponseEntity.ok(produtoRepository.findByNomeStartingWithIgnoreCase(nome));
    }
}