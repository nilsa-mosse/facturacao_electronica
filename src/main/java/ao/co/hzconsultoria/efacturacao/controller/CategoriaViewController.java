package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/categorias")
public class CategoriaViewController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/listar")
    public String listarCategorias(Model model, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            List<Categoria> categorias = categoriaRepository.findByEmpresa_Id(empresaId);
            model.addAttribute("categorias", categorias);
        }
        return "listarCategorias";
    }

    // ── Exportar Excel ──────────────────────────────────────────────────────────
    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            List<Categoria> categorias = (empresaId != null)
                    ? categoriaRepository.findByEmpresa_Id(empresaId)
                    : Collections.emptyList();

            Sheet sheet = workbook.createSheet("Categorias");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] cols = {"ID", "Nome da Categoria"};
            int[] widths = {3000, 12000};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, widths[i]);
            }

            int rowNum = 1;
            for (Categoria cat : categorias) {
                Row row = sheet.createRow(rowNum++);
                Cell c0 = row.createCell(0);
                c0.setCellValue(cat.getId());
                c0.setCellStyle(dataStyle);
                Cell c1 = row.createCell(1);
                c1.setCellValue(cat.getNome());
                c1.setCellStyle(dataStyle);
            }

            workbook.write(out);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "categorias.xlsx");
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Exportar PDF ────────────────────────────────────────────────────────────
    @GetMapping("/exportar/pdf")
    public ResponseEntity<byte[]> exportarPdf() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            List<Categoria> categorias = (empresaId != null)
                    ? categoriaRepository.findByEmpresa_Id(empresaId)
                    : Collections.emptyList();

            com.lowagie.text.Document doc =
                    new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD,
                    new java.awt.Color(0, 32, 96));
            com.lowagie.text.Paragraph title =
                    new com.lowagie.text.Paragraph("Categorias de Produtos", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(16f);
            doc.add(title);

            com.lowagie.text.pdf.PdfPTable table =
                    new com.lowagie.text.pdf.PdfPTable(2);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{20f, 80f});

            java.awt.Color headerBg = new java.awt.Color(0, 32, 96);
            java.awt.Color evenBg   = new java.awt.Color(240, 248, 255);
            java.awt.Color borderC  = new java.awt.Color(180, 180, 180);

            com.lowagie.text.Font hFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD,
                    java.awt.Color.WHITE);
            com.lowagie.text.Font dFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL,
                    java.awt.Color.BLACK);

            for (String h : new String[]{"ID", "Nome da Categoria"}) {
                com.lowagie.text.pdf.PdfPCell hCell = new com.lowagie.text.pdf.PdfPCell(
                        new com.lowagie.text.Phrase(h, hFont));
                hCell.setBackgroundColor(headerBg);
                hCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                hCell.setPadding(6f);
                hCell.setBorderColor(borderC);
                table.addCell(hCell);
            }

            int idx = 0;
            for (Categoria cat : categorias) {
                java.awt.Color rowBg = (idx % 2 == 0) ? evenBg : java.awt.Color.WHITE;
                for (String val : new String[]{String.valueOf(cat.getId()), cat.getNome()}) {
                    com.lowagie.text.pdf.PdfPCell dCell = new com.lowagie.text.pdf.PdfPCell(
                            new com.lowagie.text.Phrase(val, dFont));
                    dCell.setBackgroundColor(rowBg);
                    dCell.setPadding(5f);
                    dCell.setBorderColor(borderC);
                    table.addCell(dCell);
                }
                idx++;
            }

            doc.add(table);

            com.lowagie.text.Font noteFont = new com.lowagie.text.Font(
                    com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC,
                    new java.awt.Color(120, 120, 120));
            doc.add(new com.lowagie.text.Paragraph(
                    "\nTotal: " + categorias.size() + " categoria(s).", noteFont));

            doc.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "categorias.pdf");
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── Exportar CSV ────────────────────────────────────────────────────────────
    @GetMapping("/exportar/csv")
    public ResponseEntity<byte[]> exportarCsv() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        List<Categoria> categorias = (empresaId != null)
                ? categoriaRepository.findByEmpresa_Id(empresaId)
                : Collections.emptyList();

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Nome da Categoria\n");
        for (Categoria cat : categorias) {
            String nome = cat.getNome() != null ? cat.getNome().replace("\"", "\"\"") : "";
            sb.append(cat.getId()).append(",\"").append(nome).append("\"\n");
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "categorias.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // ── Exportar JSON ───────────────────────────────────────────────────────────
    @GetMapping("/exportar/json")
    public ResponseEntity<byte[]> exportarJson() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        List<Categoria> categorias = (empresaId != null)
                ? categoriaRepository.findByEmpresa_Id(empresaId)
                : Collections.emptyList();

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < categorias.size(); i++) {
            Categoria cat = categorias.get(i);
            String nome = cat.getNome() != null
                    ? cat.getNome().replace("\\", "\\\\").replace("\"", "\\\"")
                    : "";
            sb.append("  {\"id\": ").append(cat.getId())
              .append(", \"nome\": \"").append(nome).append("\"}");
            if (i < categorias.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "categorias.json");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    // ── Modelo de Importação ────────────────────────────────────────────────────
    @GetMapping("/modelo-importacao")
    public ResponseEntity<byte[]> descarregarModeloExcelCategorias() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Categorias");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            Cell cell = headerRow.createCell(0);
            cell.setCellValue("Nome da Categoria");
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(0, 10000);

            String[] exemplos = {"Alimentos", "Bebidas", "Limpeza", "Higiene Pessoal"};
            for (int i = 0; i < exemplos.length; i++) {
                Row row = sheet.createRow(i + 1);
                Cell dataCell = row.createCell(0);
                dataCell.setCellValue(exemplos[i]);
                dataCell.setCellStyle(dataStyle);
            }

            workbook.write(out);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "modelo_importacao_categorias.xlsx");
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
