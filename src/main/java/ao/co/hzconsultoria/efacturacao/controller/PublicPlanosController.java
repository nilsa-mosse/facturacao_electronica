package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.config.ModuloItens;
import ao.co.hzconsultoria.efacturacao.model.PlanoPagamento;
import ao.co.hzconsultoria.efacturacao.repository.PlanoPagamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Endpoint público para retornar módulos selecionados nos planos de pagamento.
 */
@RestController
public class PublicPlanosController {

    @Autowired
    private PlanoPagamentoRepository planoPagamentoRepository;

    @GetMapping("/planos-pagamento")
    public ResponseEntity<Map<String, Object>> getPlanosSelecionados() {
        List<PlanoPagamento> planos = planoPagamentoRepository.findAll();

        // Coletar módulos distintos presentes em qualquer plano
        Set<String> modulosPresentes = planos.stream()
                .flatMap(p -> {
                    Set<String> s = p.getModulos();
                    return s == null ? Collections.<String>emptySet().stream() : s.stream();
                })
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Mapear para rótulos conhecidos (ModuloItens.MODULO_LABELS)
        Map<String, String> labels = ModuloItens.MODULO_LABELS;

        List<Map<String, Object>> modulos = modulosPresentes.stream().map(key -> {
            Map<String, Object> m = new HashMap<>();
            m.put("key", key);
            m.put("label", labels.getOrDefault(key, key));
            long count = planos.stream().filter(p -> p.getModulos() != null && p.getModulos().contains(key)).count();
            m.put("planosCount", count);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("totalPlanos", planos.size());
        resp.put("modulosSelecionadosCount", modulos.size());
        resp.put("modulos", modulos);

        return ResponseEntity.ok(resp);
    }
}
