package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
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
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DocumentoExportController {

    @Autowired
    private FaturaRepository faturaRepository;

    @GetMapping("/documentos/exportar")
    public ResponseEntity<?> exportar(
            @RequestParam("formato") String formato,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "nif", required = false) String nif) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            List<Fatura> faturas = faturaRepository.findByEmpresa_Id(empresaId);

            if (status != null && !status.trim().isEmpty() && !"TODOS".equalsIgnoreCase(status)) {
                faturas = faturas.stream()
                        .filter(f -> status.equalsIgnoreCase(f.getStatus()))
                        .collect(Collectors.toList());
            }

            if (nif != null && !nif.trim().isEmpty()) {
                String nifLower = nif.toLowerCase();
                faturas = faturas.stream()
                        .filter(f -> f.getCompra() != null
                                && f.getCompra().getCliente() != null
                                && f.getCompra().getCliente().getNif() != null
                                && f.getCompra().getCliente().getNif().toLowerCase().contains(nifLower))
                        .collect(Collectors.toList());
            }

            switch (formato.toLowerCase()) {
                case "json":
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(faturas);
                case "excel":
                    return exportToCsv(faturas);
                case "pdf":
                    return exportToPdf(faturas);
                default:
                    return ResponseEntity.badRequest().body("Formato não suportado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao gerar documento: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> exportToCsv(List<Fatura> faturas) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff'); // BOM UTF-8
        csv.append("Número;Tipo;Data Emissão;Cliente;NIF Cliente;Total (Kz);Estado AGT\n");

        for (Fatura f : faturas) {
            String cliente = (f.getCompra() != null && f.getCompra().getCliente() != null)
                    ? f.getCompra().getCliente().getNome() : "Consumidor Final";
            String nifCliente = (f.getCompra() != null && f.getCompra().getCliente() != null)
                    ? f.getCompra().getCliente().getNif() : "999999999";

            csv.append(f.getNumeroFatura() != null ? f.getNumeroFatura() : "").append(";")
               .append(f.getTipoDocumento() != null ? f.getTipoDocumento() : "FT").append(";")
               .append(f.getDataEmissao() != null ? sdf.format(f.getDataEmissao()) : "").append(";")
               .append(cliente).append(";")
               .append(nifCliente != null ? nifCliente : "").append(";")
               .append(f.getTotal() != null ? String.format("%.2f", f.getTotal()) : "0.00").append(";")
               .append(f.getStatus() != null ? f.getStatus() : "").append("\n");
        }

        byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=documentos.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");
        return new ResponseEntity<>(out, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> exportToPdf(List<Fatura> faturas) throws Exception {
        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
        titleFont.setSize(16);
        titleFont.setColor(new Color(67, 97, 238));

        Paragraph title = new Paragraph("Listagem de Documentos Fiscais", titleFont);
        title.setAlignment(Paragraph.ALIGN_CENTER);
        title.setSpacingAfter(15);
        document.add(title);

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100f);
        table.setSpacingBefore(10);
        table.setWidths(new float[]{2f, 1f, 1.5f, 2.5f, 1.5f, 1.5f});

        String[] headers = {"Número", "Tipo", "Data", "Cliente", "Total (Kz)", "Estado"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setBackgroundColor(new Color(230, 235, 255));
            cell.setPadding(7);
            cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            table.addCell(cell);
        }

        for (Fatura f : faturas) {
            String cliente = (f.getCompra() != null && f.getCompra().getCliente() != null)
                    ? f.getCompra().getCliente().getNome() : "Consumidor Final";

            table.addCell(f.getNumeroFatura() != null ? f.getNumeroFatura() : "");
            table.addCell(f.getTipoDocumento() != null ? f.getTipoDocumento() : "FT");
            table.addCell(f.getDataEmissao() != null ? sdf.format(f.getDataEmissao()) : "");
            table.addCell(cliente);
            table.addCell(f.getTotal() != null ? String.format("%.2f", f.getTotal()) : "0.00");
            table.addCell(f.getStatus() != null ? f.getStatus() : "");
        }

        document.add(table);

        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA);
        footerFont.setSize(9);
        footerFont.setColor(Color.GRAY);
        Paragraph footer = new Paragraph("Total: " + faturas.size() + " documento(s)", footerFont);
        footer.setAlignment(Paragraph.ALIGN_RIGHT);
        footer.setSpacingBefore(10);
        document.add(footer);

        document.close();

        HttpHeaders pdfHeaders = new HttpHeaders();
        pdfHeaders.add("Content-Disposition", "attachment; filename=documentos.pdf");
        pdfHeaders.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(out.toByteArray(), pdfHeaders, HttpStatus.OK);
    }
}
