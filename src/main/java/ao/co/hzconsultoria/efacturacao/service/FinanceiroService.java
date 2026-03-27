package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.dto.MovimentacaoDTO;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.DespesaRepository;
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

@Service
public class FinanceiroService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private DespesaRepository despesaRepository;

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
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, java.awt.Color.BLACK);
        Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, java.awt.Color.GRAY);
        Font fontBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.BLACK);
        Font fontNormal = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.BLACK);
        Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Header
        Paragraph pTitle = new Paragraph("RELATÓRIO DE FLUXO DE CAIXA", fontTitle);
        pTitle.setAlignment(Element.ALIGN_CENTER);
        doc.add(pTitle);

        Paragraph pPeriodo = new Paragraph("Período: " + inicio.format(dtf) + " até " + fim.format(dtf), fontSubtitle);
        pPeriodo.setAlignment(Element.ALIGN_CENTER);
        pPeriodo.setSpacingAfter(20f);
        doc.add(pPeriodo);

        // Resumo Table
        PdfPTable resumoTable = new PdfPTable(3);
        resumoTable.setWidthPercentage(100);
        
        double totalIn = getTotalEntradas(movs);
        double totalOut = getTotalSaidas(movs);
        
        resumoTable.addCell(createSummaryCell("ENTRADAS", "Kz " + String.format("%.2f", totalIn), new java.awt.Color(40, 167, 69)));
        resumoTable.addCell(createSummaryCell("SAÍDAS", "Kz " + String.format("%.2f", totalOut), new java.awt.Color(220, 53, 69)));
        resumoTable.addCell(createSummaryCell("SALDO", "Kz " + String.format("%.2f", totalIn - totalOut), new java.awt.Color(23, 162, 184)));
        resumoTable.setSpacingAfter(20f);
        doc.add(resumoTable);

        // Movement Table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[] { 2f, 3f, 2f, 1.5f, 2f });

        String[] headers = { "DATA", "DESCRIÇÃO", "CATEGORIA", "TIPO", "VALOR" };
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
            c.setBackgroundColor(new java.awt.Color(52, 58, 64));
            c.setPadding(8);
            table.addCell(c);
        }

        for (MovimentacaoDTO m : movs) {
            table.addCell(new PdfPCell(new Phrase(m.getData().format(DateTimeFormatter.ofPattern("dd/MM/yy HH:mm")), fontNormal)));
            table.addCell(new PdfPCell(new Phrase(m.getDescricao(), fontNormal)));
            table.addCell(new PdfPCell(new Phrase(m.getCategoria(), fontNormal)));
            table.addCell(new PdfPCell(new Phrase(m.getTipo(), fontNormal)));
            
            PdfPCell vCell = new PdfPCell(new Phrase((m.getTipo().equals("SAIDA") ? "- " : "+ ") + String.format("%.2f", m.getValor()), fontBold));
            vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            if (m.getTipo().equals("SAIDA")) vCell.getPhrase().getFont().setColor(java.awt.Color.RED);
            else vCell.getPhrase().getFont().setColor(new java.awt.Color(40, 167, 69));
            table.addCell(vCell);
        }

        doc.add(table);
        doc.add(new Paragraph("\n\nGerado em: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")), fontNormal));
        doc.close();
    }

    private PdfPCell createSummaryCell(String title, String val, java.awt.Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(10);
        cell.setBorderWidth(1);
        cell.setBorderColor(new java.awt.Color(230, 230, 230));
        cell.addElement(new Phrase(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.GRAY)));
        cell.addElement(new Phrase(val, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color)));
        return cell;
    }
}
