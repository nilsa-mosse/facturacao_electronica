package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.dto.AgtResponse;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Cliente;
import ao.co.hzconsultoria.efacturacao.model.Compra;

import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Date;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

@Service
public class FaturaService {

    private static final Logger log = LoggerFactory.getLogger(FaturaService.class);

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private AgtService agtService;


    public Fatura emitirFatura(Compra compra) {
        Fatura fatura = new Fatura();
        fatura.setCompra(compra);
        String numeroFatura = "FT-" + System.currentTimeMillis();
        fatura.setNumeroFatura(numeroFatura);
        fatura.setDataEmissao(new Date());
        // Calcular totais e IVA
        double totalSemImposto = 0;
        double valorIva = 0;
        for (ao.co.hzconsultoria.efacturacao.model.ItemCompra item : compra.getItens()) {
            double subtotal = item.getSubtotal();
            totalSemImposto += subtotal;
        }
        double totalFinal = totalSemImposto + valorIva;
        fatura.setTotal(totalFinal);
        fatura.setIva(valorIva);

        // Gerar hash SHA-256 para assinatura
        String data = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(fatura.getDataEmissao());
        String hash = gerarHash(numeroFatura + data + totalFinal);
        fatura.setHash(hash);

        // Guardar fatura antes de enviar (para ter ID gerado)
        fatura.setEnviadaAGT(false);
        fatura.setStatus("PENDENTE");
        Fatura faturaSalva = faturaRepository.save(fatura);

        // Enviar para a AGT
        try {
            AgtResponse agtResponse = agtService.enviarFatura(faturaSalva);
            if (agtResponse.isSucesso()) {
                faturaSalva.setEnviadaAGT(true);
                faturaSalva.setStatus(agtResponse.getStatus() != null ? agtResponse.getStatus() : "VALIDADA");
                faturaSalva.setCodigoAgt(agtResponse.getCodigoAgt());
                log.info("Fatura {} enviada e validada pela AGT. Código: {}", numeroFatura, agtResponse.getCodigoAgt());
            } else {
                faturaSalva.setEnviadaAGT(false);
                faturaSalva.setStatus("FALHA_ENVIO");
                faturaSalva.setCodigoAgt("ERRO: " + agtResponse.getMensagem());
                log.warn("Falha no envio da fatura {} para AGT: {}", numeroFatura, agtResponse.getMensagem());
            }
        } catch (Exception e) {
            faturaSalva.setStatus("FALHA_ENVIO");
            log.error("Erro crítico ao enviar fatura {} para AGT: {}", numeroFatura, e.getMessage());
        }

        // Actualizar estado final na BD
        faturaSalva = faturaRepository.save(faturaSalva);
        gerarPdfFatura(faturaSalva);
        return faturaSalva;
    }

    public Fatura reenviarFatura(Long id) {
        Fatura fatura = faturaRepository.findById(id).orElseThrow(() -> new RuntimeException("Fatura não encontrada"));

        try {
            AgtResponse agtResponse = agtService.enviarFatura(fatura);
            if (agtResponse.isSucesso()) {
                fatura.setEnviadaAGT(true);
                fatura.setStatus(agtResponse.getStatus() != null ? agtResponse.getStatus() : "VALIDADA");
                fatura.setCodigoAgt(agtResponse.getCodigoAgt());
                log.info("Reenvio da Fatura {} foi aceite. Código: {}", fatura.getNumeroFatura(), agtResponse.getCodigoAgt());
            } else {
                fatura.setEnviadaAGT(false);
                fatura.setStatus("FALHA_ENVIO");
                fatura.setCodigoAgt("ERRO: " + agtResponse.getMensagem());
                log.warn("Falha no reenvio da fatura {}: {}", fatura.getNumeroFatura(), agtResponse.getMensagem());
            }
        } catch (Exception e) {
            fatura.setStatus("FALHA_ENVIO");
            log.error("Erro crítico ao reenviar fatura {}: {}", fatura.getNumeroFatura(), e.getMessage());
        }

        fatura = faturaRepository.save(fatura);
        if (fatura.isEnviadaAGT()) {
            gerarPdfFatura(fatura);
        }
        return fatura;
    }

