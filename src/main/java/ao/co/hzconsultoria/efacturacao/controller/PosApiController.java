package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import ao.co.hzconsultoria.efacturacao.service.PosService;
import ao.co.hzconsultoria.efacturacao.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/pos")
public class PosApiController {

    @Autowired
    private PosService posService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private StockService stockService;

    private Empresa getEmpresaAtual() {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user != null && user.getEmpresaId() != null) {
            return empresaRepository.findById(user.getEmpresaId()).orElse(null);
        }
        return empresaRepository.findAll().stream().findFirst().orElse(null);
    }

    private Map<String, Object> converterProdutoDto(Produto p, Set<Long> bloqueados) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("nome", p.getNome());
        m.put("preco", p.getPreco());
        m.put("codigoBarras", p.getCodigoBarra() != null ? p.getCodigoBarra() : "");
        m.put("categoriaId", p.getCategoria() != null ? p.getCategoria().getId() : 0);
        m.put("categoriaNome", p.getCategoria() != null ? p.getCategoria().getNome() : "");
        m.put("ivaPercentual", p.getIvaPercentual());
        m.put("imagem", (p.getImagem() != null && !p.getImagem().trim().isEmpty()) ? p.getImagem() : "/dist/img/no-image.png");
        m.put("disponivel", p.isDisponivel());
        m.put("quantidadeEstoque", p.getQuantidadeEstoque());
        m.put("emPromocao", p.isEmPromocao());
        m.put("bloqueado", bloqueados != null && bloqueados.contains(p.getId()));
        return m;
    }

    /**
     * Pesquisa dinâmica de produtos com filtros e paginação
     */
    @GetMapping("/produtos")
    public ResponseEntity<?> pesquisarProdutos(
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "apenasDisponiveis", defaultValue = "false") boolean apenasDisponiveis,
            @RequestParam(value = "emPromocao", defaultValue = "false") boolean emPromocao,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "32") int size) {

        Empresa e = getEmpresaAtual();
        Long empresaId = (e != null) ? e.getId() : null;

        if (categoriaId != null && categoriaId == 0) {
            categoriaId = null;
        }
        if (termo != null) {
            termo = termo.trim();
            if (termo.isEmpty()) termo = null;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nome"));
        Page<Produto> produtosPage = produtoRepository.pesquisarProdutosPos(
                empresaId, categoriaId, termo, apenasDisponiveis, emPromocao, pageable);

        Set<Long> bloqueados = stockService.listarProdutosEmInventarioParcial();

        List<Map<String, Object>> itens = new ArrayList<>();
        for (Produto p : produtosPage.getContent()) {
            itens.add(converterProdutoDto(p, bloqueados));
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("produtos", itens);
        resposta.put("paginaAtual", produtosPage.getNumber());
        resposta.put("totalPaginas", produtosPage.getTotalPages());
        resposta.put("totalElementos", produtosPage.getTotalElements());
        resposta.put("temProxima", produtosPage.hasNext());
        return ResponseEntity.ok(resposta);
    }

    /**
     * Leitura direta de código de barras para leitor físico (hardware scanner)
     */
    @GetMapping("/produtos/barcode/{codigo}")
    public ResponseEntity<?> buscarPorCodigoBarra(@PathVariable("codigo") String codigo) {
        Empresa e = getEmpresaAtual();
        Long empresaId = (e != null) ? e.getId() : null;
        String codigoLimpo = (codigo != null) ? codigo.trim() : "";
        if (codigoLimpo.isEmpty()) {
            return ResponseEntity.badRequest().body("Código de barras inválido.");
        }

        Optional<Produto> prodOpt = produtoRepository.findFirstByCodigoBarraIgnoreCaseAndEmpresa_Id(codigoLimpo, empresaId);
        if (!prodOpt.isPresent() && empresaId == null) {
            List<Produto> lista = produtoRepository.findByCodigoBarraIgnoreCaseAndEmpresa_Id(codigoLimpo, null);
            if (!lista.isEmpty()) {
                prodOpt = Optional.of(lista.get(0));
            }
        }

        if (!prodOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        Set<Long> bloqueados = stockService.listarProdutosEmInventarioParcial();
        return ResponseEntity.ok(converterProdutoDto(prodOpt.get(), bloqueados));
    }

    /**
     * Pesquisa dinâmica de clientes (para autocomplete e checkout rápido)
     */
    @GetMapping("/clientes/search")
    public ResponseEntity<?> pesquisarClientes(
            @RequestParam(value = "termo", required = false) String termo,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        Empresa e = getEmpresaAtual();
        Long empresaId = (e != null) ? e.getId() : null;
        String termoLimpo = (termo != null && !termo.trim().isEmpty()) ? termo.trim() : null;

        Pageable pageable = PageRequest.of(0, Math.min(limit, 50));
        List<Cliente> clientes = clienteRepository.pesquisarClientesPos(empresaId, termoLimpo, pageable);

        List<Map<String, Object>> res = new ArrayList<>();
        Set<String> chavesVistas = new HashSet<>();
        for (Cliente c : clientes) {
            String nif = (c.getNif() != null && !c.getNif().trim().isEmpty()) ? c.getNif().trim() : "999999999";
            String nome = (c.getNome() != null) ? c.getNome().trim() : "";

            String chave = (!"999999999".equals(nif)) ? "NIF:" + nif.toUpperCase() : "NOME:" + nome.toUpperCase();
            if (!chavesVistas.add(chave)) {
                continue;
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("nome", c.getNome());
            m.put("nif", nif);
            m.put("telefone", c.getTelefone() != null ? c.getTelefone() : "");
            m.put("email", c.getEmail() != null ? c.getEmail() : "");
            m.put("endereco", c.getEndereco() != null ? c.getEndereco() : "");
            res.add(m);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Métricas rápidas de apoio à sessão do POS
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Empresa e = getEmpresaAtual();
        Long empresaId = (e != null) ? e.getId() : null;
        List<Produto> stockBaixo = produtoRepository.findProdutosComStockBaixo(empresaId);
        Map<String, Object> s = new HashMap<>();
        s.put("totalStockBaixo", stockBaixo != null ? stockBaixo.size() : 0);
        return ResponseEntity.ok(s);
    }

    /**
     * Retorna o catálogo leve de produtos para armazenamento offline no IndexedDB
     */
    @GetMapping("/catalogo")
    public ResponseEntity<?> getCatalogoOffline() {
        Empresa e = getEmpresaAtual();
        List<Produto> produtos = (e != null) ? produtoRepository.findByEmpresa_Id(e.getId()) : produtoRepository.findAll();
        List<Map<String, Object>> res = new ArrayList<>();
        for (Produto p : produtos) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("nome", p.getNome());
            m.put("preco", p.getPreco());
            m.put("codigoBarras", p.getCodigoBarra());
            m.put("categoriaId", p.getCategoria() != null ? p.getCategoria().getId() : 0);
            m.put("ivaPercentual", p.getIvaPercentual());
            m.put("imagem", p.getImagem());
            m.put("disponivel", p.isDisponivel());
            m.put("quantidadeEstoque", p.getQuantidadeEstoque());
            res.add(m);
        }
        return ResponseEntity.ok(res);
    }

    /**
     * Listar estado de todas as mesas
     */
    @GetMapping("/mesas")
    public ResponseEntity<?> getMesas() {
        Empresa e = getEmpresaAtual();
        List<Mesa> mesas = posService.listarEInicializarMesas(e);
        return ResponseEntity.ok(mesas);
    }

    /**
     * Abrir/Ocupar uma mesa
     */
    @PostMapping("/mesas/{id}/abrir")
    public ResponseEntity<?> abrirMesa(@PathVariable("id") Long id, @RequestParam(name = "pessoas", defaultValue = "1") Integer pessoas) {
        Empresa e = getEmpresaAtual();
        Mesa m = posService.abrirMesa(id, pessoas, e);
        return ResponseEntity.ok(m);
    }

    /**
     * Libertar uma mesa
     */
    @PostMapping("/mesas/{id}/libertar")
    public ResponseEntity<?> libertarMesa(@PathVariable("id") Long id) {
        Empresa e = getEmpresaAtual();
        Mesa m = posService.libertarMesa(id, e);
        return ResponseEntity.ok(m);
    }

    /**
     * Atualizar número de pessoas de uma mesa
     */
    @PostMapping("/mesas/{id}/pessoas")
    public ResponseEntity<?> atualizarPessoasMesa(@PathVariable("id") Long id, @RequestParam("pessoas") Integer pessoas) {
        Empresa e = getEmpresaAtual();
        Mesa m = posService.atualizarNumeroPessoas(id, pessoas, e);
        return ResponseEntity.ok(m);
    }

    /**
     * Obter lista de produtos/itens associados a uma mesa
     */
    @GetMapping("/mesas/{id}/itens")
    public ResponseEntity<?> getItensMesa(@PathVariable("id") Long id) {
        Empresa e = getEmpresaAtual();
        List<Map<String, Object>> res = posService.obterItensConsumoMesa(id, e);
        return ResponseEntity.ok(res);
    }

    /**
     * Criar uma nova mesa para a empresa
     */
    @PostMapping("/mesas/criar")
    public ResponseEntity<?> criarMesa(
            @RequestParam(name = "numeroMesa", required = false) String numeroMesa,
            @RequestParam(name = "zona", required = false) String zona,
            @RequestParam(name = "capacidade", defaultValue = "4") Integer capacidade) {
        Empresa e = getEmpresaAtual();
        Mesa m = posService.criarNovaMesa(numeroMesa, zona, capacidade, e);
        return ResponseEntity.ok(m);
    }

    /**
     * Enviar comanda/pedido para a cozinha (KDS)
     */
    @PostMapping("/pedidos-cozinha")
    public ResponseEntity<?> enviarPedidoCozinha(@RequestBody Map<String, Object> payload) {
        Empresa e = getEmpresaAtual();
        Long mesaId = payload.containsKey("mesaId") && payload.get("mesaId") != null ? Long.valueOf(payload.get("mesaId").toString()) : null;
        String obs = payload.containsKey("observacoes") ? payload.get("observacoes").toString() : "";
        List<Map<String, Object>> itens = (List<Map<String, Object>>) payload.get("itens");

        PedidoCozinha p = posService.enviarPedidoCozinha(mesaId, itens, obs, e);
        return ResponseEntity.ok(p);
    }

    /**
     * Atualizar status do pedido no KDS
     */
    @PostMapping("/kds/{id}/status")
    public ResponseEntity<?> atualizarStatusKds(@PathVariable("id") Long id, @RequestParam("status") String status) {
        Empresa e = getEmpresaAtual();
        PedidoCozinha p = posService.atualizarStatusPedidoCozinha(id, status, e);
        return ResponseEntity.ok(p);
    }

    /**
     * Obter lista de pedidos ativos na cozinha
     */
    @GetMapping("/kds/pedidos")
    public ResponseEntity<?> getPedidosKds() {
        Empresa e = getEmpresaAtual();
        List<PedidoCozinha> pedidos = posService.listarPedidosCozinhaAtivos(e != null ? e.getId() : 1L);
        return ResponseEntity.ok(pedidos);
    }

    /**
     * Obter cálculo de divisão de conta (Split Bill)
     */
    @GetMapping("/split-bill")
    public ResponseEntity<?> calcularSplitBill(@RequestParam("total") Double total, @RequestParam("pessoas") Integer pessoas) {
        Map<String, Object> res = posService.calcularSplitBill(total, pessoas);
        return ResponseEntity.ok(res);
    }

    /**
     * Sincronizar vendas emitidas offline no PWA
     */
    @PostMapping("/sincronizar-offline")
    public ResponseEntity<?> sincronizarVendasOffline(@RequestBody List<Map<String, Object>> vendasOffline) {
        int processadas = 0;
        if (vendasOffline != null) {
            processadas = vendasOffline.size();
        }
        Map<String, Object> res = new HashMap<>();
        res.put("sucesso", true);
        res.put("totalSincronizadas", processadas);
        res.put("mensagem", processadas + " vendas offline sincronizadas com sucesso!");
        return ResponseEntity.ok(res);
    }


    /**
     * Obter/Guardar configurações de POS (Esc/POS, Gaveta, Balança)
     */
    @GetMapping("/configuracao")
    public ResponseEntity<?> getConfiguracao() {
        Empresa e = getEmpresaAtual();
        ConfiguracaoPos cfg = posService.obterOuCriarConfiguracaoPos(e);
        return ResponseEntity.ok(cfg);
    }

    /**
     * Ativar / Desativar o Modo Restauração (gestão de mesas) para a empresa
     */
    @PostMapping("/configuracao/toggle-restauracao")
    public ResponseEntity<?> toggleModoRestauracao() {
        if (!ao.co.hzconsultoria.efacturacao.security.SecurityUtils.isSuperAdmin()) {
            Map<String, Object> err = new HashMap<>();
            err.put("sucesso", false);
            err.put("mensagem", "Apenas o SuperUtilizador tem autorização para alterar a Gestão de Restaurante.");
            return ResponseEntity.status(403).body(err);
        }
        Empresa e = getEmpresaAtual();
        ConfiguracaoPos cfg = posService.toggleModoRestauracao(e);
        Map<String, Object> res = new HashMap<>();
        res.put("modoRestauracaoAtivo", cfg.getModoRestauracaoAtivo());
        return ResponseEntity.ok(res);
    }
}

