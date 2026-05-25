package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fornecedor;
import ao.co.hzconsultoria.efacturacao.repository.FornecedorRepository;
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

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;

@Controller
public class FornecedorExportController {

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @GetMapping("/fornecedores/exportar")
    public ResponseEntity<?> exportar(
            @RequestParam("formato") String formato,
            @RequestParam(value = "busca", required = false) String busca) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            List<Fornecedor> fornecedores = fornecedorRepository.findByEmpresa_Id(empresaId);

            if (busca != null && !busca.trim().isEmpty()) {
                String b = busca.toLowerCase();
                fornecedores = fornecedores.stream()
                        .filter(f -> (f.getNome() != null && f.getNome().toLowerCase().contains(b))
                                || (f.getNif() != null && f.getNif().toLowerCase().contains(b))
                                || (f.getEmail() != null && f.getEmail().toLowerCase().contains(b)))
                        .collect(Collectors.toList());
            }

            switch (formato.toLowerCase()) {
                case "json":
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(fornecedores);
                case "excel":
                    return exportToCsv(fornecedores);
                case "pdf":
                    return exportToPdf(fornecedores);
                default:
                    return ResponseEntity.badRequest().body("Formato não suportado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao gerar documento: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> exportToCsv(List<Fornecedor> fornecedores) {
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff'); // BOM UTF-8
        csv.append("ID;Nome;NIF;Email;Telefone;Endereço\n");

        for (Fornecedor f : fornecedores) {
            csv.append(f.getId()).append(";")
               .append(f.getNome() != null ? f.getNome() : "").append(";")
               .append(f.getNif() != null ? f.getNif() : "").append(";")
               .append(f.getEmail() != null ? f.getEmail() : "").append(";")
               .append(f.getTelefone() != null ? f.getTelefone() : "").append(";")
               .append(f.getEndereco() != null ? f.getEndereco() : "").append("\n");
        }

        byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=fornecedores.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");
        return new ResponseEntity<>(out, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> exportToPdf(List<Fornecedor> fornecedores) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Fetch company details
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        String nomeEmpresa = empresa != null ? empresa.getNome() : "Empresa Desconhecida";
        String nifEmpresa = empresa != null ? empresa.getNif() : "N/D";
        String enderecoEmpresa = empresa != null ? empresa.getEndereco() : "N/D";

        // Font definitions
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(67, 97, 238));
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 41, 59));
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(100, 116, 139));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(51, 65, 85));
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(71, 85, 105));
        Font cellFontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(51, 65, 85));

        // Header Section
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{6f, 4f});

        // Brand and title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("KWANZA ERP", brandFont));
        leftCell.addElement(new Paragraph("Listagem Oficial de Fornecedores", titleFont));
        headerTable.addCell(leftCell);

        // Date and stats
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String formattedDate = now.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph p1 = new Paragraph("Emitido em: " + formattedDate, infoFont);
        p1.setAlignment(Element.ALIGN_RIGHT);
        Paragraph p2 = new Paragraph("Total: " + fornecedores.size() + " fornecedor(es)", totalFont);
        p2.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(p1);
        rightCell.addElement(p2);
        headerTable.addCell(rightCell);

        document.add(headerTable);

        // Spacer Line
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(10);
        document.add(spacer);

        // Data Table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{3f, 2f, 3.5f, 2f, 4.5f});

        String[] headers = {"Nome", "NIF", "Email", "Telefone", "Endereço"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(new Color(67, 97, 238));
            cell.setPadding(8f);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBorderColor(new Color(67, 97, 238));
            table.addCell(cell);
        }

        boolean alt = false;
        Color altColor = new Color(248, 250, 252);
        Color borderColor = new Color(226, 232, 240);

        for (Fornecedor f : fornecedores) {
            String[] rowData = {
                f.getNome() != null ? f.getNome() : "",
                f.getNif() != null ? f.getNif() : "",
                f.getEmail() != null ? f.getEmail() : "",
                f.getTelefone() != null ? f.getTelefone() : "",
                f.getEndereco() != null ? f.getEndereco() : ""
            };

            for (String val : rowData) {
                PdfPCell cell = new PdfPCell(new Phrase(val, cellFont));
                cell.setPadding(7f);
                cell.setBorderColor(borderColor);
                if (alt) {
                    cell.setBackgroundColor(altColor);
                }
                table.addCell(cell);
            }
            alt = !alt;
        }

        document.add(table);

        // Fiscal Compliance Section
        String hashOriginal = "Fornecedores-" + nifEmpresa + "-" + formattedDate;
        String hashHex = Integer.toHexString(hashOriginal.hashCode()).toUpperCase();
        if (hashHex.length() < 4) hashHex = "ABCD";
        else hashHex = hashHex.substring(0, 4);
        String hashFiscal = hashHex + "-Processado por programa validado nº 382/AGT/2026";

        String qrContent = "Empresa: " + nomeEmpresa + "\nNIF: " + nifEmpresa + "\nDoc: Listagem de Fornecedores\nData: " + formattedDate + "\nHash: " + hashFiscal;
        Image qrCodeImage = gerarQrCode(qrContent);

        // Spacer before footer
        Paragraph spacer2 = new Paragraph(" ");
        spacer2.setSpacingBefore(15);
        document.add(spacer2);

        // Footer table for compliance details
        PdfPTable footerTable = new PdfPTable(2);
        footerTable.setWidthPercentage(100f);
        footerTable.setWidths(new float[]{8f, 2f});

        PdfPCell fLeft = new PdfPCell();
        fLeft.setBorder(Rectangle.NO_BORDER);
        Paragraph empPar = new Paragraph(nomeEmpresa + " | NIF: " + nifEmpresa, totalFont);
        Paragraph endPar = new Paragraph("Endereço: " + enderecoEmpresa, infoFont);
        Paragraph hashPar = new Paragraph("Hash: " + hashFiscal, cellFontBold);
        hashPar.setSpacingBefore(5f);
        fLeft.addElement(empPar);
        fLeft.addElement(endPar);
        fLeft.addElement(hashPar);
        footerTable.addCell(fLeft);

        PdfPCell fRight = new PdfPCell();
        fRight.setBorder(Rectangle.NO_BORDER);
        fRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        qrCodeImage.scaleAbsolute(60f, 60f);
        qrCodeImage.setAlignment(Element.ALIGN_RIGHT);
        fRight.addElement(qrCodeImage);
        footerTable.addCell(fRight);

        document.add(footerTable);

        document.close();

        HttpHeaders pdfHeaders = new HttpHeaders();
        pdfHeaders.add("Content-Disposition", "attachment; filename=fornecedores.pdf");
        pdfHeaders.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(out.toByteArray(), pdfHeaders, HttpStatus.OK);
    }

    private Image gerarQrCode(String text) throws Exception {
        com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 100, 100);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? java.awt.Color.BLACK.getRGB() : java.awt.Color.WHITE.getRGB());
            }
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);
        return Image.getInstance(baos.toByteArray());
    }
}
