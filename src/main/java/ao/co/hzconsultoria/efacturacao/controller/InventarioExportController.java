package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.Inventario;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.InventarioRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class InventarioExportController {

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @GetMapping("/gestao-inventarios/exportar")
    public ResponseEntity<?> exportar(
            @RequestParam("formato") String formato,
            @RequestParam(value = "busca", required = false) String busca,
            @RequestParam(value = "tipo", required = false) String tipo,
            @RequestParam(value = "estado", required = false) String estado) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            List<Inventario> inventarios = inventarioRepository.findByEmpresa_IdOrderByCreatedAtDesc(empresaId);

            if (busca != null && !busca.trim().isEmpty()) {
                String b = busca.toLowerCase();
                inventarios = inventarios.stream()
                        .filter(inv -> (inv.getCodigo() != null && inv.getCodigo().toLowerCase().contains(b))
                                || (inv.getNome() != null && inv.getNome().toLowerCase().contains(b)))
                        .collect(Collectors.toList());
            }

            if (tipo != null && !tipo.trim().isEmpty()) {
                try {
                    Inventario.TipoInventario tipoEnum = Inventario.TipoInventario.valueOf(tipo.toUpperCase());
                    inventarios = inventarios.stream()
                            .filter(inv -> tipoEnum.equals(inv.getTipo()))
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException ignored) {}
            }

            if (estado != null && !estado.trim().isEmpty()) {
                try {
                    Inventario.EstadoInventario estadoEnum = Inventario.EstadoInventario.valueOf(estado.toUpperCase());
                    inventarios = inventarios.stream()
                            .filter(inv -> estadoEnum.equals(inv.getEstado()))
                            .collect(Collectors.toList());
                } catch (IllegalArgumentException ignored) {}
            }

            switch (formato.toLowerCase()) {
                case "json":
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(inventarios);
                case "excel":
                    return exportToCsv(inventarios);
                case "pdf":
                    return exportToPdf(inventarios);
                default:
                    return ResponseEntity.badRequest().body("Formato não suportado");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao gerar documento: " + e.getMessage());
        }
    }

    private ResponseEntity<byte[]> exportToCsv(List<Inventario> inventarios) {
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff'); // BOM UTF-8
        csv.append("Código;Nome;Tipo;Armazém;Localização;Total Itens;Divergentes;Valor Diverg. (Kz);Estado\n");

        for (Inventario inv : inventarios) {
            csv.append(inv.getCodigo() != null ? inv.getCodigo() : "").append(";")
               .append(inv.getNome() != null ? inv.getNome() : "").append(";")
               .append(inv.getTipo() != null ? inv.getTipo().name() : "").append(";")
               .append(inv.getArmazem() != null ? inv.getArmazem() : "Principal").append(";")
               .append(inv.getLocalizacao() != null ? inv.getLocalizacao() : "N/D").append(";")
               .append(inv.getTotalItens()).append(";")
               .append(inv.getItensDivergentes()).append(";")
               .append(String.format("%.2f", inv.getValorDivergencias())).append(";")
               .append(inv.getEstado() != null ? inv.getEstado().name() : "").append("\n");
        }

        byte[] out = csv.toString().getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=inventarios.csv");
        headers.add("Content-Type", "text/csv; charset=utf-8");
        return new ResponseEntity<>(out, headers, HttpStatus.OK);
    }

    private ResponseEntity<byte[]> exportToPdf(List<Inventario> inventarios) throws Exception {
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
        Font cellFontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(51, 65, 85));
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(71, 85, 105));

        // Header Section
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{6f, 4f});

        // Brand and title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("KWANZA ERP", brandFont));
        leftCell.addElement(new Paragraph("Listagem Oficial de Inventários", titleFont));
        headerTable.addCell(leftCell);

        // Date and stats
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String formattedDate = now.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph p1 = new Paragraph("Emitido em: " + formattedDate, infoFont);
        p1.setAlignment(Element.ALIGN_RIGHT);
        Paragraph p2 = new Paragraph("Total: " + inventarios.size() + " inventário(s)", totalFont);
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
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1.5f, 2.5f, 1.2f, 2f, 1f, 1.8f, 1.5f});

        String[] headers = {"Código", "Nome", "Tipo", "Armazém", "Itens", "Valor Diverg.", "Estado"};
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

        for (Inventario inv : inventarios) {
            table.addCell(createCell(inv.getCodigo() != null ? inv.getCodigo() : "", cellFontBold, borderColor, alt, altColor));
            table.addCell(createCell(inv.getNome() != null ? inv.getNome() : "", cellFont, borderColor, alt, altColor));
            table.addCell(createCell(inv.getTipo() != null ? inv.getTipo().name() : "", cellFont, borderColor, alt, altColor));
            table.addCell(createCell(inv.getArmazem() != null ? inv.getArmazem() : "Principal", cellFont, borderColor, alt, altColor));
            table.addCell(createCell(String.valueOf(inv.getTotalItens()), cellFont, borderColor, alt, altColor));

            // Valor Divergências
            double valDiv = inv.getValorDivergencias();
            String valStr = String.format("%.2f Kz", valDiv);
            PdfPCell valCell = new PdfPCell(new Phrase(valStr, cellFontBold));
            valCell.setPadding(7f);
            valCell.setBorderColor(borderColor);
            if (alt) {
                valCell.setBackgroundColor(altColor);
            }
            if (valDiv < 0) {
                valCell.getPhrase().getFont().setColor(new Color(239, 68, 68)); // red
            } else if (valDiv > 0) {
                valCell.getPhrase().getFont().setColor(new Color(16, 185, 129)); // green
            }
            table.addCell(valCell);

            // Estado com cor de fundo premium (Pill)
            String estadoStr = inv.getEstado() != null ? inv.getEstado().name() : "";
            PdfPCell estadoCell = new PdfPCell(new Phrase(estadoStr, cellFontBold));
            estadoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            estadoCell.setPadding(7f);
            estadoCell.setBorderColor(borderColor);
            if (alt) {
                estadoCell.setBackgroundColor(altColor);
            }

            if ("FINALIZADO".equals(estadoStr)) {
                estadoCell.setBackgroundColor(new Color(220, 252, 231)); // light green
                estadoCell.getPhrase().getFont().setColor(new Color(21, 128, 61));
            } else if ("CANCELADO".equals(estadoStr)) {
                estadoCell.setBackgroundColor(new Color(254, 226, 226)); // light red
                estadoCell.getPhrase().getFont().setColor(new Color(185, 28, 28));
            } else if ("EM_CONTAGEM".equals(estadoStr)) {
                estadoCell.setBackgroundColor(new Color(219, 234, 254)); // light blue
                estadoCell.getPhrase().getFont().setColor(new Color(29, 78, 216));
            } else if ("EM_REVISAO".equals(estadoStr)) {
                estadoCell.setBackgroundColor(new Color(254, 243, 199)); // light yellow
                estadoCell.getPhrase().getFont().setColor(new Color(180, 83, 9));
            } else {
                estadoCell.setBackgroundColor(new Color(243, 244, 246)); // light grey
                estadoCell.getPhrase().getFont().setColor(new Color(75, 85, 99));
            }
            table.addCell(estadoCell);

            alt = !alt;
        }

        document.add(table);

        // Fiscal Compliance Section
        String hashOriginal = "Inventarios-" + nifEmpresa + "-" + formattedDate;
        String hashHex = Integer.toHexString(hashOriginal.hashCode()).toUpperCase();
        if (hashHex.length() < 4) hashHex = "ABCD";
        else hashHex = hashHex.substring(0, 4);
        String hashFiscal = hashHex + "-Processado por programa validado nº 382/AGT/2026";

        String qrContent = "Empresa: " + nomeEmpresa + "\nNIF: " + nifEmpresa + "\nDoc: Listagem de Inventarios\nData: " + formattedDate + "\nHash: " + hashFiscal;
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
        pdfHeaders.add("Content-Disposition", "attachment; filename=inventarios.pdf");
        pdfHeaders.setContentType(MediaType.APPLICATION_PDF);
        return new ResponseEntity<>(out.toByteArray(), pdfHeaders, HttpStatus.OK);
    }

    private PdfPCell createCell(String text, Font font, Color borderColor, boolean alt, Color altColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(7f);
        cell.setBorderColor(borderColor);
        if (alt) {
            cell.setBackgroundColor(altColor);
        }
        return cell;
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
