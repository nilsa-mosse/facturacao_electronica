package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ImpostoRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.service.ProdutoService;
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
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;


@Controller
public class ProdutoController {
    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ImpostoRepository impostoRepository;

    @Value("${app.upload.dir:./uploads/produtos/}")
    private String uploadDir;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ProdutoService produtoService;

    @GetMapping({"/cadastroProduto", "/produtos/novo"})

    public String cadastroProduto(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("produto", new Produto());
        model.addAttribute("categorias", categoriaRepository.findAll());

        model.addAttribute("impostos", impostoRepository.findAll());
        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(null) : null;
        String regimeFiscal = (empresa != null && empresa.getRegimeFiscal() != null) ? empresa.getRegimeFiscal() : "GERAL";
        model.addAttribute("regimeFiscal", regimeFiscal);
        return "cadastroProduto";
    }

    @PostMapping("/salvarProduto")
    @Transactional
    public String salvarProduto(@RequestParam("nome") String nome,
            @RequestParam("descricao") String descricao,
            @RequestParam("preco") double preco,
            @RequestParam("quantidadeEstoque") Double quantidadeEstoque,
            @RequestParam("codigoBarra") String codigoBarra,
            @RequestParam("categoriaId") Long categoriaId,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem,
            @RequestParam(value = "ivaPercentual", required = false) Double ivaPercentual,
            @RequestParam(value = "dataFabrico", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFabrico,
            @RequestParam(value = "dataExpiracao", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataExpiracao,
            @RequestParam(value = "unidadeMedida", required = false) String unidadeMedida,
            RedirectAttributes redirectAttributes) {

        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(quantidadeEstoque);
        produto.setCodigoBarra(codigoBarra);
        produto.setIvaPercentual(ivaPercentual);
        produto.setDataFabrico(dataFabrico);
        produto.setDataExpiracao(dataExpiracao);
        produto.setUnidadeMedida(unidadeMedida);


        
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        produto.setEmpresa(empresa);

        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        produto.setCategoria(categoria);
        
        try {
            produtoRepository.save(produto);
            
            if (imagem != null && !imagem.isEmpty()) {
                String fileName = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, imagem.getBytes());
                
                produto.setImagem("/uploads/produtos/" + fileName);
                produto.setImagemBlob(imagem.getBytes());
                produtoRepository.save(produto);
            }
            
            redirectAttributes.addFlashAttribute("mensagem", "Produto '" + produto.getNome() + "' cadastrado com sucesso!");
            return "redirect:/produtos/novo";
            
        } catch (Exception e) {
            System.err.println("Erro ao cadastrar produto: " + e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao cadastrar produto: " + e.getMessage());
            return "redirect:/produtos/novo";
        }
    }

    @GetMapping(value = "/produto/imagem/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImagem(@PathVariable Long id) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = produtoRepository.findById(id).orElse(null);
        if (produto != null && produto.getEmpresa() != null && produto.getEmpresa().getId().equals(empresaId) && produto.getImagemBlob() != null) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(produto.getImagemBlob());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/produtos/detalhes/{id}")
    public String detalhesProduto(@PathVariable Long id, Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = produtoRepository.findById(id).orElse(new Produto());
        
        // Segurança: verificar se o produto pertence à empresa
        if (produto.getEmpresa() != null && !produto.getEmpresa().getId().equals(empresaId)) {
            return "redirect:/produtos/listar";
        }

        model.addAttribute("produto", produto);
        model.addAttribute("categorias", categoriaRepository.findAll());

        model.addAttribute("impostos", impostoRepository.findAll());
        
        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(null) : null;
        model.addAttribute("regimeFiscal", (empresa != null) ? empresa.getRegimeFiscal() : "Regime Geral");
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
            @RequestParam(value = "dataFabrico", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFabrico,
            @RequestParam(value = "dataExpiracao", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataExpiracao,
            @RequestParam(value = "unidadeMedida", required = false) String unidadeMedida,
            RedirectAttributes redirectAttributes) throws IOException {


        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado: " + id));

        // Segurança
        if (produto.getEmpresa() != null && !produto.getEmpresa().getId().equals(empresaId)) {
            return "redirect:/produtos/listar";
        }

        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setQuantidadeEstoque(quantidadeEstoque);
        produto.setCodigoBarra(codigoBarra);
        produto.setIvaPercentual(ivaPercentual);
        produto.setDataFabrico(dataFabrico);
        produto.setDataExpiracao(dataExpiracao);
        produto.setUnidadeMedida(unidadeMedida);


        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        produto.setCategoria(categoria);

        if (imagem != null && !imagem.isEmpty()) {
            try {
                String fileName = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, imagem.getBytes());
                
                produto.setImagem("/uploads/produtos/" + fileName);
                produto.setImagemBlob(imagem.getBytes());
                System.out.println("====== IMAGEM EDITADA NO DISCO: " + path.toAbsolutePath() + " ======");
            } catch (IOException e) {
                System.err.println("Erro ao editar imagem no disco: " + e.getMessage());
            }
        }

        try {
            produtoRepository.save(produto);
            redirectAttributes.addFlashAttribute("mensagem", "Produto '" + produto.getNome() + "' atualizado com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao salvar produto: " + e.getMessage());
            redirectAttributes.addFlashAttribute("erro", "Erro ao atualizar produto: " + e.getMessage());
        }

        return "redirect:/produtos/listar";
    }

    @GetMapping("/produtos/apagar/{id}")
    public String apagarProduto(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = produtoRepository.findById(id).orElse(null);
        
        if (produto != null && produto.getEmpresa() != null && produto.getEmpresa().getId().equals(empresaId)) {
            produtoRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("mensagem",
                    messageSource.getMessage("msg.produto.apagado", null, LocaleContextHolder.getLocale()));
        }
        
        return "redirect:/produtos/listar";
    }

    @GetMapping("/api/produtos/pesquisar")
    public ResponseEntity<java.util.List<Produto>> pesquisarProdutos(@RequestParam("nome") String nome) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (nome == null || nome.length() < 1) {
            return ResponseEntity.ok(new java.util.ArrayList<>());
        }
        return ResponseEntity.ok(produtoRepository.findByNomeContainingIgnoreCaseAndEmpresa_Id(nome, empresaId));
    }

    @GetMapping("/api/produtos/promover/{id}")
    @Transactional
    public ResponseEntity<?> promoverProduto(@PathVariable Long id, @RequestParam("novoPreco") Double novoPreco) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = produtoRepository.findById(id).orElse(null);
        
        if (produto != null && produto.getEmpresa() != null && produto.getEmpresa().getId().equals(empresaId)) {
            produto.setPrecoOriginal(produto.getPreco());
            produto.setPreco(novoPreco);
            produto.setEmPromocao(true);
            produtoRepository.save(produto);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).build();
    }
}