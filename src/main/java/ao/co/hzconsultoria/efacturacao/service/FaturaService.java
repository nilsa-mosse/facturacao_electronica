package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.dto.AgtResponse;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Cliente;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoAGT;
import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.model.ItemDevolucao;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoAGTRepository;
import ao.co.hzconsultoria.efacturacao.repository.DevolucaoRepository;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Calendar;

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

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ConfiguracaoEmpresaService configuracaoEmpresaService;

    @Autowired
    private ConfiguracaoEmpresaRepository configuracaoEmpresaRepository;

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    @Autowired
    private ConfiguracaoAGTRepository configuracaoAGTRepository;

    @Autowired
    private DevolucaoRepository devolucaoRepository;

    @Value("${app.upload.logo.dir:./uploads/logo/}")
    private String logoUploadDir;

    public Fatura emitirFatura(Compra compra) {
        return emitirDocumento(compra, "FT");
    }

    public Fatura emitirNotaCredito(Devolucao devolucao) {
        Fatura fatura = new Fatura();
        fatura.setEmpresa(devolucao.getEmpresa());
        fatura.setTipoDocumento("NC");
        
        // Numeração Sequencial NC
        Calendar cal = Calendar.getInstance();
        int ano = cal.get(Calendar.YEAR);
        long count = faturaRepository.countByTypeAndYear("NC", ano, fatura.getEmpresa().getId()) + 1;
        String numeroNC = "NC " + ano + "/" + count;
        
        fatura.setNumeroFatura(numeroNC);
        fatura.setDataEmissao(new Date());
        fatura.setSystemEntryDate(new Date());
        fatura.setInvoiceStatus("N");
        
        // Totais da Devolução
        fatura.setTotal(devolucao.getTotal() + (devolucao.getIva() != null ? devolucao.getIva() : 0.0));
        fatura.setIva(devolucao.getIva() != null ? devolucao.getIva() : 0.0);
        
        // Link com a transação original via Compra (se existir)
        if (devolucao.getFatura() != null) {
            fatura.setCompra(devolucao.getFatura().getCompra());
        }

        // Assinatura RSA
        ConfiguracaoSistemaEntity configSistema = configuracaoSistemaRepository.findById(1L).orElse(new ConfiguracaoSistemaEntity());
        Fatura ultimaNC = faturaRepository.findLastByType("NC", fatura.getEmpresa().getId());
        fatura.setPreviousHash((ultimaNC != null) ? ultimaNC.getHash() : "");
        fatura.setHashControl(String.valueOf(configSistema.getAgtChaveVersao()));

        String dadosAssinar = montarStringAssinatura(fatura);
        String assinatura = assinarRSA(dadosAssinar, configSistema.getAgtPrivateKey());
        fatura.setHash(assinatura);

        fatura.setEnviadaAGT(false);
        fatura.setStatus("PENDENTE");
        
        Fatura faturaSalva = faturaRepository.save(fatura);
        faturaSalva = processarEnvioAGT(faturaSalva);
        
        // O PDF será gerado pelo DevolucaoService após salvar o vínculo bidirecional
        return faturaSalva;
    }

    public Fatura emitirProforma(Compra compra) {
        return emitirDocumento(compra, "FP");
    }

    public Fatura emitirDocumento(Compra compra, String tipo) {
        Fatura fatura = new Fatura();
        fatura.setCompra(compra);
        if (compra != null && compra.getEmpresa() != null) {
            fatura.setEmpresa(compra.getEmpresa());
        }
        fatura.setTipoDocumento(tipo);
        
        // Numeração Sequencial (AGT): CODE YEAR/COUNT
        Calendar cal = Calendar.getInstance();
        int ano = cal.get(Calendar.YEAR);
        long count = faturaRepository.countByTypeAndYear(tipo, ano, fatura.getEmpresa().getId()) + 1;
        String numeroFatura = tipo + " " + ano + "/" + count;
        
        fatura.setNumeroFatura(numeroFatura);
        fatura.setDataEmissao(new Date());
        fatura.setSystemEntryDate(new Date());
        fatura.setInvoiceStatus("N");
        // Calcular totais e IVA
        double totalSemImposto = 0;
        double valorIva = 0;
        for (ao.co.hzconsultoria.efacturacao.model.ItemCompra item : compra.getItens()) {
            double subtotal = item.getSubtotal() != null ? item.getSubtotal() : 0.0;
            totalSemImposto += subtotal;
            if (item.getIva() != null) {
                valorIva += item.getIva();
            } else if (item.getIvaPercentual() != null) {
                valorIva += subtotal * (item.getIvaPercentual() / 100);
            } else {
                valorIva += subtotal * 0.14; // Fallback
            }
        }

        double comissao = compra.getComissaoMulticaixa() != null ? compra.getComissaoMulticaixa() : 0.0;
        double totalFinal = (totalSemImposto + valorIva) - comissao;

        fatura.setTotal(totalFinal);
        fatura.setIva(valorIva);

        // --- Lógica de Assinatura AGT (RSA) ---
        ConfiguracaoSistemaEntity configSistema = configuracaoSistemaRepository.findById(1L).orElse(new ConfiguracaoSistemaEntity());
        Fatura ultimaFatura = faturaRepository.findLastByType(tipo, fatura.getEmpresa().getId());
        String hashAnterior = (ultimaFatura != null) ? ultimaFatura.getHash() : "";
        fatura.setPreviousHash(hashAnterior);
        fatura.setHashControl(String.valueOf(configSistema.getAgtChaveVersao()));

        String dadosAssinar = montarStringAssinatura(fatura);
        String assinatura = assinarRSA(dadosAssinar, configSistema.getAgtPrivateKey());
        fatura.setHash(assinatura);

        // Guardar fatura antes de enviar (para ter ID gerado)
        fatura.setEnviadaAGT(false);
        fatura.setStatus("PENDENTE");
        Fatura faturaSalva = faturaRepository.save(fatura);

        if (!"FP".equals(tipo)) {
            faturaSalva = processarEnvioAGT(faturaSalva);
        } else {
            faturaSalva.setStatus("EMITIDA");
            log.info("Pró-forma {} emitida com sucesso.", numeroFatura);
        }

        // Actualizar estado final na BD
        faturaSalva = faturaRepository.save(faturaSalva);
        gerarPdfFatura(faturaSalva);
        return faturaSalva;
    }

    private Fatura processarEnvioAGT(Fatura fatura) {
        boolean podeEnviar = true;
        String motivoNaoEnvio = "";
        java.util.List<ConfiguracaoAGT> configsAgt = configuracaoAGTRepository.findAll();
        ConfiguracaoAGT configAgt = null;
        
        if (!configsAgt.isEmpty()) {
            configAgt = configsAgt.get(0);
            if (!configAgt.isEnvioAgtAtivo()) {
                podeEnviar = false;
                motivoNaoEnvio = "Envio automático para AGT desactivado globalmente.";
            } else if (configAgt.getLimiteDocumentosDiarios() != null && configAgt.getLimiteDocumentosDiarios() > 0) {
                java.time.LocalDate hoje = java.time.LocalDate.now();
                if (configAgt.getDataUltimoEnvio() == null || !configAgt.getDataUltimoEnvio().equals(hoje)) {
                    configAgt.setDataUltimoEnvio(hoje);
                    configAgt.setDocumentosEnviadosHoje(0);
                }
                if (configAgt.getDocumentosEnviadosHoje() >= configAgt.getLimiteDocumentosDiarios()) {
                    podeEnviar = false;
                    motivoNaoEnvio = "Limite diário de envio para AGT atingido (" + configAgt.getLimiteDocumentosDiarios() + " docs).";
                }
            }
        }

        if (podeEnviar) {
            try {
                AgtResponse agtResponse = agtService.enviarFatura(fatura);
                if (agtResponse.isSucesso()) {
                    fatura.setEnviadaAGT(true);
                    fatura.setStatus(agtResponse.getStatus() != null ? agtResponse.getStatus() : "VALIDADA");
                    fatura.setCodigoAgt(agtResponse.getCodigoAgt());
                    log.info("{} {} enviada e validada pela AGT. Código: {}", 
                            fatura.getTipoDocumento(), fatura.getNumeroFatura(), agtResponse.getCodigoAgt());
                    
                    if (configAgt != null && configAgt.getLimiteDocumentosDiarios() != null && configAgt.getLimiteDocumentosDiarios() > 0) {
                        configAgt.setDocumentosEnviadosHoje(configAgt.getDocumentosEnviadosHoje() + 1);
                        configuracaoAGTRepository.save(configAgt);
                    }
                } else {
                    fatura.setEnviadaAGT(false);
                    fatura.setStatus("FALHA_ENVIO");
                    fatura.setCodigoAgt("ERRO: " + agtResponse.getMensagem());
                    log.warn("Falha no envio da {} {} para AGT: {}", 
                            fatura.getTipoDocumento(), fatura.getNumeroFatura(), agtResponse.getMensagem());
                }
            } catch (Exception e) {
                fatura.setStatus("FALHA_ENVIO");
                log.error("Erro crítico ao enviar {} {} para AGT: {}", 
                        fatura.getTipoDocumento(), fatura.getNumeroFatura(), e.getMessage());
            }
        } else {
            fatura.setEnviadaAGT(false);
            fatura.setStatus("EMITIDA_OFFLINE");
            log.info("O documento {} não foi enviado para a AGT. Motivo: {}", 
                    fatura.getNumeroFatura(), motivoNaoEnvio);
        }
        return fatura;
    }

    public Fatura reenviarFatura(Long id) {
        Fatura fatura = faturaRepository.findById(id).orElseThrow(() -> new RuntimeException("Fatura não encontrada"));
        Fatura processada = processarEnvioAGT(fatura);
        processada = faturaRepository.save(processada);
        if (processada.isEnviadaAGT()) {
            gerarPdfFatura(processada);
        }
        return processada;
    }

    private String montarStringAssinatura(Fatura f) {
        // Formato: InvoiceDate;SystemEntryDate;InvoiceNo;GrossTotal;HashControl
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        DecimalFormat df = new DecimalFormat("0.00");
        
        return sdfDate.format(f.getDataEmissao()) + ";" +
               sdfTime.format(f.getSystemEntryDate()) + ";" +
               f.getNumeroFatura() + ";" +
               df.format(f.getTotal()).replace(",", ".") + ";" +
               (f.getPreviousHash() != null ? f.getPreviousHash() : "");
    }

    private String assinarRSA(String dados, String privateKeyPem) {
        if (privateKeyPem == null || privateKeyPem.isEmpty()) {
            log.warn("Chave privada RSA não configurada. Usando hash SHA-256 como fallback temporário.");
            return gerarHash(dados);
        }
        try {
            // Remover cabeçalhos PEM
            String privateKeyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            
            byte[] pkcs8EncodedKey = Base64.getDecoder().decode(privateKeyContent);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8EncodedKey));
            
            Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(dados.getBytes());
            byte[] signed = signature.sign();
            
            return Base64.getEncoder().encodeToString(signed);
        } catch (Exception e) {
            log.error("Erro ao assinar com RSA: {}", e.getMessage());
            return gerarHash(dados);
        }
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

    public void gerarPdfFatura(Fatura fatura) {
        try {
            // Salva em ./uploads/faturas (pasta externa, acessível via /uploads/faturas/**)
            File dir = new File("./uploads/faturas");
            if (!dir.exists())
                dir.mkdirs();

            String filePath = "./uploads/faturas/" + fatura.getNumeroFatura() + ".pdf";

            log.info("Iniciando geração de PDF para fatura {} -> {}", fatura.getNumeroFatura(), filePath);
            // Gera o PDF no local externo (usando escrita atômica)
            gerarPdf(filePath, fatura);
            log.info("PDF da fatura gerado em: {}", filePath);
        } catch (Exception e) {
            log.error("Erro ao gerar PDF da fatura: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private void gerarPdf(String filePath, Fatura fatura) throws Exception {
        // Use a temp file and then move it into place to avoid partially-written files being served
        Path finalPath = Paths.get(filePath).toAbsolutePath().normalize();
        Path tempPath = Paths.get(filePath + ".tmp").toAbsolutePath().normalize();

        // Ensure parent exists
        if (finalPath.getParent() != null) Files.createDirectories(finalPath.getParent());

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        // Use try-with-resources for FileOutputStream to ensure it's closed
        try (FileOutputStream fos = new FileOutputStream(tempPath.toFile())) {
            com.lowagie.text.pdf.PdfWriter writer = com.lowagie.text.pdf.PdfWriter.getInstance(doc, fos);
            try {
                doc.open();

                // Dados da Empresa (Dinâmico)
                Empresa configEmpresa = fatura.getEmpresa();
                if (configEmpresa == null && fatura.getCompra() != null) {
                    configEmpresa = fatura.getCompra().getEmpresa();
                }

                if (configEmpresa == null) {
                    java.util.List<Empresa> empresas = empresaRepository.findAll();
                    configEmpresa = empresas.isEmpty() ? new Empresa() : empresas.get(0);
                }

                // Obter configurações específicas da empresa
                ConfiguracaoEmpresa configuracao = null;
                if (configEmpresa.getId() != null) {
                    configuracao = configuracaoEmpresaService.obterConfiguracao(configEmpresa.getId());
                }

                if (configEmpresa.getId() == null || configEmpresa.getNome() == null || configEmpresa.getNome().isEmpty()) {
                    configEmpresa.setNome("MINHA EMPRESA (Configurar nas definições)");
                    configEmpresa.setNif("999999999");
                    configEmpresa.setEndereco("Endereço não configurado");
                    configEmpresa.setEmail("configurar@empresa.com");
                }

                // --- PALETA DE CORES PREMIUM ---
                java.awt.Color primaryColor = new java.awt.Color(0, 86, 179); // Corporate Blue
                java.awt.Color textColor = new java.awt.Color(44, 62, 80);    // Anthracite Gray
                java.awt.Color lightGrayBg = new java.awt.Color(248, 249, 250);
                java.awt.Color zebraColor = new java.awt.Color(251, 251, 252);
                java.awt.Color borderColor = new java.awt.Color(233, 236, 239);

                // Fonts
                Font fontFactura = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, primaryColor);
                Font fontSubtitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, textColor);
                Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, textColor);
                Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, textColor);
                Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);
                Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new java.awt.Color(108, 117, 125));

                DecimalFormat df = new DecimalFormat("#,##0.00");

                // Barra de topo (Design Element)
                PdfPTable topBar = new PdfPTable(1);
                topBar.setWidthPercentage(100);
                PdfPCell barCell = new PdfPCell();
                barCell.setFixedHeight(4f);
                barCell.setBackgroundColor(primaryColor);
                barCell.setBorder(0);
                topBar.addCell(barCell);
                doc.add(topBar);
                doc.add(new Paragraph(" "));

                // Header Section
                PdfPTable mainHeader = new PdfPTable(2);
                mainHeader.setWidthPercentage(100);
                mainHeader.setWidths(new float[] { 7, 3 });

                String tituloDocumento = "FACTURA";
                if ("FP".equals(fatura.getTipoDocumento())) {
                    tituloDocumento = "FACTURA PRÓ-FORMA";
                } else if ("FR".equals(fatura.getTipoDocumento())) {
                    tituloDocumento = "FACTURA RECIBO";
                } else if ("NC".equals(fatura.getTipoDocumento())) {
                    tituloDocumento = "NOTA DE CRÉDITO";
                }

                PdfPCell titleCell = new PdfPCell();
                titleCell.setBorder(0);
                titleCell.addElement(new Phrase(tituloDocumento, fontFactura));
                Paragraph pNum = new Paragraph(fatura.getNumeroFatura(), fontSubtitle);
                titleCell.addElement(pNum);
                mainHeader.addCell(titleCell);

                PdfPCell logoCell = new PdfPCell();
                try {
                    String logoPath = configEmpresa.getLogotipo();
                    if (logoPath != null && !logoPath.isEmpty()) {
                        String caminhoAbsoluto = resolverCaminhoImagem(logoPath);
                        if (caminhoAbsoluto != null && !caminhoAbsoluto.isEmpty()) {
                            Image logo = Image.getInstance(caminhoAbsoluto);
                            logo.scaleToFit(120, 60);
                            logoCell = new PdfPCell(logo);
                        } else {
                            logoCell = new PdfPCell(new Phrase(configEmpresa.getNome(), fontSubtitle));
                        }
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
                
                // Linha Separadora
                PdfPTable lineTable = new PdfPTable(1);
                lineTable.setWidthPercentage(100);
                PdfPCell lineCell = new PdfPCell();
                lineCell.setBorder(0);
                lineCell.setBorderWidthBottom(1f);
                lineCell.setBorderColorBottom(borderColor);
                lineTable.addCell(lineCell);
                doc.add(lineTable);
                
                doc.add(new Paragraph(" "));

                // Client & Meta Section
                PdfPTable infoTable = new PdfPTable(2);
                infoTable.setWidthPercentage(100);
                infoTable.setWidths(new float[] { 5, 5 });

                // Left: My Company Info
                PdfPCell myInfoCell = new PdfPCell();
                myInfoCell.setBorder(0);
                myInfoCell.setPaddingRight(20);
                myInfoCell.addElement(new Paragraph("DE:", smallFont));
                myInfoCell.addElement(new Paragraph(configEmpresa.getNome(), bold));
                myInfoCell.addElement(new Paragraph(configEmpresa.getEndereco() != null ? configEmpresa.getEndereco() : "Angola", normal));
                myInfoCell.addElement(new Paragraph("NIF: " + (configEmpresa.getNif() != null ? configEmpresa.getNif() : "999999999"), normal));
                if (configEmpresa.getTelefone() != null && !configEmpresa.getTelefone().isEmpty()) {
                    myInfoCell.addElement(new Paragraph("Tel: " + configEmpresa.getTelefone(), normal));
                }
                infoTable.addCell(myInfoCell);

                // Right: Bill To
                Compra c = fatura.getCompra();
                String nomeCli = c != null && c.getNomeCliente() != null ? c.getNomeCliente() : "Consumidor Final";
                String nifCli = c != null && c.getNifCliente() != null ? c.getNifCliente() : "999999999";
                String endCli = c != null && c.getMoradaCliente() != null ? c.getMoradaCliente() : "";

                PdfPCell billToCell = new PdfPCell();
                billToCell.setBorder(0);
                billToCell.addElement(new Paragraph("FACTURADO A:", smallFont));
                billToCell.addElement(new Paragraph(nomeCli, bold));
                billToCell.addElement(new Paragraph("NIF: " + nifCli, normal));
                if (!endCli.isEmpty()) billToCell.addElement(new Paragraph(endCli, normal));
                infoTable.addCell(billToCell);
                
                doc.add(infoTable);
                doc.add(new Paragraph(" "));

                // Metadata Table (Horizontal bar style)
                PdfPTable metaBar = new PdfPTable(4);
                metaBar.setWidthPercentage(100);
                metaBar.setSpacingBefore(10);
                metaBar.setSpacingAfter(10);
                
                addMetaCell(metaBar, "DATA DE EMISSÃO", fatura.getDataEmissao() != null ? new SimpleDateFormat("dd/MM/yyyy").format(fatura.getDataEmissao()) : "-", smallFont, bold, lightGrayBg);
                addMetaCell(metaBar, "DATA DE VENCIMENTO", fatura.getDataEmissao() != null ? new SimpleDateFormat("dd/MM/yyyy").format(fatura.getDataEmissao()) : "-", smallFont, bold, lightGrayBg);
                addMetaCell(metaBar, "MOEDA", "AOA (Kwanza)", smallFont, bold, lightGrayBg);
                
                String refDoc = "N/A";
                if (fatura.getCompra() != null && fatura.getCompra().getFaturaReferencia() != null) {
                    java.util.List<Fatura> faturasOrig = faturaRepository.findByCompra(fatura.getCompra().getFaturaReferencia());
                    refDoc = !faturasOrig.isEmpty() ? faturasOrig.get(0).getNumeroFatura() : "#" + fatura.getCompra().getFaturaReferencia().getId();
                }
                addMetaCell(metaBar, "DOC. REFERÊNCIA", refDoc, smallFont, bold, lightGrayBg);
                
                doc.add(metaBar);
                doc.add(new Paragraph(" "));

                // Items Table
                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[] { 4f, 1f, 1.5f, 1f, 1.2f, 1.8f });

                addModernHeader(table, "DESCRIÇÃO", tableHeaderFont, primaryColor);
                addModernHeader(table, "QTD", tableHeaderFont, primaryColor);
                addModernHeader(table, "UNITÁRIO", tableHeaderFont, primaryColor);
                addModernHeader(table, "IVA", tableHeaderFont, primaryColor);
                addModernHeader(table, "VAL. IVA", tableHeaderFont, primaryColor);
                addModernHeader(table, "TOTAL", tableHeaderFont, primaryColor);

                double totalIva = 0;
                double subtotalGeral = 0;
                int rowCount = 0;

                if ("NC".equals(fatura.getTipoDocumento())) {
                    Devolucao dev = devolucaoRepository.findByNotaCredito(fatura);
                    if (dev != null && dev.getItens() != null) {
                        for (ItemDevolucao item : dev.getItens()) {
                            java.awt.Color currentBg = (rowCount % 2 == 0) ? java.awt.Color.WHITE : zebraColor;
                            double subtotal = item.getSubtotal() != null ? item.getSubtotal() : 0.0;
                            double valorIvaItem = item.getIvaValor() != null ? item.getIvaValor() : 0.0;
                            double percIva = item.getIvaPercentual() != null ? item.getIvaPercentual() : 0.0;

                            subtotalGeral += subtotal;
                            totalIva += valorIvaItem;

                            String nomeProd = item.getProduto() != null ? item.getProduto().getNome() : "Produto N/A";
                            table.addCell(modernCell(nomeProd, normal, Element.ALIGN_LEFT, currentBg, borderColor));
                            table.addCell(modernCell(String.valueOf(item.getQuantidade()), normal, Element.ALIGN_CENTER, currentBg, borderColor));
                            table.addCell(modernCell(df.format(item.getPreco()), normal, Element.ALIGN_RIGHT, currentBg, borderColor));
                            table.addCell(modernCell(df.format(percIva) + "%", normal, Element.ALIGN_CENTER, currentBg, borderColor));
                            table.addCell(modernCell(df.format(valorIvaItem), normal, Element.ALIGN_RIGHT, currentBg, borderColor));
                            table.addCell(modernCell(df.format(subtotal), bold, Element.ALIGN_RIGHT, currentBg, borderColor));
                            rowCount++;
                        }
                    }
                } else if (fatura.getCompra() != null && fatura.getCompra().getItens() != null) {
                    for (ao.co.hzconsultoria.efacturacao.model.ItemCompra item : fatura.getCompra().getItens()) {
                        java.awt.Color currentBg = (rowCount % 2 == 0) ? java.awt.Color.WHITE : zebraColor;
                        double subtotal = item.getSubtotal() != null ? item.getSubtotal() : 0.0;
                        double valorIvaItem = item.getIva() != null ? item.getIva() : 0.0;
                        double percIva = item.getIvaPercentual() != null ? item.getIvaPercentual() : 14.0;

                        subtotalGeral += subtotal;
                        totalIva += valorIvaItem;

                        table.addCell(modernCell(item.getNomeProduto(), normal, Element.ALIGN_LEFT, currentBg, borderColor));
                        table.addCell(modernCell(String.valueOf(item.getQuantidade()), normal, Element.ALIGN_CENTER, currentBg, borderColor));
                        table.addCell(modernCell(df.format(item.getPreco()), normal, Element.ALIGN_RIGHT, currentBg, borderColor));
                        table.addCell(modernCell(df.format(percIva) + "%", normal, Element.ALIGN_CENTER, currentBg, borderColor));
                        table.addCell(modernCell(df.format(valorIvaItem), normal, Element.ALIGN_RIGHT, currentBg, borderColor));
                        table.addCell(modernCell(df.format(subtotal), bold, Element.ALIGN_RIGHT, currentBg, borderColor));
                        rowCount++;
                    }
                }
                doc.add(table);

                // Totals Block
                PdfPTable totalTable = new PdfPTable(2);
                totalTable.setWidthPercentage(40);
                totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
                totalTable.setSpacingBefore(20);

                double comissao = 0.0;
                if (fatura.getCompra() != null && fatura.getCompra().getComissaoMulticaixa() != null) {
                    comissao = fatura.getCompra().getComissaoMulticaixa();
                }
                double totalFinalComLiquido = (subtotalGeral + totalIva) - comissao;

                addModernTotalRow(totalTable, "SUBTOTAL", df.format(subtotalGeral), normal, normal, null, borderColor);
                addModernTotalRow(totalTable, "TOTAL IVA", df.format(totalIva), normal, normal, null, borderColor);
                if (comissao > 0) {
                    addModernTotalRow(totalTable, "DESC. COMISSÃO TPA", "-" + df.format(comissao), normal, normal, null, borderColor);
                }
                addModernTotalRow(totalTable, "TOTAL FINAL", df.format(totalFinalComLiquido) + " Kz", fontSubtitle, fontSubtitle, lightGrayBg, primaryColor);

                doc.add(totalTable);

                // Footer Section
                doc.add(new Paragraph(" "));
                
                PdfPTable separator = new PdfPTable(1);
                separator.setWidthPercentage(100);
                PdfPCell sepCell = new PdfPCell();
                sepCell.setBorder(0);
                sepCell.setBorderWidthTop(0.5f);
                sepCell.setBorderColorTop(borderColor);
                separator.addCell(sepCell);
                doc.add(separator);
                
                doc.add(new Paragraph(" "));

                PdfPTable footerTable = new PdfPTable(2);
                footerTable.setWidthPercentage(100);
                footerTable.setWidths(new float[]{ 6, 4 });

                PdfPCell qrCell = new PdfPCell();
                qrCell.setBorder(0);
                try {
                    Image qr = gerarQrCode(fatura.getNumeroFatura() + "|" + fatura.getHash());
                    if (qr != null) {
                        qr.scaleToFit(60, 60);
                        qrCell.addElement(qr);
                    }
                } catch (Exception ignored) {}
                
                String hash = fatura.getHash();
                String complianceHash = (hash != null && hash.length() >= 31) ? "" + hash.charAt(0) + hash.charAt(10) + hash.charAt(20) + hash.charAt(30) : "-";
                ConfiguracaoSistemaEntity configSistema = configuracaoSistemaRepository.findById(1L).orElse(new ConfiguracaoSistemaEntity());
                String certNo = (configSistema != null && configSistema.getAgtCertificadoNumero() != null) ? configSistema.getAgtCertificadoNumero() : "0000";
                
                qrCell.addElement(new Paragraph("Hash AGT: " + hash, smallFont));
                qrCell.addElement(new Paragraph(complianceHash + "-Processado por programa validado n.º " + certNo + "/AGT", smallFont));
                footerTable.addCell(qrCell);

                PdfPCell thanksCell = new PdfPCell();
                thanksCell.setBorder(0);
                thanksCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                Paragraph pThanks = new Paragraph("OBRIGADO PELA PREFERÊNCIA", bold);
                pThanks.setAlignment(Element.ALIGN_RIGHT);
                thanksCell.addElement(pThanks);

                if (configuracao != null && configuracao.isUsarRodapePersonalizadoEmDocumentos() && configuracao.getRodapePersonalizado() != null && !configuracao.getRodapePersonalizado().isEmpty()) {
                    Paragraph pRodape = new Paragraph(configuracao.getRodapePersonalizado(), smallFont);
                    pRodape.setAlignment(Element.ALIGN_RIGHT);
                    thanksCell.addElement(pRodape);
                } else {
                    Paragraph p2 = new Paragraph("Os bens/serviços foram colocados à disposição na data do documento.", smallFont);
                    p2.setAlignment(Element.ALIGN_RIGHT);
                    thanksCell.addElement(p2);
                }
                footerTable.addCell(thanksCell);
                doc.add(footerTable);
                
                if ("FP".equals(fatura.getTipoDocumento())) {
                    Paragraph pWarning = new Paragraph("ESTE DOCUMENTO NÃO SERVE DE FACTURA", bold);
                    pWarning.setAlignment(Element.ALIGN_CENTER);
                    pWarning.setSpacingBefore(15);
                    doc.add(pWarning);
                }

            } finally {
                if (doc.isOpen()) { doc.close(); }
            }

            // After document and stream are closed, move temp to final atomically
            try {
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                log.info("Movido PDF temporário {} para {}", tempPath, finalPath);
            } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
                // Fallback if atomic move is not supported on the filesystem
                Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Movido (fallback) PDF temporário {} para {}", tempPath, finalPath);
            }
        }
    }

    /**
     * Resolve o caminho absoluto de uma imagem armazenada em /uploads/
     * Converte uma URL relativa como "/uploads/logo/..." em um caminho absoluto
     * acessível
     */
    private String resolverCaminhoImagem(String caminhoRelativo) {
        if (caminhoRelativo == null || caminhoRelativo.isEmpty()) {
            return null;
        }

        // Se já é um caminho absoluto, retorna como está
        if (new File(caminhoRelativo).isAbsolute()) {
            return caminhoRelativo;
        }

        // Remove a barra inicial se existir (/uploads/logo/... -> uploads/logo/...)
        String caminhoLimpo = caminhoRelativo.startsWith("/") ? caminhoRelativo.substring(1) : caminhoRelativo;

        // Resolve a partir do diretório raiz do projeto
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();

        // Constrói o caminho absoluto completo
        Path imagemPath = projectRoot.resolve(caminhoLimpo).normalize();

        // Verifica se o arquivo existe antes de retornar
        if (Files.exists(imagemPath)) {
            return imagemPath.toString();
        }

        log.warn("Arquivo de imagem não encontrado: {}", imagemPath);
        return null;
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont, java.awt.Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorder(0);
        cell.setPadding(8);
        cell.addElement(new Paragraph(label, labelFont));
        cell.addElement(new Paragraph(value, valueFont));
        table.addCell(cell);
    }

    private void addModernTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont, java.awt.Color bg, java.awt.Color borderColor) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(0);
        c1.setBorderWidthBottom(0.5f);
        c1.setBorderColorBottom(borderColor);
        c1.setPadding(6);
        if (bg != null) c1.setBackgroundColor(bg);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, valueFont));
        c2.setBorder(0);
        c2.setBorderWidthBottom(0.5f);
        c2.setBorderColorBottom(borderColor);
        c2.setPadding(6);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (bg != null) c2.setBackgroundColor(bg);
        table.addCell(c2);
    }

    private PdfPCell modernCell(String text, Font font, int align, java.awt.Color bg, java.awt.Color borderColor) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(0);
        c.setBorderWidthBottom(0.5f);
        c.setBorderColorBottom(borderColor);
        c.setPadding(8);
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        return c;
    }

    private void addModernHeader(PdfPTable table, String text, Font font, java.awt.Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBackgroundColor(bg);
        c.setBorder(0);
        c.setPadding(10);
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(c);
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
