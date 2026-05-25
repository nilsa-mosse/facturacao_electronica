package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Imposto;
import ao.co.hzconsultoria.efacturacao.repository.ImpostoRepository;
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

@Controller
public class ImpostoExportController {

    @Autowired
    private ImpostoRepository impostoRepository;

    @GetMapping("/configuracoes/impostos/exportar")
    public ResponseEntity<?> exportar(@RequestParam("formato") String formato) {
        try {
            List<Imposto> impostos = impostoRepository.findAll();

            switch (formato.toLowerCase()) {
                case "json":
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(impostos);
                case "excel":
                    return exportToCsv(impostos);
                case "pdf":
                    return exportToPdf(impostos);
                default:
                    return ResponseEntity.badRequest().body("Formato não suportado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao gerar documento: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> exportToCsv(List<Imposto> impostos) {
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff'); // BOM UTF-8
        csv.append("ID;Nome;Tipo;Percentagem (%);Código AGT;Motivo de Isenção\n");

        for (Imposto imp : impostos) {
            csv.append(imp.getId()).append(";")
               .append(imp.getNome() != null ? imp.getNome() : "").append(";")
               .append(imp.getTipo() != null ? imp.getTipo() : "").append(";")
               .append(imp.getPercentagem() != null ? imp.getPercentagem().toPlainString() : "0").append(";")
               .append(imp.getCodigoAgt() != null ? imp.getCodigoAgt() : "").append(";")
               .append(imp.getMotivoIsencao() != null ? imp.getMotivoIsencao() : "").append("\n");
        }

        byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=impostos.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");
        return new ResponseEntity<>(out, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> exportToPdf(List<Imposto> impostos) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        titleFont.setSize(16);
        titleFont.setColor(new Color(67, 97, 238));

        Paragraph title = new Paragraph("Lista de Impostos e Taxas", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{2.5f, 1.5f, 1.5f, 1.5f, 3f});

        String[] headers = {"Nome", "Tipo", "Percentagem", "Cód. AGT", "Motivo Isenção"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setBackgroundColor(new Color(230, 235, 255));
            cell.setPadding(7);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (Imposto imp : impostos) {
            table.addCell(imp.getNome() != null ? imp.getNome() : "");
            table.addCell(imp.getTipo() != null ? imp.getTipo() : "");
            table.addCell(imp.getPercentagem() != null ? imp.getPercentagem().toPlainString() + "%" : "0%");
            table.addCell(imp.getCodigoAgt() != null ? imp.getCodigoAgt() : "");
            table.addCell(imp.getMotivoIsencao() != null ? imp.getMotivoIsencao() : "—");
        }

        document.add(table);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA);
        footerFont.setSize(9);
        footerFont.setColor(Color.GRAY);
        Paragraph footer = new Paragraph("Total: " + impostos.size() + " imposto(s)", footerFont);
        footer.setAlignment(Paragraph.ALIGN_RIGHT);
        footer.setSpacingBefore(10);
        document.add(footer);

        document.close();

        HttpHeaders pdfHeaders = new HttpHeaders();
        pdfHeaders.add("Content-Disposition", "attachment; filename=impostos.pdf");
        pdfHeaders.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(out.toByteArray(), pdfHeaders, HttpStatus.OK);
    }
}
