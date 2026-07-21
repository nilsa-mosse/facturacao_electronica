package ao.co.hzconsultoria.efacturacao.controller;

import java.util.List;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.model.Estoque;
import ao.co.hzconsultoria.efacturacao.model.Estabelecimento;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ImpostoRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EstoqueRepository;
import ao.co.hzconsultoria.efacturacao.repository.EstabelecimentoRepository;
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
import org.springframework.web.bind.annotation.ResponseBody;
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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;

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

    @Autowired
    private EstoqueRepository estoqueRepository;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.RegimeFiscalRepository regimeFiscalRepository;

    @GetMapping({ "/cadastroProduto", "/produtos/novo" })
    public String cadastroProduto(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("produto", new Produto());
        model.addAttribute("categorias", (empresaId != null) ? categoriaRepository.findByEmpresa_Id(empresaId) : java.util.Collections.emptyList());

        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(null) : null;
        String regimeFiscal = (empresa != null && empresa.getRegimeFiscal() != null) ? empresa.getRegimeFiscal() : "GERAL";

        java.util.Set<ao.co.hzconsultoria.efacturacao.model.Imposto> impostosDoRegime = new java.util.HashSet<>();
        java.util.Optional<ao.co.hzconsultoria.efacturacao.model.RegimeFiscal> optRegime = regimeFiscalRepository.findByCodigo(regimeFiscal);
        if (optRegime.isPresent()) {
            impostosDoRegime = optRegime.get().getImpostos();
        }
        model.addAttribute("impostos", impostosDoRegime);
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
            @RequestParam(value = "precoCompra", required = false) Double precoCompra,
            RedirectAttributes redirectAttributes) {

        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            redirectAttributes.addFlashAttribute("erro", "Erro: Sessão expirada ou empresa não identificada.");
            return "redirect:/produtos/novo";
        }

        // ── Verificação de duplicados ──────────────────────────────────────────
        if (nome != null && !nome.trim().isEmpty()) {
            if (!produtoRepository.findByNomeIgnoreCaseAndEmpresa_Id(nome.trim(), empresaId).isEmpty()) {
                redirectAttributes.addFlashAttribute("erro",
                        "Já existe um produto com o nome '" + nome.trim() + "' no catálogo desta empresa.");
                return "redirect:/produtos/novo";
            }
        }
        if (codigoBarra != null && !codigoBarra.trim().isEmpty()) {
            if (!produtoRepository.findByCodigoBarraIgnoreCaseAndEmpresa_Id(codigoBarra.trim(), empresaId).isEmpty()) {
                redirectAttributes.addFlashAttribute("erro",
                        "Já existe um produto com o código de barras '" + codigoBarra.trim() + "' no catálogo.");
                return "redirect:/produtos/novo";
            }
        }
        // ──────────────────────────────────────────────────────────────────────

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
        produto.setPrecoCompra(precoCompra);

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        produto.setEmpresa(empresa);

        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        produto.setCategoria(categoria);

        try {
            produtoRepository.save(produto);

            // Criar relação inicial com a tabela de estoque por estabelecimento
            List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findByEmpresa_Id(empresaId);
            if (!estabelecimentos.isEmpty()) {
                Estabelecimento principal = estabelecimentos.get(0);
                Estoque estoque = new Estoque();
                estoque.setProduto(produto);
                estoque.setEstabelecimento(principal);
                estoque.setQuantidade(quantidadeEstoque != null ? quantidadeEstoque : 0.0);
                estoque.setUpdatedAt(java.time.LocalDateTime.now());
                estoqueRepository.save(estoque);
            }

            if (imagem != null && !imagem.isEmpty()) {
                String fileName = UUID.randomUUID().toString() + "_" + imagem.getOriginalFilename();
                Path path = Paths.get(uploadDir + fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, imagem.getBytes());

                produto.setImagem("/uploads/produtos/" + fileName);
                produto.setImagemBlob(imagem.getBytes());
                produtoRepository.save(produto);
            }

            redirectAttributes.addFlashAttribute("mensagem",
                    "Produto '" + produto.getNome() + "' cadastrado com sucesso!");
            return "redirect:/produtos/novo";

        } catch (Exception e) {
            e.printStackTrace();
            String msgErro;
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                msgErro = "Não foi possível cadastrar o produto: já existe um produto com o mesmo nome ou código de barras nesta empresa.";
            } else if (e.getMessage() != null && e.getMessage().contains("ConstraintViolation")) {
                msgErro = "Não foi possível cadastrar o produto: dados inválidos ou obrigatórios em falta. Verifique todos os campos.";
            } else if (e.getMessage() != null && e.getMessage().contains("NonUniqueResult")) {
                msgErro = "Não foi possível cadastrar o produto: foram encontrados registos duplicados na base de dados. Contacte o administrador.";
            } else {
                msgErro = "Não foi possível cadastrar o produto. Verifique se todos os campos obrigatórios estão preenchidos correctamente.";
            }
            redirectAttributes.addFlashAttribute("erro", msgErro);
            return "redirect:/produtos/novo";
        }
    }

    @GetMapping(value = "/produto/imagem/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> getImagem(@PathVariable Long id) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = produtoRepository.findById(id).orElse(null);
        if (produto != null && produto.getEmpresa() != null && produto.getEmpresa().getId().equals(empresaId)
                && produto.getImagemBlob() != null) {
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

        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(null) : null;
        String regimeFiscal = (empresa != null && empresa.getRegimeFiscal() != null) ? empresa.getRegimeFiscal() : "GERAL";

        java.util.Set<ao.co.hzconsultoria.efacturacao.model.Imposto> impostosDoRegime = new java.util.HashSet<>();
        java.util.Optional<ao.co.hzconsultoria.efacturacao.model.RegimeFiscal> optRegime = regimeFiscalRepository.findByCodigo(regimeFiscal);
        if (optRegime.isPresent()) {
            impostosDoRegime = optRegime.get().getImpostos();
        }
        model.addAttribute("impostos", impostosDoRegime);

        System.out.println("DETALHES DO PRODUTO: " + regimeFiscal);
        model.addAttribute("regimeFiscal", regimeFiscal);
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
            @RequestParam(value = "precoCompra", required = false) Double precoCompra,
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
        produto.setPrecoCompra(precoCompra);

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

            // Sincronizar com a tabela de estoque
            List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findByEmpresa_Id(empresaId);
            if (!estabelecimentos.isEmpty()) {
                Estabelecimento principal = estabelecimentos.get(0);
                Estoque estoque = estoqueRepository.findByProdutoAndEstabelecimento(produto, principal)
                        .orElse(new Estoque());

                estoque.setProduto(produto);
                estoque.setEstabelecimento(principal);
                estoque.setQuantidade(quantidadeEstoque != null ? quantidadeEstoque : 0.0);
                estoque.setUpdatedAt(java.time.LocalDateTime.now());
                estoqueRepository.save(estoque);
            }

            redirectAttributes.addFlashAttribute("mensagem",
                    "Produto '" + produto.getNome() + "' atualizado com sucesso!");
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
            // Limpar stock relacionado antes de apagar o produto
            List<Estoque> estoques = estoqueRepository.findByProduto(produto);
            estoqueRepository.deleteAll(estoques);

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
        
        if (empresaId == null) {
            // Se for SuperAdmin sem empresa, pesquisa globalmente
            return ResponseEntity.ok(produtoRepository.findByNomeContainingIgnoreCase(nome));
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

    @GetMapping("/api/produtos/retirar-promocao/{id}")
    @Transactional
    public ResponseEntity<?> retirarPromocao(@PathVariable Long id) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Produto produto = produtoRepository.findById(id).orElse(null);

        if (produto != null && produto.getEmpresa() != null && produto.getEmpresa().getId().equals(empresaId)) {
            if (produto.isEmPromocao()) {
                if (produto.getPrecoOriginal() != null) {
                    produto.setPreco(produto.getPrecoOriginal());
                }
                produto.setPrecoOriginal(null);
                produto.setEmPromocao(false);
                produtoRepository.save(produto);
            }
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(403).build();
    }

    @PostMapping("/api/produtos/config/toggle-datas")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> toggleDatas(@RequestParam("exibir") boolean exibir) {
        ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L)
                .orElse(new ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity());
        config.setExibirDatasValidade(exibir);
        configuracaoSistemaRepository.save(config);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/produtos/modelo-excel")
    public ResponseEntity<byte[]> descarregarModeloExcel() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Produtos");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Nome", "Categoria", "Descrição", "Preço Venda (Kz)", 
                "Preço Compra (Kz)", "Stock Inicial", "Código de Barras", 
                "Taxa IVA (%)", "Unidade de Medida"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Exemplo 1
            Row ex1 = sheet.createRow(1);
            ex1.createCell(0).setCellValue("Água Mineral 1.5L");
            ex1.createCell(1).setCellValue("Bebidas");
            ex1.createCell(2).setCellValue("Água mineral natural sem gás");
            ex1.createCell(3).setCellValue(250.0);
            ex1.createCell(4).setCellValue(150.0);
            ex1.createCell(5).setCellValue(50.0);
            ex1.createCell(6).setCellValue("5601234567890");
            ex1.createCell(7).setCellValue(14.0);
            ex1.createCell(8).setCellValue("Unidade (UN)");

            // Exemplo 2
            Row ex2 = sheet.createRow(2);
            ex2.createCell(0).setCellValue("Caderno A4 100 Folhas");
            ex2.createCell(1).setCellValue("Papelaria");
            ex2.createCell(2).setCellValue("Caderno pautado capa dura");
            ex2.createCell(3).setCellValue(1200.0);
            ex2.createCell(4).setCellValue(800.0);
            ex2.createCell(5).setCellValue(20.0);
            ex2.createCell(6).setCellValue("5609876543210");
            ex2.createCell(7).setCellValue(14.0);
            ex2.createCell(8).setCellValue("Unidade (UN)");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=modelo_importacao_produtos.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/produtos/importar-excel")
    @Transactional
    public String importarExcel(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            redirectAttributes.addFlashAttribute("erro", "Erro: Sessão expirada ou empresa não identificada.");
            return "redirect:/cadastroProduto";
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("erro", "Por favor, selecione um ficheiro Excel válido.");
            return "redirect:/cadastroProduto";
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            redirectAttributes.addFlashAttribute("erro", "Empresa não encontrada.");
            return "redirect:/cadastroProduto";
        }

        int importados = 0;
        int ignorados = 0;
        List<String> erros = new java.util.ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                redirectAttributes.addFlashAttribute("erro", "A folha de cálculo não contém dados para importar.");
                return "redirect:/cadastroProduto";
            }

            List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findByEmpresa_Id(empresaId);
            Estabelecimento estabPrincipal = estabelecimentos.isEmpty() ? null : estabelecimentos.get(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nome = getCellValueAsString(row.getCell(0));
                if (nome == null || nome.trim().isEmpty()) {
                    continue;
                }

                String nomeCategoria = getCellValueAsString(row.getCell(1));
                String descricao = getCellValueAsString(row.getCell(2));
                Double precoVenda = getCellValueAsDouble(row.getCell(3));
                Double precoCompra = getCellValueAsDouble(row.getCell(4));
                Double stockInicial = getCellValueAsDouble(row.getCell(5));
                String codigoBarra = getCellValueAsString(row.getCell(6));
                Double ivaPercentual = getCellValueAsDouble(row.getCell(7));
                String unidadeMedida = getCellValueAsString(row.getCell(8));

                if (precoVenda == null || precoVenda <= 0) {
                    ignorados++;
                    erros.add("Linha " + (i + 1) + " ('" + nome + "'): Preço de venda inválido ou ausente.");
                    continue;
                }

                // ── Verificação de duplicados na importação ────────────────────
                if (!produtoRepository.findByNomeIgnoreCaseAndEmpresa_Id(nome.trim(), empresaId).isEmpty()) {
                    ignorados++;
                    erros.add("Linha " + (i + 1) + " ('" + nome + "'): Produto com este nome já existe no catálogo. Ignorado.");
                    continue;
                }
                if (codigoBarra != null && !codigoBarra.trim().isEmpty()) {
                    if (!produtoRepository.findByCodigoBarraIgnoreCaseAndEmpresa_Id(codigoBarra.trim(), empresaId).isEmpty()) {
                        ignorados++;
                        erros.add("Linha " + (i + 1) + " ('" + nome + "'): Código de barras '" + codigoBarra.trim() + "' já existe no catálogo. Ignorado.");
                        continue;
                    }
                }
                // ───────────────────────────────────────────────────────────────

                // Tratar Categoria
                Categoria categoria = null;
                if (nomeCategoria != null && !nomeCategoria.trim().isEmpty()) {
                    String catNomeTrim = nomeCategoria.trim();
                    List<Categoria> cats = categoriaRepository.findByEmpresa_Id(empresaId);
                    for (Categoria c : cats) {
                        if (c.getNome() != null && c.getNome().equalsIgnoreCase(catNomeTrim)) {
                            categoria = c;
                            break;
                        }
                    }
                    if (categoria == null) {
                        categoria = new Categoria();
                        categoria.setNome(catNomeTrim);
                        categoria.setEmpresa(empresa);
                        categoria = categoriaRepository.save(categoria);
                    }
                }

                Produto produto = new Produto();
                produto.setNome(nome.trim());
                produto.setDescricao(descricao != null ? descricao.trim() : "");
                produto.setPreco(precoVenda);
                produto.setPrecoCompra(precoCompra != null ? precoCompra : 0.0);
                produto.setQuantidadeEstoque(stockInicial != null ? stockInicial : 0.0);
                produto.setCodigoBarra(codigoBarra != null ? codigoBarra.trim() : "");
                produto.setIvaPercentual(ivaPercentual != null ? ivaPercentual : 0.0);
                produto.setUnidadeMedida((unidadeMedida != null && !unidadeMedida.trim().isEmpty()) ? unidadeMedida.trim() : "Unidade (UN)");
                produto.setEmpresa(empresa);
                produto.setCategoria(categoria);

                produtoRepository.save(produto);

                if (estabPrincipal != null) {
                    Estoque estoque = new Estoque();
                    estoque.setProduto(produto);
                    estoque.setEstabelecimento(estabPrincipal);
                    estoque.setQuantidade(produto.getQuantidadeEstoque() != null ? produto.getQuantidadeEstoque() : 0.0);
                    estoque.setUpdatedAt(java.time.LocalDateTime.now());
                    estoqueRepository.save(estoque);
                }

                importados++;
            }

            if (importados > 0) {
                String msg = "Importação concluída com sucesso! " + importados + " produto(s) cadastrado(s).";
                if (ignorados > 0) {
                    msg += " (" + ignorados + " produto(s) ignorado(s) por dados inválidos)";
                }
                redirectAttributes.addFlashAttribute("mensagem", msg);
            } else {
                redirectAttributes.addFlashAttribute("erro", "Nenhum produto foi importado. Verifique os dados no ficheiro Excel.");
            }

            if (!erros.isEmpty()) {
                redirectAttributes.addFlashAttribute("errosImportacao", erros);
            }

        } catch (Exception e) {
            e.printStackTrace();
            String msgErro;
            if (e.getMessage() != null && e.getMessage().contains("NonUniqueResult")) {
                msgErro = "Existem produtos duplicados na base de dados que impediram a importação. Verifique o catálogo antes de importar novos produtos.";
            } else if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                msgErro = "Alguns produtos do ficheiro Excel já existem no catálogo (nome ou código de barras duplicado). Corrija o ficheiro e tente novamente.";
            } else {
                msgErro = "Não foi possível processar o ficheiro Excel. Verifique se o formato está correcto e se os dados estão preenchidos conforme o modelo oficial.";
            }
            redirectAttributes.addFlashAttribute("erro", msgErro);
        }

        return "redirect:/cadastroProduto";
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double doubleVal = cell.getNumericCellValue();
                if (doubleVal == (long) doubleVal) {
                    return String.format("%d", (long) doubleVal);
                } else {
                    return String.valueOf(doubleVal);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String str = cell.getStringCellValue().trim().replace(",", ".");
                    return Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }
}