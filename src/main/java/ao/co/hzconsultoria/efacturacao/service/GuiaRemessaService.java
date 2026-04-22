package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.EventoTracking;
import ao.co.hzconsultoria.efacturacao.model.GuiaRemessa;
import ao.co.hzconsultoria.efacturacao.model.ItemGuiaRemessa;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.GuiaRemessaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;

import com.lowagie.text.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Service
public class GuiaRemessaService {

    @Autowired
    private GuiaRemessaRepository guiaRemessaRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private ProdutoRepository produtoRepository;

    private static final Logger log = LoggerFactory.getLogger(GuiaRemessaService.class);

    @Transactional
    public GuiaRemessa gerarGuiaAPartirDeFatura(Long facturaId) {
        Compra fatura = compraRepository.findById(facturaId).orElseThrow(() -> new RuntimeException("Fatura não encontrada"));
        
        GuiaRemessa guia = new GuiaRemessa();
        guia.setCliente(fatura.getCliente());
        guia.setDataEmissao(LocalDateTime.now());
        guia.setFaturaOrigem(fatura);
        guia.setNumeroGuia("GR-" + System.currentTimeMillis());
        guia.setStatus("ATIVA");
        
        List<ItemGuiaRemessa> itensGuia = fatura.getItens().stream().map(item -> {
            ItemGuiaRemessa ig = new ItemGuiaRemessa();
            ig.setNomeProduto(item.getNomeProduto());
            ig.setQuantidade(Double.valueOf(item.getQuantidade()));
            ig.setUnidadeMedida("UN");
            ig.setGuiaRemessa(guia);
            return ig;
        }).collect(Collectors.toList());
        
        guia.setItens(itensGuia);
        return guiaRemessaRepository.save(guia);
    }

    public List<GuiaRemessa> listarTodas() {
        return guiaRemessaRepository.findAll();
    }

    public GuiaRemessa buscarPorId(Long id) {
        return guiaRemessaRepository.findById(id).orElse(null);
    }

    @Transactional
    public void salvar(GuiaRemessa guia) {
        if (guia.getDataEmissao() == null) guia.setDataEmissao(LocalDateTime.now());
        if (guia.getNumeroGuia() == null) guia.setNumeroGuia("GR-" + System.currentTimeMillis());
        
        // Garantir vínculo bidireccional dos itens
        if (guia.getItens() != null) {
            guia.getItens().forEach(i -> i.setGuiaRemessa(guia));
        } else {
            guia.setItens(new ArrayList<>());
        }

        // Limpar referência se o ID for nulo ou inválido (evita TransientPropertyValueException)
        if (guia.getGuiaReferencia() != null && (guia.getGuiaReferencia().getId() == null || guia.getGuiaReferencia().getId() <= 0)) {
            guia.setGuiaReferencia(null);
        }

        // Se for uma retificação, marcar a guia anterior como SUBSTITUIDA
        if (guia.getGuiaReferencia() != null && guia.getGuiaReferencia().getId() != null && guia.getGuiaReferencia().getId() > 0) {
            try {
                GuiaRemessa original = buscarPorId(guia.getGuiaReferencia().getId());
                if (original != null) {
                    original.setStatus("SUBSTITUIDA");
                    guiaRemessaRepository.save(original);
                }
            } catch (Exception e) {
                log.warn("Falha ao marcar guia original como substituída: {}", e.getMessage());
            }
        }
        
        // Inicializar tracking se for nova
        if (guia.getId() == null && (guia.getEventosTracking() == null || guia.getEventosTracking().isEmpty())) {
            EventoTracking ev = new EventoTracking();
            ev.setStatus("EM_PROCESSAMENTO");
            ev.setDataHora(LocalDateTime.now());
            ev.setLocalizacao(guia.getLocalCarga());
            ev.setObservacao("Guia emitida e aguardando processamento.");
            ev.setGuiaRemessa(guia);
            
            List<EventoTracking> eventos = new ArrayList<>();
            eventos.add(ev);
            guia.setEventosTracking(eventos);
            guia.setTrackingStatus("EM_PROCESSAMENTO");
        }
        
        // Comunicar à AGT
        comunicarAGT(guia);
        
        guiaRemessaRepository.save(guia);
        gerarPdfGuia(guia);
    }

    private void comunicarAGT(GuiaRemessa guia) {
        // Gerar Hash de Assinatura
        String input = (guia.getNumeroGuia() != null ? guia.getNumeroGuia() : "") + 
                       (guia.getDataEmissao() != null ? guia.getDataEmissao().toString() : "") + 
                       (guia.getCliente() != null ? guia.getCliente().getNif() : "");
        
        String hash = gerarHash(input);
        guia.setHashAgt(hash);
        
        // Simular Código de Validação da AGT
        String codigoValidacao = "AGT-GR-" + System.currentTimeMillis();
        guia.setCodigoValidacao(codigoValidacao);
        guia.setDataValidacaoAgt(LocalDateTime.now());
        
        log.info("Guia {} comunicada à AGT. Código: {}", guia.getNumeroGuia(), codigoValidacao);
    }

