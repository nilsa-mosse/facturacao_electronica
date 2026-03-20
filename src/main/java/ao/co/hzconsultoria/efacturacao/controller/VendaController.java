package ao.co.hzconsultoria.efacturacao.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.model.Venda;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.VendaService;


@Controller
public class VendaController {

    @Autowired
    private ProdutoRepository produtoRepository;
    
    @Autowired
    private VendaService vendaService;

    @GetMapping("/pos")
    public String abrirPDV(Model model) {
    	 Pageable pageable = PageRequest.of(0, 60); // primeira página, 20 itens
        List<Produto> produtos = produtoRepository.findAll(pageable).getContent();
        model.addAttribute("produtos", produtos);
        return "pos";
    }


    @PostMapping("/finalizar")
    public String finalizarVenda(@RequestParam("itens") String itensJson) {
        // Aqui você converte JSON -> lista de objetos
        System.out.println(itensJson);

        // TODO: salvar venda no banco

        return "redirect:/pos";
    }
    
    /*
    @PostMapping("/api/compras")
    public ResponseEntity<?>  finalizarCompra(@RequestBody Compra compra) {
    	
    	System.out.println("Resultado: "+ compra.getId());
        vendaService.finalizarVenda(compra);
    	return ResponseEntity.ok("OK");
    }   
    */
    
    
    @PostMapping("/api/compras")
    public ResponseEntity<?> finalizarCompra(@RequestBody List<Compra> compras) 
    {
        if (compras == null || compras.isEmpty()) {
            return ResponseEntity.badRequest().body("Compras list cannot be null or empty");
        }
        for (Compra compra : compras) {
            if (compra.getItens() == null || compra.getItens().isEmpty()) {
                return ResponseEntity.badRequest().body("Each compra must have at least one item");
            }
        }
        vendaService.finalizarVendas(compras);
        return ResponseEntity.ok().build();
    }
    
    /*
    @GetMapping("/vendas/historico")
    public String historico(Model model) {

        List<Venda> vendas = vendaService.listarTodas();

        model.addAttribute("vendas", vendas);

        return "historico";
    }
    */
    
    
}