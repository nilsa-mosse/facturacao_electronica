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

import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.model.ItemDevolucao;
import ao.co.hzconsultoria.efacturacao.service.DevolucaoService;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/devolucoes")
public class DevolucaoRestController {

    private static final Logger log = LoggerFactory.getLogger(DevolucaoRestController.class);

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private DevolucaoService devolucaoService;

    @GetMapping("/buscar-fatura")
    public Map<String, Object> buscarFatura(@RequestParam String numero) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            String numLimpo = (numero != null) ? numero.trim() : "";
            Optional<Fatura> faturaOpt = faturaRepository.findByNumeroFaturaAndEmpresa_Id(numLimpo, empresaId);
            
            // Tentativa alternativa com letras maiúsculas ou busca parcial caso não encontre
            if (!faturaOpt.isPresent() && !numLimpo.isEmpty()) {
                faturaOpt = faturaRepository.findByNumeroFaturaAndEmpresa_Id(numLimpo.toUpperCase(), empresaId);
            }
            if (!faturaOpt.isPresent() && !numLimpo.isEmpty()) {
                // Tenta buscar por ID da fatura se o usuário digitou apenas o número
                try {
                    String apenasNumeros = numLimpo.replaceAll("[^0-9]", "");
                    if (!apenasNumeros.isEmpty()) {
                        Long numId = Long.parseLong(apenasNumeros);
                        List<Fatura> faturas = faturaRepository.findAll();
                        faturaOpt = faturas.stream()
                                .filter(f -> (empresaId == null || (f.getEmpresa() != null && f.getEmpresa().getId().equals(empresaId)))
                                        && (f.getId().equals(numId) || (f.getCompra() != null && f.getCompra().getId().equals(numId))))
                                .findFirst();
                    }
                } catch (Exception ignored) {}
            }
            
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

    @GetMapping("/{id}/detalhes")
    public ResponseEntity<?> obterDetalhes(@PathVariable Long id) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Devolucao dev = devolucaoService.buscarPorId(id, empresaId);
            if (dev == null) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> res = new HashMap<>();
            res.put("id", dev.getId());
            res.put("dataDevolucao", dev.getDataDevolucao() != null ? dev.getDataDevolucao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-");
            res.put("motivo", dev.getMotivo() != null ? dev.getMotivo() : "Sem motivo especificado");
            res.put("total", dev.getTotal() != null ? dev.getTotal() : 0.0);
            res.put("iva", dev.getIva() != null ? dev.getIva() : 0.0);
            res.put("operador", dev.getUsuario() != null ? dev.getUsuario().getNome() : "Sistema");
            
            if (dev.getFatura() != null && dev.getFatura().getNumeroFatura() != null) {
                res.put("faturaNumero", dev.getFatura().getNumeroFatura());
                res.put("faturaPdf", "/uploads/faturas/" + dev.getFatura().getNumeroFatura() + ".pdf");
            } else {
                res.put("faturaNumero", null);
                res.put("faturaPdf", null);
            }
            
            if (dev.getNotaCredito() != null && dev.getNotaCredito().getNumeroFatura() != null) {
                res.put("notaCreditoNumero", dev.getNotaCredito().getNumeroFatura());
                res.put("notaCreditoPdf", "/uploads/faturas/" + dev.getNotaCredito().getNumeroFatura() + ".pdf");
            } else {
                res.put("notaCreditoNumero", null);
                res.put("notaCreditoPdf", null);
            }
            
            List<Map<String, Object>> itens = new ArrayList<>();
            if (dev.getItens() != null) {
                for (ItemDevolucao item : dev.getItens()) {
                    Map<String, Object> i = new HashMap<>();
                    i.put("produtoNome", item.getProduto() != null ? item.getProduto().getNome() : "Item Devolvido");
                    i.put("quantidade", item.getQuantidade() != null ? item.getQuantidade() : 1);
                    i.put("preco", item.getPreco() != null ? item.getPreco() : 0.0);
                    i.put("ivaPercentual", item.getIvaPercentual() != null ? item.getIvaPercentual() : 0.0);
                    i.put("ivaValor", item.getIvaValor() != null ? item.getIvaValor() : 0.0);
                    i.put("subtotal", item.getSubtotal() != null ? item.getSubtotal() : 0.0);
                    itens.add(i);
                }
            }
            res.put("itens", itens);
            
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            log.error("Erro ao obter detalhes da devolução {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(Collections.singletonMap("erro", e.getMessage()));
        }
    }
}
