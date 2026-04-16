package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.dto.EstoqueDTO;
import ao.co.hzconsultoria.efacturacao.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EstoqueController {

    @Autowired
    private EstoqueService estoqueService;

    @GetMapping("/api/produtos/{id}/disponibilidade")
    public ResponseEntity<List<EstoqueDTO>> disponibilidade(@PathVariable("id") Long produtoId,
                                                           @RequestParam(value = "showZero", required = false, defaultValue = "false") boolean showZero,
                                                           @RequestParam(value = "includeAll", required = false, defaultValue = "false") boolean includeAll) {
        List<EstoqueDTO> dtos = estoqueService.buscarDisponibilidadeProduto(produtoId, showZero, includeAll);
        return ResponseEntity.ok(dtos);
    }
}