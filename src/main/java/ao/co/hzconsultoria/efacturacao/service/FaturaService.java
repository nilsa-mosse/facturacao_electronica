package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
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
    @Autowired
    private FaturaRepository faturaRepository;
    @Autowired
    private ProdutoRepository produtoRepository;

    public Fatura emitirFatura(Compra compra) {
        Fatura fatura = new Fatura();
        fatura.setCompra(compra);
        String numeroFatura = "FT-" + System.currentTimeMillis();
        fatura.setNumeroFatura(numeroFatura);
        fatura.setDataEmissao(new Date());
        fatura.setEnviadaAGT(true);
        fatura.setStatus("VALIDADA");
        // Calcular total, iva, hash, codigoAgt
        double totalSemImposto = 0;
        double valorIva = 0;
        for (ao.co.hzconsultoria.efacturacao.model.ItemCompra item : compra.getItens()) {
            // Buscar IVA do produto se possível
            double ivaPercentual = 0;
            // ... buscar ivaPercentual do produto se necessário ...
            double subtotal = item.getSubtotal();
            if (ivaPercentual > 0) {
                valorIva += subtotal * (ivaPercentual / 100);
            }
            totalSemImposto += subtotal;
        }
        double totalFinal = totalSemImposto + valorIva;
        fatura.setTotal(totalFinal);
        fatura.setIva(valorIva);
        String data = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(fatura.getDataEmissao());
        String hash = gerarHash(numeroFatura + data + totalFinal);
        fatura.setHash(hash);
        fatura.setCodigoAgt("AGT" + System.currentTimeMillis());
        Fatura faturaSalva = faturaRepository.save(fatura);
        gerarPdfFatura(faturaSalva);
        return faturaSalva;
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
            if (!dir1.exists()) dir1.mkdirs();
            String filePath1 = "src/main/resources/static/faturas/" + fatura.getNumeroFatura() + ".pdf";
            // Salva em target/classes/static/faturas (para servir via Spring Boot)
            File dir2 = new File("target/classes/static/faturas");
            if (!dir2.exists()) dir2.mkdirs();
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
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

        DecimalFormat df = new DecimalFormat("#,##0.00");

        // ================= HEADER =================
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{2, 3});

        // LOGO
        try {
            Image logo = Image.getInstance("src/main/resources/static/logo.png");
            logo.scaleToFit(80, 80);
            PdfPCell logoCell = new PdfPCell(logo);
            logoCell.setBorder(0);
            header.addCell(logoCell);
        } catch (Exception e) {
            header.addCell(new PdfPCell(new Phrase("LOGO")));
        }

        // INFO EMPRESA
        PdfPCell empresa = new PdfPCell();
        empresa.setBorder(0);
        empresa.addElement(new Paragraph("HZ Consultoria", titulo));
        empresa.addElement(new Paragraph("NIF: 5000000000", normal));
        empresa.addElement(new Paragraph("Luanda - Angola", normal));
        header.addCell(empresa);

        doc.add(header);

        doc.add(new Paragraph(" "));

        // ================= INFO FATURA =================
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);

        info.addCell(cell("Fatura Nº: " + fatura.getNumeroFatura(), bold));
        info.addCell(cell("Data: " + fatura.getDataEmissao(), normal));

        info.addCell(cell("Status: " + fatura.getStatus(), normal));
        info.addCell(cell("Código AGT: " + fatura.getCodigoAgt(), normal));

        doc.add(info);

        doc.add(new Paragraph(" "));

        // ================= TABELA =================
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 2, 1, 2, 2});

        addHeader(table, "Produto");
        addHeader(table, "Qtd");
        addHeader(table, "Preço");
        addHeader(table, "%IVA");
        addHeader(table, "IVA");
        addHeader(table, "Subtotal");

        double totalIva = 0;
        double total = 0;

        for (var item : fatura.getCompra().getItens()) {

            double iva = 14; // exemplo
            double subtotal = item.getSubtotal();
            double valorIva = subtotal * (iva / 100);

            totalIva += valorIva;
            total += subtotal;

            table.addCell(cell(item.getNomeProduto(), normal));
            table.addCell(cellCenter(String.valueOf(item.getQuantidade()), normal));
            table.addCell(cellRight(df.format(item.getPreco()), normal));
            table.addCell(cellCenter(df.format(iva), normal));
            table.addCell(cellRight(df.format(valorIva), normal));
            table.addCell(cellRight(df.format(subtotal), normal));
        }

        doc.add(table);

        doc.add(new Paragraph(" "));

        // ================= TOTAIS =================
        PdfPTable totais = new PdfPTable(2);
        totais.setWidthPercentage(40);
        totais.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totais.addCell(cell("Total s/ IVA", normal));
        totais.addCell(cellRight(df.format(total), bold));

        totais.addCell(cell("IVA", normal));
        totais.addCell(cellRight(df.format(totalIva), bold));

        totais.addCell(cell("TOTAL", bold));
        totais.addCell(cellRight(df.format(total + totalIva), bold));

        doc.add(totais);

        doc.add(new Paragraph(" "));

        // ================= QR CODE =================
        String qrTexto = fatura.getNumeroFatura() + "|" + fatura.getTotal();

        Image qrImage = gerarQrCode(qrTexto);
        qrImage.scaleToFit(100, 100);

        doc.add(new Paragraph("Validação:", bold));
        doc.add(qrImage);

        doc.add(new Paragraph("Hash: " + fatura.getHash(), normal));

        doc.close();
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