    private String gerarHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Erro ao gerar hash AGT", e);
            return "HASH_ERROR_" + System.currentTimeMillis();
        }
    }

    private void gerarPdfGuia(GuiaRemessa guia) {
        try {
            File dir1 = new File("src/main/resources/static/guias");
            if (!dir1.exists()) dir1.mkdirs();
            String filePath1 = "src/main/resources/static/guias/" + guia.getNumeroGuia() + ".pdf";

            File dir2 = new File("target/classes/static/guias");
            if (!dir2.exists()) dir2.mkdirs();
            String filePath2 = "target/classes/static/guias/" + guia.getNumeroGuia() + ".pdf";

            gerarPdf(filePath1, guia);
            gerarPdf(filePath2, guia);
        } catch (Exception e) {
            log.error("Erro crítico ao gerar PDF da Guia {}: ", guia.getNumeroGuia(), e);
        }
    }

    private void gerarPdf(String filePath, GuiaRemessa guia) throws Exception {
        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa configEmpresa = empresaRepository.findById(empresaId).orElse(new Empresa());

        java.awt.Color blackColor = new java.awt.Color(33, 37, 41);
        Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, blackColor);
        Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, blackColor);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10, new java.awt.Color(73, 80, 87));
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 7);
        Font smallBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, blackColor);

        // Header
        PdfPTable mainHeader = new PdfPTable(2);
        mainHeader.setWidthPercentage(100);
        mainHeader.setWidths(new float[] { 6, 4 });

        PdfPCell titleCell = new PdfPCell(new Phrase("GUIA DE REMESSA", fontTitle));
        titleCell.setBorder(0);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        mainHeader.addCell(titleCell);

        PdfPCell logoCell = new PdfPCell();
        try {
            if (configEmpresa.getLogotipo() != null) {
                Image logo = Image.getInstance(configEmpresa.getLogotipo());
                logo.scaleToFit(150, 150);
                logoCell = new PdfPCell(logo);
            } else {
                logoCell = new PdfPCell(new Phrase(configEmpresa.getNome(), fontSubtitle));
            }
        } catch (Exception e) {
            logoCell = new PdfPCell(new Phrase(configEmpresa.getNome(), fontSubtitle));
        }
        logoCell.setBorder(0);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        mainHeader.addCell(logoCell);
        doc.add(mainHeader);
        doc.add(new Paragraph(" "));

        // Info Sections
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);

        // From (Company)
        PdfPCell cellDe = new PdfPCell();
        cellDe.setBorder(0);
        cellDe.addElement(new Paragraph(configEmpresa.getNome(), bold));
        cellDe.addElement(new Paragraph(configEmpresa.getEndereco(), normal));
        cellDe.addElement(new Paragraph("NIF: " + configEmpresa.getNif(), normal));
        infoTable.addCell(cellDe);

        // Meta (Doc info)
        PdfPCell cellMeta = new PdfPCell();
        cellMeta.setBorder(0);
        cellMeta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        String numeroDoc = guia.getNumeroGuia() != null ? guia.getNumeroGuia() : "-";
        String dataDoc = guia.getDataEmissao() != null ? guia.getDataEmissao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-";
        cellMeta.addElement(new Paragraph("Nº DOCUMENTO: " + numeroDoc, bold));
        cellMeta.addElement(new Paragraph("DATA: " + dataDoc, normal));
        
        if (guia.getCodigoValidacao() != null) {
            Paragraph agtCode = new Paragraph("CÓD. AGT: " + guia.getCodigoValidacao(), smallBold);
            agtCode.setSpacingBefore(5);
            cellMeta.addElement(agtCode);
            
            String miniHash = guia.getHashAgt() != null && guia.getHashAgt().length() > 10 ? 
                              guia.getHashAgt().substring(0, 4) + "..." + guia.getHashAgt().substring(guia.getHashAgt().length() - 4) : "-";
            cellMeta.addElement(new Paragraph("HASH: " + miniHash, small));
        }
        
        infoTable.addCell(cellMeta);
        doc.add(infoTable);
        doc.add(new Paragraph(" "));

        // Transportation Details
        PdfPTable transportTable = new PdfPTable(1);
        transportTable.setWidthPercentage(100);
        PdfPCell transHeader = new PdfPCell(new Phrase("DETALHES DE TRANSPORTE", fontSubtitle));
        transHeader.setBackgroundColor(new java.awt.Color(248, 249, 250));
        transHeader.setPadding(8);
        transportTable.addCell(transHeader);
        
        PdfPCell transBody = new PdfPCell();
        transBody.setPadding(10);
        String origem = guia.getLocalCarga() != null ? guia.getLocalCarga() : "-";
        String destino = guia.getLocalDescarga() != null ? guia.getLocalDescarga() : "-";
        String viatura = guia.getMatriculaViatura() != null ? guia.getMatriculaViatura() : "-";
        String motorista = guia.getMotorista() != null ? guia.getMotorista() : "-";
        
        transBody.addElement(new Paragraph("ORIGEM: " + origem, normal));
        transBody.addElement(new Paragraph("DESTINO: " + destino, normal));
        transBody.addElement(new Paragraph("VIATURA: " + viatura + " | MOTORISTA: " + motorista, normal));
        transportTable.addCell(transBody);
        doc.add(transportTable);
        doc.add(new Paragraph(" "));

        // Items Table
        PdfPTable itemsTable = new PdfPTable(new float[] { 6, 2, 2 });
        itemsTable.setWidthPercentage(100);
        
        String[] headers = { "ARTIGO / DESIGNAÇÃO", "QTD", "UNIDADE" };
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, bold));
            c.setBackgroundColor(new java.awt.Color(241, 243, 245));
            c.setPadding(8);
            itemsTable.addCell(c);
        }

        for (ItemGuiaRemessa item : guia.getItens()) {
            itemsTable.addCell(new PdfPCell(new Phrase(item.getNomeProduto(), normal)));
            itemsTable.addCell(new PdfPCell(new Phrase(String.valueOf(item.getQuantidade()), normal)));
            itemsTable.addCell(new PdfPCell(new Phrase(item.getUnidadeMedida(), normal)));
        }
        doc.add(itemsTable);

        doc.close();
    }

    @Transactional
    public Compra converterParaFactura(Long guiaId) {
        GuiaRemessa guia = buscarPorId(guiaId);
        if (!"ATIVA".equals(guia.getStatus())) throw new RuntimeException("Apenas guias ATIVAS podem ser convertidas.");
        if (guia.getFaturaOrigem() != null) throw new RuntimeException("Esta guia já possui uma factura vinculada.");

        Compra compra = new Compra();
        compra.setCliente(guia.getCliente());
        compra.setDataCompra(LocalDateTime.now());
        compra.setStatus("EMITIDA");
        compra.setTipoDocumento("FT");

        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();

        List<ao.co.hzconsultoria.efacturacao.model.ItemCompra> itens = guia.getItens().stream().map(ig -> {
            ao.co.hzconsultoria.efacturacao.model.ItemCompra ic = new ao.co.hzconsultoria.efacturacao.model.ItemCompra();
            ic.setNomeProduto(ig.getNomeProduto());
            ic.setQuantidade(ig.getQuantidade().intValue());
            
            // Tentar buscar preço real do produto
            double preco = 0.0;
            java.util.List<Produto> prods = produtoRepository.findByNomeStartingWithIgnoreCaseAndEmpresa_Id(ig.getNomeProduto(), empresaId);
            if (prods != null && !prods.isEmpty()) {
                preco = prods.get(0).getPreco();
            }
            
            ic.setPreco(preco);
            ic.setSubtotal(preco * ic.getQuantidade());
            ic.setCompra(compra);
            return ic;
        }).collect(Collectors.toList());

        compra.setItens(itens);
        Compra faturaFinal = vendaService.finalizarVenda(compra);
        
        guia.setStatus("FECHADA");
        guia.setFaturaOrigem(faturaFinal);
        guiaRemessaRepository.save(guia);
        
        return faturaFinal;
    }

    @Transactional
    public void anularGuia(Long id, String motivo) {
        GuiaRemessa guia = buscarPorId(id);
        guia.setStatus("ANULADA");
        guia.setMotivoAnulacao(motivo);
        guiaRemessaRepository.save(guia);
    }

    @Transactional
    public void eliminarTodas() {
        guiaRemessaRepository.deleteAll();
    }

    @Transactional
    public void adicionarEventoTracking(Long guiaId, String status, String local, String obs) {
        GuiaRemessa guia = buscarPorId(guiaId);
        if (guia == null) return;

        EventoTracking ev = new EventoTracking();
        ev.setStatus(status);
        ev.setDataHora(LocalDateTime.now());
        ev.setLocalizacao(local);
        ev.setObservacao(obs);
        ev.setGuiaRemessa(guia);

        if (guia.getEventosTracking() == null) guia.setEventosTracking(new ArrayList<>());
        guia.getEventosTracking().add(ev);
        guia.setTrackingStatus(status);
        
        guiaRemessaRepository.save(guia);
    }
}
