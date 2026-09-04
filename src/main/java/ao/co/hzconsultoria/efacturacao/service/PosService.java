package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PosService {

    @Autowired
    private MesaRepository mesaRepository;

    @Autowired
    private PedidoCozinhaRepository pedidoCozinhaRepository;

    @Autowired
    private ConfiguracaoPosRepository configuracaoPosRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private FaturaService faturaService;

    @Transactional
    public List<Mesa> listarEInicializarMesas(Empresa empresa) {
        if (empresa == null) return Collections.emptyList();
        List<Mesa> mesas = mesaRepository.findByEmpresaIdOrderByNumeroMesaAsc(empresa.getId());
        if (mesas.isEmpty()) {
            // Inicializar 12 mesas por defeito para a empresa
            for (int i = 1; i <= 12; i++) {
                String num = String.format("Mesa %02d", i);
                String zona = (i <= 6) ? "Sala Principal" : "Esplanada";
                Mesa m = new Mesa(num, zona, 4, empresa);
                mesas.add(mesaRepository.save(m));
            }
        }
        return mesas;
    }

    @Transactional
    public Mesa abrirMesa(Long mesaId, Integer numeroPessoas, Empresa empresa) {
        Mesa mesa = mesaRepository.findByIdAndEmpresaId(mesaId, empresa.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mesa não encontrada."));
        mesa.setStatus("OCUPADA");
        mesa.setNumeroPessoas(numeroPessoas != null && numeroPessoas > 0 ? numeroPessoas : 1);
        mesa.setDataAbertura(new Date());
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa libertarMesa(Long mesaId, Empresa empresa) {
        Mesa mesa = mesaRepository.findByIdAndEmpresaId(mesaId, empresa.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mesa não encontrada."));
        mesa.setStatus("LIVRE");
        mesa.setTotalAcumulado(0.0);
        mesa.setNumeroPessoas(1);
        mesa.setDataAbertura(null);
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa atualizarNumeroPessoas(Long mesaId, Integer numeroPessoas, Empresa empresa) {
        Mesa mesa = mesaRepository.findByIdAndEmpresaId(mesaId, empresa.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mesa não encontrada."));
        if (numeroPessoas != null && numeroPessoas > 0) {
            mesa.setNumeroPessoas(numeroPessoas);
        }
        return mesaRepository.save(mesa);
    }

    @Transactional
    public Mesa criarNovaMesa(String numeroMesa, String zona, Integer capacidade, Empresa empresa) {
        if (empresa == null) throw new IllegalArgumentException("Empresa não identificada.");
        if (numeroMesa == null || numeroMesa.trim().isEmpty()) {
            long total = mesaRepository.countByEmpresaId(empresa.getId());
            numeroMesa = String.format("Mesa %02d", total + 1);
        }
        if (zona == null || zona.trim().isEmpty()) {
            zona = "Sala Principal";
        }
        if (capacidade == null || capacidade <= 0) {
            capacidade = 4;
        }
        Mesa novaMesa = new Mesa(numeroMesa.trim(), zona.trim(), capacidade, empresa);
        novaMesa.setStatus("LIVRE");
        return mesaRepository.save(novaMesa);
    }

    @Transactional
    public PedidoCozinha enviarPedidoCozinha(Long mesaId, List<Map<String, Object>> itensDto, String observacoes, Empresa empresa) {
        Mesa mesa = null;
        if (mesaId != null) {
            mesa = mesaRepository.findByIdAndEmpresaId(mesaId, empresa.getId()).orElse(null);
            if (mesa != null && "LIVRE".equals(mesa.getStatus())) {
                mesa.setStatus("OCUPADA");
                mesa.setDataAbertura(new Date());
                mesaRepository.save(mesa);
            }
        }

        PedidoCozinha pedido = new PedidoCozinha();
        pedido.setNumeroPedido("KDS-" + System.currentTimeMillis() % 10000);
        pedido.setMesa(mesa);
        pedido.setEmpresa(empresa);
        pedido.setStatus("PENDENTE");
        pedido.setDataHora(new Date());
        pedido.setObservacoes(observacoes);

        double totalPedido = 0.0;
        List<ItemPedidoCozinha> itens = new ArrayList<>();
        if (itensDto != null) {
            for (Map<String, Object> map : itensDto) {
                if (map == null || map.get("produtoId") == null) continue;
                Long produtoId = Long.valueOf(map.get("produtoId").toString());
                Double qtd = (map.get("quantidade") != null) ? Double.valueOf(map.get("quantidade").toString()) : 1.0;
                String obs = (map.get("observacao") != null) ? map.get("observacao").toString() : "";

                Produto p = produtoRepository.findById(produtoId).orElse(null);
                String nomeProduto = (p != null) ? p.getNome() : "Item " + produtoId;
                if (p != null) {
                    totalPedido += p.getPreco() * qtd;
                }

                ItemPedidoCozinha item = new ItemPedidoCozinha(p, nomeProduto, qtd, obs);
                itens.add(item);
            }
        }
        pedido.setItens(itens);

        if (mesa != null) {
            mesa.setTotalAcumulado((mesa.getTotalAcumulado() != null ? mesa.getTotalAcumulado() : 0.0) + totalPedido);
            mesaRepository.save(mesa);
        }

        return pedidoCozinhaRepository.save(pedido);
    }

    @Transactional
    public PedidoCozinha atualizarStatusPedidoCozinha(Long pedidoId, String novoStatus, Empresa empresa) {
        PedidoCozinha p = pedidoCozinhaRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado"));
        p.setStatus(novoStatus);
        return pedidoCozinhaRepository.save(p);
    }

    public List<PedidoCozinha> listarPedidosCozinhaAtivos(Long empresaId) {
        return pedidoCozinhaRepository.findByEmpresaIdAndStatusNotOrderByDataHoraAsc(empresaId, "ENTREGUE");
    }

    public Map<String, Object> calcularSplitBill(Double totalVenda, Integer numeroPessoas) {
        Map<String, Object> res = new HashMap<>();
        if (numeroPessoas == null || numeroPessoas <= 0) numeroPessoas = 1;
        if (totalVenda == null) totalVenda = 0.0;

        double valorPorPessoa = Math.round((totalVenda / numeroPessoas) * 100.0) / 100.0;
        res.put("totalVenda", totalVenda);
        res.put("numeroPessoas", numeroPessoas);
        res.put("valorPorPessoa", valorPorPessoa);
        return res;
    }

    @Transactional
    public ConfiguracaoPos obterOuCriarConfiguracaoPos(Empresa empresa) {
        if (empresa == null) return new ConfiguracaoPos();
        return configuracaoPosRepository.findByEmpresaId(empresa.getId())
                .orElseGet(() -> configuracaoPosRepository.save(new ConfiguracaoPos(empresa)));
    }

    @Transactional
    public ConfiguracaoPos toggleModoRestauracao(Empresa empresa) {
        ConfiguracaoPos config = obterOuCriarConfiguracaoPos(empresa);
        Boolean atual = config.getModoRestauracaoAtivo();
        config.setModoRestauracaoAtivo(!atual);
        return configuracaoPosRepository.save(config);
    }

    public List<Map<String, Object>> obterItensConsumoMesa(Long mesaId, Empresa empresa) {
        if (mesaId == null) return Collections.emptyList();
        List<PedidoCozinha> pedidos = pedidoCozinhaRepository.findByMesaIdAndStatusNot(mesaId, "CANCELADO");

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (PedidoCozinha p : pedidos) {
            if (p.getItens() != null) {
                for (ItemPedidoCozinha item : p.getItens()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("pedidoId", p.getId());
                    map.put("numeroPedido", p.getNumeroPedido());
                    map.put("statusPedido", p.getStatus());
                    map.put("dataHora", p.getDataHora());
                    map.put("produtoId", item.getProduto() != null ? item.getProduto().getId() : null);
                    map.put("nomeProduto", item.getNomeProduto());
                    map.put("quantidade", item.getQuantidade());
                    double preco = item.getProduto() != null ? item.getProduto().getPreco() : 0.0;
                    map.put("precoUnitario", preco);
                    map.put("total", preco * item.getQuantidade());
                    map.put("observacao", item.getObservacao());
                    resultado.add(map);
                }
            }
        }
        return resultado;
    }
}
