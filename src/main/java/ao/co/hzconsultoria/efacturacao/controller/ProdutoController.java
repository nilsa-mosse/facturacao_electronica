package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class ProdutoController {
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/cadastroProduto")
    public String cadastroProduto(Model model) {
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("produto", new Produto());
        model.addAttribute("categorias", categoriaRepository.findAll(pageable).getContent());
        return "cadastroProduto";
    }

    @PostMapping("/salvarProduto")
    public String salvarProduto(@RequestParam("nome") String nome,
                                @RequestParam("descricao") String descricao,
                                @RequestParam("preco") double preco,
                                @RequestParam("quantidadeEstoque") int quantidadeEstoque,
                                @RequestParam("codigoBarra") String codigoBarra,
                                @RequestParam("categoriaId") Long categoriaId,
                                @RequestParam(value = "imagem", required = false) MultipartFile imagem,
                                Model model) throws IOException {
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(quantidadeEstoque);
        produto.setCodigoBarra(codigoBarra);
        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        produto.setCategoria(categoria);
        // Salva primeiro para gerar o ID
        produtoRepository.save(produto);
        if (imagem != null && !imagem.isEmpty()) {
            produto.setImagemBlob(imagem.getBytes());
            produto.setImagem("/produto/imagem/" + produto.getId()); // Caminho correto
            produtoRepository.save(produto); // Salva novamente com imagem
        }
        model.addAttribute("mensagem", "Produto cadastrado com sucesso!");
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("categorias", categoriaRepository.findAll(pageable).getContent());
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
        return "detalhesProduto";
    }

    @PostMapping("/produtos/editar")
    public String editarProduto(
            @RequestParam("id") Long id,
            @RequestParam("nome") String nome,
            @RequestParam("descricao") String descricao,
            @RequestParam("preco") double preco,
            @RequestParam("quantidadeEstoque") int quantidadeEstoque,
            @RequestParam("codigoBarra") String codigoBarra,
            @RequestParam("categoriaId") Long categoriaId,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem,
            Model model) throws IOException {

        // Buscar o produto pelo ID
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + id));

        // Atualizar campos
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(quantidadeEstoque);
        produto.setCodigoBarra(codigoBarra);

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

        // Redirecionar para a listagem com os dados atualizados
        return "redirect:/produtos/listar";
    }

    @GetMapping("/produtos/apagar/{id}")
    public String apagarProduto(@PathVariable Long id) {
        produtoRepository.deleteById(id);
        return "redirect:/produtos/listar";
    }
}