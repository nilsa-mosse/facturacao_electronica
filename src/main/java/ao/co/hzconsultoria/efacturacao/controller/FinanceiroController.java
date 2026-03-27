package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.dto.MovimentacaoDTO;
import ao.co.hzconsultoria.efacturacao.service.FinanceiroService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/financeiro")
public class FinanceiroController {

    @Autowired
    private FinanceiroService financeiroService;

    @GetMapping("/fluxo-caixa")
    public String fluxoCaixa(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {
        
        if (inicio == null) inicio = LocalDate.now().withDayOfMonth(1);
        if (fim == null) fim = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        List<MovimentacaoDTO> movs = financeiroService.obterFluxoDeCaixa(inicio, fim);
        double totalIn = financeiroService.getTotalEntradas(movs);
        double totalOut = financeiroService.getTotalSaidas(movs);

        model.addAttribute("movimentacoes", movs);
        model.addAttribute("totalEntradas", totalIn);
        model.addAttribute("totalSaidas", totalOut);
        model.addAttribute("saldoConsolidado", totalIn - totalOut);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        
        return "fluxoCaixa";
    }

    @GetMapping("/fluxo-caixa/pdf")
    public ResponseEntity<byte[]> exportarPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        
        try {
            List<MovimentacaoDTO> movs = financeiroService.obterFluxoDeCaixa(inicio, fim);
            String fileName = "Fluxo_Caixa_" + inicio + "_a_" + fim + ".pdf";
            String filePath = "target/" + fileName;
            
            financeiroService.gerarPdfFluxoCaixa(movs, inicio, fim, filePath);
            
            File file = new File(filePath);
            byte[] contents = Files.readAllBytes(file.toPath());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(fileName, fileName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            
            return ResponseEntity.ok().headers(headers).body(contents);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
