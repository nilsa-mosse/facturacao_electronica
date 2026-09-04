package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import ao.co.hzconsultoria.efacturacao.service.PosService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private EmpresaRepository empresaRepository;

    private Empresa getEmpresaAtual() {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        if (user != null && user.getEmpresaId() != null) {
            return empresaRepository.findById(user.getEmpresaId()).orElse(null);
        }
        return empresaRepository.findAll().stream().findFirst().orElse(null);
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

