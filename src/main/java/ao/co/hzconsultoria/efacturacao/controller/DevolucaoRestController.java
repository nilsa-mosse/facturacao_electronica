package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devolucoes")
public class DevolucaoRestController {

    private static final Logger log = LoggerFactory.getLogger(DevolucaoRestController.class);

    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/buscar-fatura")
    public Map<String, Object> buscarFatura(@RequestParam String numero) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<Fatura> faturaOpt = faturaRepository.findByNumeroFaturaAndEmpresa_Id(numero, empresaId);
            
            if (faturaOpt.isPresent()) {
                Fatura f = faturaOpt.get();
                response.put("sucesso", true);
                response.put("id", f.getId());
                response.put("numero", f.getNumeroFatura());
                response.put("total", f.getTotal());
                
                List<Map<String, Object>> itensList = new ArrayList<>();
                
                if (f.getCompra() != null && f.getCompra().getItens() != null) {
                    for (ItemCompra item : f.getCompra().getItens()) {
                        Map<String, Object> i = new HashMap<>();
                        if (item.getProduto() != null) {
                            i.put("produtoId", item.getProduto().getId());
                            i.put("produtoNome", item.getProduto().getNome());
                        } else {
                            i.put("produtoId", null);
                            i.put("produtoNome", item.getNomeProduto() != null ? item.getNomeProduto() : "Produto N/A");
                        }
                        i.put("quantidade", item.getQuantidade() != null ? item.getQuantidade() : 0.0);
                        i.put("preco", item.getPreco() != null ? item.getPreco() : 0.0);
                        i.put("ivaPercentual", item.getIvaPercentual() != null ? item.getIvaPercentual() : 0.0);
                        i.put("subtotal", item.getSubtotal() != null ? item.getSubtotal() : 0.0);
                        itensList.add(i);
                    }
                }
                
                response.put("itens", itensList);
            } else {
                response.put("sucesso", false);
                response.put("mensagem", "Factura '" + numero + "' não encontrada ou não pertence à sua empresa.");
            }
        } catch (Exception e) {
            log.error("Erro ao buscar factura {}: {}", numero, e.getMessage(), e);
            response.put("sucesso", false);
            response.put("mensagem", "Erro interno ao processar busca: " + e.getMessage());
        }
        return response;
    }
}
