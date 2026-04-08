package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping("/entrada")
    public String formEntrada(Model model) {
        model.addAttribute("tipo", "ENTRA");
        model.addAttribute("titulo", "Entrada de Stock");
        model.addAttribute("produtos", produtoRepository.findAll());
        return "movimentoStock";
    }

    @GetMapping("/saida")
    public String formSaida(Model model) {
        model.addAttribute("tipo", "SAIDA");
        model.addAttribute("titulo", "Saída de Stock");
        model.addAttribute("produtos", produtoRepository.findAll());
        return "movimentoStock";
    }

    @GetMapping("/ajuste")
    public String formAjuste(Model model) {
        model.addAttribute("tipo", "AJUSTE");
        model.addAttribute("titulo", "Ajuste de Stock");
        model.addAttribute("produtos", produtoRepository.findAll());
        return "movimentoStock";
    }

    @PostMapping("/movimentar")
    public String movimentar(@RequestParam Long produtoId, 
                             @RequestParam Double quantidade, 
                             @RequestParam String tipo,
                             @RequestParam String motivo,
                             @RequestParam(required = false) String referencia,
                             @RequestParam(required = false) String origem,
                             @RequestParam(required = false) Double precoCusto,
                             @RequestParam(value = "documento", required = false) MultipartFile documento,
                             RedirectAttributes ra) throws IOException {
        try {
            byte[] blob = (documento != null && !documento.isEmpty()) ? documento.getBytes() : null;
            String nomeDoc = (documento != null && !documento.isEmpty()) ? documento.getOriginalFilename() : null;
            
            stockService.registrarMovimento(produtoId, quantidade, tipo, motivo, referencia, origem, nomeDoc, blob, precoCusto);
            ra.addFlashAttribute("mensagemSucesso", "Movimento de stock realizado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("mensagemErro", "Erro ao realizar movimento: " + e.getMessage());
        }
        return "redirect:/stock/historico";
    }

    @GetMapping("/historico")
    public String historico(Model model) {
        model.addAttribute("movimentos", stockService.listarTodos());
        return "listarMovimentos";
    }
}
