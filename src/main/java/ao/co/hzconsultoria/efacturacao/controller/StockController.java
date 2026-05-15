package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.MovimentoStock;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/documento/{id}")
    public ResponseEntity<Resource> visualizarDocumento(@PathVariable Long id) {
        MovimentoStock movimento = stockService.buscarPorId(id);
        if (movimento == null || movimento.getDocumentoBlob() == null || movimento.getDocumentoBlob().length == 0) {
            return ResponseEntity.notFound().build();
        }

        byte[] blob = movimento.getDocumentoBlob();
        String fileName = movimento.getNomeDocumento() != null ? movimento.getNomeDocumento() : "documento_" + id;
        
        // Determinar o MediaType de forma mais robusta
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String lowerFileName = fileName.toLowerCase();
        
        if (lowerFileName.endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (lowerFileName.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (lowerFileName.endsWith(".gif")) {
            mediaType = MediaType.IMAGE_GIF;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(blob.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(new ByteArrayResource(blob));
    }

    @GetMapping("/saldos")
    public String saldos(Model model) {
        model.addAttribute("produtos", stockService.listarTodosProdutos());
        return "listarSaldos";
    }

    @GetMapping("/alertas")
    public String alertas(Model model) {
        model.addAttribute("produtosEstoqueBaixo", stockService.buscarProdutosComStockBaixo());
        return "estoqueBaixo";
    }
}
