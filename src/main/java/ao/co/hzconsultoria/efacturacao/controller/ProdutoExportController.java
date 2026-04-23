package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProdutoExportController {

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping("/produtos/exportar")
    public ResponseEntity<?> exportar(@RequestParam("formato") String formato,
                                    @RequestParam(value = "busca", required = false) String busca,
                                    @RequestParam(value = "cat", required = false) Long cat) {

        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            List<Produto> produtos;
            
            if (cat != null) {
                produtos = produtoRepository.findByCategoria_IdAndEmpresa_Id(cat, empresaId);
            } else {
                produtos = produtoRepository.findByEmpresa_Id(empresaId);
            }

            if (busca != null && !busca.isEmpty()) {
                String b = busca.toLowerCase();
                produtos = produtos.stream()
                        .filter(p -> p.getNome().toLowerCase().contains(b))
                        .collect(Collectors.toList());
            }

            switch (formato.toLowerCase()) {
                case "json":
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(produtos);
                case "excel":
                    return exportToCsv(produtos);
                case "pdf":
                    return exportToPdf(produtos);
                default:
                    return ResponseEntity.badRequest().body("Formato não suportado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao gerar documento: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> exportToCsv(List<Produto> produtos) {
        StringBuilder csv = new StringBuilder();
        // Cabeçalho com BOM para o Excel reconhecer UTF-8 (acentos)
        csv.append('\ufeff'); 
        csv.append("ID;Nome;Categoria;Preço;Stock;Código\n");

        for (Produto p : produtos) {
            csv.append(p.getId()).append(";")
               .append(p.getNome()).append(";")
               .append(p.getCategoria() != null ? p.getCategoria().getNome() : "Geral").append(";")
               .append(p.getPreco()).append(";")
               .append(p.getQuantidadeEstoque() != null ? p.getQuantidadeEstoque() : 0).append(";")
               .append(p.getCodigoBarra() != null ? p.getCodigoBarra() : "").append("\n");
        }

        byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=produtos.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");
        return new ResponseEntity<>(out, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> exportToPdf(List<Produto> produtos) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        font.setSize(18);
        font.setColor(Color.BLUE);

        Paragraph p = new Paragraph("Lista de Artigos", font);
        p.setAlignment(Paragraph.ALIGN_CENTER);
        document.add(p);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);

        String[] headers = {"Nome", "Categoria", "Preço", "Stock", "Código"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (Produto prod : produtos) {
            table.addCell(prod.getNome());
            table.addCell(prod.getCategoria() != null ? prod.getCategoria().getNome() : "Geral");
            table.addCell(String.format("%.2f Kz", prod.getPreco()));
            table.addCell(String.valueOf(prod.getQuantidadeEstoque() != null ? prod.getQuantidadeEstoque() : 0));
            table.addCell(prod.getCodigoBarra() != null ? prod.getCodigoBarra() : "");
        }

        document.add(table);
        document.close();

        HttpHeaders headersPdf = new HttpHeaders();
        headersPdf.add("Content-Disposition", "attachment; filename=produtos.pdf");
        headersPdf.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(out.toByteArray(), headersPdf, HttpStatus.OK);
    }
}
