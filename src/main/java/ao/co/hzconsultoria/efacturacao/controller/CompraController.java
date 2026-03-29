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
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.FaturaService;
import ao.co.hzconsultoria.efacturacao.service.VendaService;


@Controller
public class CompraController {

    @Autowired
    private ProdutoRepository produtoRepository;
    
    @Autowired
    private CategoriaRepository categoriaRepository;
    
    @Autowired
    private VendaService vendaService;
    
    @Autowired
    private FaturaService faturaService;

    @GetMapping("/pos")
    public String abrirPDV(Model model) {
    	 Pageable pageable = PageRequest.of(0, 60); // primeira página, 60 itens
        List<Produto> produtos = produtoRepository.findAll(pageable).getContent();
        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", categoriaRepository.findAll());
        return "pos";
    }


    @PostMapping("/finalizar")
    public String finalizarVenda(@RequestParam("itens") String itensJson) {
        // Aqui você converte JSON -> lista de objetos
        System.out.println(itensJson);

        // TODO: salvar venda no banco

        return "redirect:/pos";
    }
    
    @PostMapping("/api/compras/single")
    public ResponseEntity<?> finalizarCompraSingle(@RequestBody Compra compra) {
        if (compra == null || compra.getItens() == null || compra.getItens().isEmpty()) {
            return ResponseEntity.badRequest().body("Compra ou itens não podem ser nulos ou vazios");
        }
        compra.getItens().forEach(item -> item.setCompra(compra));
        Compra compraSalva = vendaService.finalizarVenda(compra);
        Fatura fatura = faturaService.emitirFatura(compraSalva);
        String pdfFile = "/faturas/" + fatura.getNumeroFatura() + ".pdf";
        return ResponseEntity.ok().body(pdfFile);
    }
    
}