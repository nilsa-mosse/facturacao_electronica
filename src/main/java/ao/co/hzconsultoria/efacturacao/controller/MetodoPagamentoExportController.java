package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.MetodoPagamento;
import ao.co.hzconsultoria.efacturacao.repository.MetodoPagamentoRepository;
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
public class MetodoPagamentoExportController {

    @Autowired
    private MetodoPagamentoRepository metodoPagamentoRepository;

    @GetMapping("/configuracoes/pagamentos/exportar")
    public ResponseEntity<?> exportar(@RequestParam("formato") String formato) {
        try {
            List<MetodoPagamento> metodos = metodoPagamentoRepository.findAll();

            switch (formato.toLowerCase()) {
                case "json":
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(metodos);
                case "excel":
                    return exportToCsv(metodos);
                case "pdf":
                    return exportToPdf(metodos);
                default:
                    return ResponseEntity.badRequest().body("Formato não suportado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao gerar documento: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> exportToCsv(List<MetodoPagamento> metodos) {
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff'); // BOM UTF-8
        csv.append("ID;Nome;Código AGT;Estado\n");

        for (MetodoPagamento mp : metodos) {
            csv.append(mp.getId()).append(";")
               .append(mp.getNome() != null ? mp.getNome() : "").append(";")
               .append(mp.getCodigoAgt() != null ? mp.getCodigoAgt() : "").append(";")
               .append(mp.isActivo() ? "Activo" : "Inactivo").append("\n");
        }

        byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=metodos-pagamento.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");
        return new ResponseEntity<>(out, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> exportToPdf(List<MetodoPagamento> metodos) throws Exception {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        titleFont.setSize(16);
        titleFont.setColor(new Color(67, 97, 238));

        Paragraph title = new Paragraph("Lista de Métodos de Pagamento", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(80f);
        table.setHorizontalAlignment(PdfPTable.ALIGN_CENTER);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{3f, 2f, 1.5f});

        String[] headers = {"Nome", "Código AGT", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setBackgroundColor(new Color(230, 235, 255));
            cell.setPadding(7);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (MetodoPagamento mp : metodos) {
            table.addCell(mp.getNome() != null ? mp.getNome() : "");
            table.addCell(mp.getCodigoAgt() != null ? mp.getCodigoAgt() : "");

            PdfPCell estadoCell = new PdfPCell(new Phrase(mp.isActivo() ? "Activo" : "Inactivo"));
            estadoCell.setBackgroundColor(mp.isActivo() ? new Color(220, 252, 231) : new Color(254, 226, 226));
            estadoCell.setPadding(5);
            estadoCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(estadoCell);
        }

        document.add(table);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA);
        footerFont.setSize(9);
        footerFont.setColor(Color.GRAY);
        Paragraph footer = new Paragraph("Total: " + metodos.size() + " método(s) de pagamento", footerFont);
        footer.setAlignment(Paragraph.ALIGN_RIGHT);
        footer.setSpacingBefore(10);
        document.add(footer);

        document.close();

        HttpHeaders pdfHeaders = new HttpHeaders();
        pdfHeaders.add("Content-Disposition", "attachment; filename=metodos-pagamento.pdf");
        pdfHeaders.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(out.toByteArray(), pdfHeaders, HttpStatus.OK);
    }
}