    private String gerarHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash SHA-256", e);
        }
    }

    private void gerarPdfFatura(Fatura fatura) {
        try {
            // Salva em src/main/resources/static/faturas (para persistência futura)
            File dir1 = new File("src/main/resources/static/faturas");
            if (!dir1.exists())
                dir1.mkdirs();
            String filePath1 = "src/main/resources/static/faturas/" + fatura.getNumeroFatura() + ".pdf";
            // Salva em target/classes/static/faturas (para servir via Spring Boot)
            File dir2 = new File("target/classes/static/faturas");
            if (!dir2.exists())
                dir2.mkdirs();
            String filePath2 = "target/classes/static/faturas/" + fatura.getNumeroFatura() + ".pdf";

            // Gera o PDF em ambos os locais
            gerarPdf(filePath1, fatura);
            gerarPdf(filePath2, fatura);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gerarPdf(String filePath, Fatura fatura) throws Exception {

        Document doc = new Document(PageSize.A4, 40, 40, 50, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        // Background Shapes
        com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContentUnder();
        java.awt.Color dustyRose = new java.awt.Color(217, 167, 160);
        java.awt.Color darkText = new java.awt.Color(26, 26, 26);
        java.awt.Color lightGrey = new java.awt.Color(241, 241, 241);

        cb.saveState();
        cb.setColorFill(dustyRose);
        // Top right organic curve
        float w = doc.getPageSize().getWidth();
        float h = doc.getPageSize().getHeight();
        cb.moveTo(w - 200, h);
        cb.curveTo(w - 200, h - 80, w - 80, h - 180, w, h - 100);
        cb.lineTo(w, h);
        cb.fill();
        // Bottom left organic curve
        cb.moveTo(0, 180);
        cb.curveTo(80, 180, 180, 80, 100, 0);
        cb.lineTo(0, 0);
        cb.fill();
        cb.restoreState();

        Font fontFactura = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, darkText);
        Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkText);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, darkText);
        Font normalWhite = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, darkText);
        Font thankYouFont = FontFactory.getFont(FontFactory.TIMES_ITALIC, 18, darkText);

        DecimalFormat df = new DecimalFormat("#,##0.00");

        // Top Section: Title
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        PdfPCell titleCell = new PdfPCell(new Phrase("FACTURA", fontFactura));
        titleCell.setBorder(0);
        titleCell.setPaddingTop(20);
        titleCell.setPaddingBottom(30);
        header.addCell(titleCell);
        doc.add(header);

        // Client Info & Metadata
        PdfPTable clientMetaTable = new PdfPTable(2);
        clientMetaTable.setWidthPercentage(100);
        clientMetaTable.setWidths(new float[] { 6, 4 });

        // Left: Client Info
        PdfPCell clientCell = new PdfPCell();
        clientCell.setBorder(0);
        
        Cliente cliente = (fatura.getCompra() != null) ? fatura.getCompra().getCliente() : null;
        String nomeCliente = (cliente != null) ? cliente.getNome() : "Consumidor Final";
        String enderecoCliente = (cliente != null) ? cliente.getEndereco() : "Endereço Não Especificado";
        String contatoCliente = (cliente != null) ? (cliente.getEmail() + " | " + cliente.getTelefone()) : "contato@exemplo.com";
        String nifCliente = (cliente != null) ? cliente.getNif() : "999999999";

        clientCell.addElement(new Paragraph("DADOS DO CLIENTE", fontSubtitle));
        clientCell.addElement(new Paragraph(nomeCliente, bold));
        clientCell.addElement(new Paragraph(enderecoCliente, normal));
        clientCell.addElement(new Paragraph(contatoCliente, normal));
        clientCell.addElement(new Paragraph("NIF: " + nifCliente, normal));
        clientMetaTable.addCell(clientCell);

        // Right: Metadata (Invoice No, Date)
        PdfPCell metaCell = new PdfPCell();
        metaCell.setBorder(0);

        PdfPTable metaInner = new PdfPTable(2);
        metaInner.setWidthPercentage(100);
        metaInner.setWidths(new float[] { 4, 6 });

        PdfPCell lblInvoice = new PdfPCell(new Phrase("Nº FACTURA", fontSubtitle));
        lblInvoice.setBorder(0);
        PdfPCell valInvoice = new PdfPCell(new Phrase(fatura.getNumeroFatura(), normal));
        valInvoice.setBorder(0);
        metaInner.addCell(lblInvoice);
        metaInner.addCell(valInvoice);

        PdfPCell lblDate = new PdfPCell(new Phrase("DATA", fontSubtitle));
        lblDate.setBorder(0);
        PdfPCell valDate;
        if (fatura.getDataEmissao() != null) {
            valDate = new PdfPCell(
                    new Phrase(new java.text.SimpleDateFormat("dd/MM/yyyy").format(fatura.getDataEmissao()), normal));
        } else {
            valDate = new PdfPCell(new Phrase("", normal));
        }
        valDate.setBorder(0);
        metaInner.addCell(lblDate);
        metaInner.addCell(valDate);

        metaCell.addElement(metaInner);
        clientMetaTable.addCell(metaCell);

        doc.add(clientMetaTable);
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));

        // Items Table
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 3.5f, 0.8f, 1.5f, 1f, 1.2f, 2f });
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        addCanvaHeader(table, "DESCRIÇÃO", normalWhite, dustyRose);
        addCanvaHeader(table, "QTD", normalWhite, dustyRose);
        addCanvaHeader(table, "PREÇO UNIT.", normalWhite, dustyRose);
        addCanvaHeader(table, "%IVA", normalWhite, dustyRose);
        addCanvaHeader(table, "IVA", normalWhite, dustyRose);
        addCanvaHeader(table, "TOTAL", normalWhite, dustyRose);

        double totalIva = 0;
        double total = 0;

        if (fatura.getCompra() != null && fatura.getCompra().getItens() != null) {
            for (ao.co.hzconsultoria.efacturacao.model.ItemCompra item : fatura.getCompra().getItens()) {
                double iva = 14;
                double subtotal = item.getSubtotal();
                double valorIva = subtotal * (iva / 100);

                totalIva += valorIva;
                total += subtotal;

                table.addCell(cleanCell(item.getNomeProduto(), normal, Element.ALIGN_LEFT));
                table.addCell(cleanCell(String.valueOf(item.getQuantidade()), normal, Element.ALIGN_CENTER));
                table.addCell(cleanCell(df.format(item.getPreco()), normal, Element.ALIGN_RIGHT));
                table.addCell(cleanCell(df.format(iva) + "%", normal, Element.ALIGN_CENTER));
                table.addCell(cleanCell(df.format(valorIva), normal, Element.ALIGN_RIGHT));
                table.addCell(cleanCell(df.format(subtotal), normal, Element.ALIGN_RIGHT));
            }
        }

        doc.add(table);

        // Totals Section
        PdfPTable totais = new PdfPTable(2);
        totais.setWidthPercentage(40);
        totais.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totais.addCell(cleanCell("Subtotal", normal, Element.ALIGN_LEFT));
        totais.addCell(cleanCell(df.format(total), normal, Element.ALIGN_RIGHT));

        totais.addCell(cleanCell("IVA", normal, Element.ALIGN_LEFT));
        totais.addCell(cleanCell(df.format(totalIva), normal, Element.ALIGN_RIGHT));

        PdfPCell totalLbl = new PdfPCell(new Phrase("TOTAL", bold));
        totalLbl.setBorderWidth(0);
        totalLbl.setBorderWidthTop(1);
        totalLbl.setBorderColor(lightGrey);
        totalLbl.setPaddingTop(8);
        totalLbl.setPaddingBottom(8);

        PdfPCell totalVal = new PdfPCell(new Phrase(df.format(total + totalIva), bold));
        totalVal.setBorderWidth(0);
        totalVal.setBorderWidthTop(1);
        totalVal.setBorderColor(lightGrey);
        totalVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalVal.setPaddingTop(8);
        totalVal.setPaddingBottom(8);

        totais.addCell(totalLbl);
        totais.addCell(totalVal);

        doc.add(totais);
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));

        // Footer Area
        PdfPTable footer = new PdfPTable(3);
        footer.setWidthPercentage(100);
        footer.setWidths(new float[] { 4, 3, 3 });

        // Left Footer: Company Details
        PdfPCell footerCo = new PdfPCell();
        footerCo.setBorder(0);
        footerCo.addElement(new Paragraph("HZ Consultoria", bold));
        footerCo.addElement(new Paragraph("Luanda - Angola", normal));
        footerCo.addElement(new Paragraph("NIF: 5000000000", normal));
        footerCo.addElement(new Paragraph("info@hzconsultoria.co.ao", normal));
        footer.addCell(footerCo);

        // Center Footer: QR Code & Hash
        PdfPCell footerVal = new PdfPCell();
        footerVal.setBorder(0);
        String qrTexto = fatura.getNumeroFatura() + "|" + fatura.getTotal();
        try {
            Image qrImage = gerarQrCode(qrTexto);
            if (qrImage != null) {
                qrImage.scaleToFit(60, 60);
                footerVal.addElement(qrImage);
            }
        } catch (Exception ignored) {
        }
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 6, darkText);
        footerVal.addElement(new Paragraph("Hash: " + fatura.getHash(), smallFont));
        if (fatura.getCodigoAgt() != null) {
            footerVal.addElement(new Paragraph("AGT: " + fatura.getCodigoAgt(), smallFont));
        }
        footer.addCell(footerVal);

        // Right Footer: Thanks & Signature
        PdfPCell footerSig = new PdfPCell();
        footerSig.setBorder(0);
        footerSig.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph thanks = new Paragraph("Obrigado", thankYouFont);
        thanks.setAlignment(Element.ALIGN_CENTER);
        footerSig.addElement(thanks);
        footerSig.addElement(new Paragraph(" ", normal));

        PdfPCell lineCell = new PdfPCell(new Phrase("Assinatura", normal));
        lineCell.setBorderWidth(0);
        lineCell.setBorderWidthTop(1);
        lineCell.setBorderColor(darkText);
        lineCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        lineCell.setPaddingTop(4);

        PdfPTable sigTable = new PdfPTable(1);
        sigTable.setWidthPercentage(80);
        sigTable.setHorizontalAlignment(Element.ALIGN_CENTER);
        sigTable.addCell(lineCell);

        footerSig.addElement(sigTable);
        footer.addCell(footerSig);

        doc.add(footer);

        doc.close();
    }

    private void addCanvaHeader(PdfPTable table, String text, Font font, java.awt.Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorder(0);
        c.setPadding(8);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c);
    }

    private PdfPCell cleanCell(String text, Font font, int alignment) {
        PdfPCell c = new PdfPCell(new Phrase(text != null ? text : "", font));
        c.setBorderWidth(0);
        c.setBorderWidthBottom(1f);
        c.setBorderColorBottom(new java.awt.Color(241, 241, 241));
        c.setPaddingTop(8);
        c.setPaddingBottom(8);
        c.setHorizontalAlignment(alignment);
        return c;
    }

    private Image gerarQrCode(String texto) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(texto, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage bufferedImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    bufferedImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return Image.getInstance(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void gerarFatura(Carrinho carrinho) {
        // Lógica para gerar fatura a partir do carrinho
        // (implementar conforme necessidade do negócio)
    }

    public String gerarFatura(Compra compra) {
        StringBuilder fatura = new StringBuilder();
        fatura.append("Fatura\n");
        fatura.append("Data: ").append(compra.getDataCompra()).append("\n");
        fatura.append("Itens:\n");

        compra.getItens().forEach(item -> {
            fatura.append(item.getNomeProduto())
                    .append(" - Qtd: ").append(item.getQuantidade())
                    .append(" - Preço: ").append(item.getPreco())
                    .append(" - Subtotal: ").append(item.getSubtotal())
                    .append("\n");
        });

        fatura.append("Total: ").append(compra.getTotal()).append("\n");
        return fatura.toString();
    }
}