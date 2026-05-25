package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.dto.MovimentacaoDTO;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.DespesaRepository;
import ao.co.hzconsultoria.efacturacao.repository.DevolucaoRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;

@Service
public class FinanceiroService {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private DespesaRepository despesaRepository;

    @Autowired
    private DevolucaoRepository devolucaoRepository;

    @Autowired
    private CompraRepository compraRepository;

    public List<MovimentacaoDTO> obterFluxoDeCaixa(LocalDate inicio, LocalDate fim) {
        List<MovimentacaoDTO> movimentacoes = new ArrayList<>();

        // Adicionar Entradas (Vendas/Compras)
        List<Compra> compras = compraRepository.findAll().stream()
            .filter(c -> c.getDataCompra() != null && !c.getDataCompra().toLocalDate().isBefore(inicio) && !c.getDataCompra().toLocalDate().isAfter(fim))
            .collect(Collectors.toList());
        
        for (Compra c : compras) {
            String desc = "Venda #" + c.getId();
            movimentacoes.add(new MovimentacaoDTO(
                c.getDataCompra(),
                desc,
                "ENTRADA",
                c.getTotal() != null ? c.getTotal() : 0.0,
                "Vendas"
            ));
        }

        // Adicionar Saídas (Despesas)
        List<Despesa> despesas = despesaRepository.findByDataDespesaBetween(inicio, fim);
        for (Despesa d : despesas) {
            movimentacoes.add(new MovimentacaoDTO(
                d.getDataDespesa().atStartOfDay(),
                d.getDescricao(),
                "SAIDA",
                d.getValor() != null ? d.getValor() : 0.0,
                d.getCategoria() != null ? d.getCategoria() : "Despesas"
            ));
        }

        // Adicionar Saídas (Devoluções)
        List<Devolucao> devolucoes = devolucaoRepository.findAll().stream()
            .filter(d -> d.getDataDevolucao() != null && !d.getDataDevolucao().toLocalDate().isBefore(inicio) && !d.getDataDevolucao().toLocalDate().isAfter(fim))
            .collect(Collectors.toList());
        for (Devolucao d : devolucoes) {
            String desc = "Devolução #" + d.getId() + (d.getFatura() != null ? " (Ref: " + d.getFatura().getNumeroFatura() + ")" : "");
            movimentacoes.add(new MovimentacaoDTO(
                d.getDataDevolucao(),
                desc,
                "SAIDA",
                (d.getTotal() != null ? d.getTotal() : 0.0) + (d.getIva() != null ? d.getIva() : 0.0),
                "Devoluções"
            ));
        }

        return movimentacoes.stream()
            .sorted(Comparator.comparing(MovimentacaoDTO::getData).reversed())
            .collect(Collectors.toList());
    }

    public Double getTotalEntradas(List<MovimentacaoDTO> movs) {
        return movs.stream().filter(m -> m.getTipo().equals("ENTRADA")).mapToDouble(MovimentacaoDTO::getValor).sum();
    }

    public Double getTotalSaidas(List<MovimentacaoDTO> movs) {
        return movs.stream().filter(m -> m.getTipo().equals("SAIDA")).mapToDouble(MovimentacaoDTO::getValor).sum();
    }

