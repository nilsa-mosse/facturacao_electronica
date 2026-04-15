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
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
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

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping("/pos")
    public String abrirPDV(Model model) {
    	 Pageable pageable = PageRequest.of(0, 60);
        List<Produto> produtos = produtoRepository.findAll(pageable).getContent();
        model.addAttribute("produtos", produtos);
        model.addAttribute("categorias", categoriaRepository.findAll());
        // Passa lista de clientes para o modal de identificacao
        model.addAttribute("clientes", clienteRepository.findAll());
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
        resolverCliente(compra);
        Compra compraSalva = vendaService.finalizarVenda(compra);
        Fatura fatura = faturaService.emitirFatura(compraSalva);
        String pdfFile = "/faturas/" + fatura.getNumeroFatura() + ".pdf";
        return ResponseEntity.ok().body(pdfFile);
    }

    @PostMapping("/api/compras/proforma")
    public ResponseEntity<?> emitirProforma(@RequestBody Compra compra) {
        if (compra == null || compra.getItens() == null || compra.getItens().isEmpty()) {
            return ResponseEntity.badRequest().body("Compra ou itens não podem ser nulos ou vazios");
        }
        compra.getItens().forEach(item -> item.setCompra(compra));
        resolverCliente(compra);
        Compra compraSalva = vendaService.finalizarVenda(compra, "FP");
        Fatura fatura = faturaService.emitirProforma(compraSalva);
        String pdfFile = "/faturas/" + fatura.getNumeroFatura() + ".pdf";
        return ResponseEntity.ok().body(pdfFile);
    }

    /**
     * Resolve os dados de identificação do cliente.
     * Se clienteId foi enviado, busca o Cliente na BD e popula os campos.
     * Caso contrário usa os dados diretos (novo cliente temporário) ou Consumidor Final.
     */
    private void resolverCliente(Compra compra) {
        if (compra.getCliente() != null && compra.getCliente().getId() != null) {
            clienteRepository.findById(compra.getCliente().getId()).ifPresent(c -> {
                compra.setCliente(c);
                compra.setNomeCliente(c.getNome());
                compra.setNifCliente(c.getNif() != null ? c.getNif() : "999999999");
            });
        } else {
            if (compra.getNomeCliente() == null || compra.getNomeCliente().trim().isEmpty()) {
                compra.setNomeCliente("Consumidor Final");
            }
            if (compra.getNifCliente() == null || compra.getNifCliente().trim().isEmpty()) {
                compra.setNifCliente("999999999");
            }
            compra.setCliente(null);
        }
    }
}