    public void gerarPdfFluxoCaixa(List<MovimentacaoDTO> movs, LocalDate inicio, LocalDate fim, String filePath) throws Exception {
        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        // Fetch company details
        Long empresaId = 1L;
        try {
            Long curId = SecurityUtils.getCurrentEmpresaId();
            if (curId != null) empresaId = curId;
        } catch (Exception e) {}
        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        String nomeEmpresa = empresa != null ? empresa.getNome() : "Empresa Desconhecida";
        String nifEmpresa = empresa != null ? empresa.getNif() : "N/D";
        String enderecoEmpresa = empresa != null ? empresa.getEndereco() : "N/D";

        // Font definitions
        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new java.awt.Color(67, 97, 238));
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new java.awt.Color(30, 41, 59));
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new java.awt.Color(100, 116, 139));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new java.awt.Color(51, 65, 85));
        Font cellFontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new java.awt.Color(51, 65, 85));
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new java.awt.Color(71, 85, 105));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Header Section (2-columns)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{6f, 4f});

        // Brand & title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("KWANZA ERP", brandFont));
        leftCell.addElement(new Paragraph("Relatório de Fluxo de Caixa", titleFont));
        headerTable.addCell(leftCell);

        // Period info
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph p1 = new Paragraph("Período: " + inicio.format(dtf) + " até " + fim.format(dtf), infoFont);
        p1.setAlignment(Element.ALIGN_RIGHT);
        Paragraph p2 = new Paragraph("Emitido em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), infoFont);
        p2.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(p1);
        rightCell.addElement(p2);
        headerTable.addCell(rightCell);

        doc.add(headerTable);

        // Spacer Line
        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(10);
        doc.add(spacer);

        // Summary Cards
        PdfPTable resumoTable = new PdfPTable(3);
        resumoTable.setWidthPercentage(100);
        resumoTable.setSpacingAfter(15f);

        double totalIn = getTotalEntradas(movs);
        double totalOut = getTotalSaidas(movs);
        double saldo = totalIn - totalOut;

        resumoTable.addCell(createSummaryCell("ENTRADAS (VENDAS)", "Kz " + String.format("%.2f", totalIn), new java.awt.Color(16, 185, 129))); // Success green
        resumoTable.addCell(createSummaryCell("SAÍDAS (DESPESAS)", "Kz " + String.format("%.2f", totalOut), new java.awt.Color(239, 68, 68))); // Danger red
        resumoTable.addCell(createSummaryCell("SALDO LÍQUIDO", "Kz " + String.format("%.2f", saldo), saldo >= 0 ? new java.awt.Color(67, 97, 238) : new java.awt.Color(239, 68, 68)));
        
        doc.add(resumoTable);

        // Movements Table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 2.2f, 3.5f, 2f, 1.3f, 2f });

        String[] headers = { "DATA", "DESCRIÇÃO", "CATEGORIA", "TIPO", "VALOR" };
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
            c.setBackgroundColor(new java.awt.Color(67, 97, 238));
            c.setPadding(8f);
            c.setBorderColor(new java.awt.Color(67, 97, 238));
            table.addCell(c);
        }

        boolean alt = false;
        java.awt.Color altColor = new java.awt.Color(248, 250, 252);
        java.awt.Color borderColor = new java.awt.Color(226, 232, 240);

        for (MovimentacaoDTO m : movs) {
            // Data
            PdfPCell dateCell = new PdfPCell(new Phrase(m.getData().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")), cellFont));
            dateCell.setPadding(7f);
            dateCell.setBorderColor(borderColor);
            if (alt) dateCell.setBackgroundColor(altColor);
            table.addCell(dateCell);

            // Descrição
            PdfPCell descCell = new PdfPCell(new Phrase(m.getDescricao(), cellFont));
            descCell.setPadding(7f);
            descCell.setBorderColor(borderColor);
            if (alt) descCell.setBackgroundColor(altColor);
            table.addCell(descCell);

            // Categoria
            PdfPCell catCell = new PdfPCell(new Phrase(m.getCategoria(), cellFont));
            catCell.setPadding(7f);
            catCell.setBorderColor(borderColor);
            if (alt) catCell.setBackgroundColor(altColor);
            table.addCell(catCell);

            // Tipo
            String tipoText = "ENTRADA".equals(m.getTipo()) ? "ENTRADA" : "SAÍDA";
            PdfPCell tipoCell = new PdfPCell(new Phrase(tipoText, cellFontBold));
            tipoCell.setPadding(7f);
            tipoCell.setBorderColor(borderColor);
            if (alt) tipoCell.setBackgroundColor(altColor);
            if ("ENTRADA".equals(m.getTipo())) {
                tipoCell.getPhrase().getFont().setColor(new java.awt.Color(16, 185, 129));
            } else {
                tipoCell.getPhrase().getFont().setColor(new java.awt.Color(239, 68, 68));
            }
            table.addCell(tipoCell);

            // Valor
            String valStr = ("ENTRADA".equals(m.getTipo()) ? "+ " : "- ") + String.format("%.2f Kz", m.getValor());
            PdfPCell vCell = new PdfPCell(new Phrase(valStr, cellFontBold));
            vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            vCell.setPadding(7f);
            vCell.setBorderColor(borderColor);
            if (alt) vCell.setBackgroundColor(altColor);
            if ("ENTRADA".equals(m.getTipo())) {
                vCell.getPhrase().getFont().setColor(new java.awt.Color(16, 185, 129));
            } else {
                vCell.getPhrase().getFont().setColor(new java.awt.Color(239, 68, 68));
            }
            table.addCell(vCell);

            alt = !alt;
        }

        doc.add(table);

        // Fiscal Compliance Section
        String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String hashOriginal = "Financeiro-" + nifEmpresa + "-" + formattedDate;
        String hashHex = Integer.toHexString(hashOriginal.hashCode()).toUpperCase();
        if (hashHex.length() < 4) hashHex = "ABCD";
        else hashHex = hashHex.substring(0, 4);
        String hashFiscal = hashHex + "-Processado por programa validado nº 382/AGT/2026";

        String qrContent = "Empresa: " + nomeEmpresa + "\nNIF: " + nifEmpresa + "\nDoc: Fluxo de Caixa\nData: " + formattedDate + "\nHash: " + hashFiscal;
        Image qrCodeImage = gerarQrCode(qrContent);

        // Spacer before footer
        Paragraph spacer2 = new Paragraph(" ");
        spacer2.setSpacingBefore(15);
        doc.add(spacer2);

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

        doc.add(footerTable);

        doc.close();
    }

    private PdfPCell createSummaryCell(String title, String val, java.awt.Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10f);
        cell.setBorderWidth(1f);
        cell.setBorderColor(new java.awt.Color(226, 232, 240));
        cell.setBackgroundColor(new java.awt.Color(248, 250, 252));
        
        Paragraph titleP = new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(100, 116, 139)));
        titleP.setSpacingAfter(4f);
        cell.addElement(titleP);
        
        Paragraph valP = new Paragraph(val, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color));
        cell.addElement(valP);
        
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