package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class FolhaSalarioService {

    @Autowired
    private ColaboradorRepository colaboradorRepository;

    @Autowired
    private FolhaProcessamentoRepository folhaRepository;

    @Autowired
    private SalarioProcessadoRepository salarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private SalarioProcessadoSubsidioRepository spSubsidioRepository;

    @Autowired
    private ParametroPayrollRepository parametroPayrollRepository;

    @Autowired
    private EscalaoIrtRepository escalaoIrtRepository;

    private static final DecimalFormat DF = new DecimalFormat("#,##0.00");

    @Transactional
    public FolhaProcessamento criarRascunhoFolha(int mes, int ano, Long empresaId) throws Exception {
        Optional<FolhaProcessamento> exist = folhaRepository.findByMesAndAnoAndEmpresa_Id(mes, ano, empresaId);
        if (exist.isPresent()) {
            FolhaProcessamento folha = exist.get();
            if (!"RASCUNHO".equals(folha.getEstado())) {
                throw new IllegalStateException("Esta folha já foi processada ou paga e não pode ser alterada.");
            }
            // Limpar salários anteriores do rascunho existente
            List<SalarioProcessado> salarios = salarioRepository.findByFolhaProcessamento_Id(folha.getId());
            salarioRepository.deleteAll(salarios);
            
            folha.setDataProcessamento(LocalDateTime.now());
            folhaRepository.save(folha);
            gerarSalariosIndividuais(folha, empresaId);
            return folha;
        }

        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));

        FolhaProcessamento novaFolha = new FolhaProcessamento();
        novaFolha.setMes(mes);
        novaFolha.setAno(ano);
        novaFolha.setDataProcessamento(LocalDateTime.now());
        novaFolha.setEstado("RASCUNHO");
        novaFolha.setEmpresa(empresa);
        novaFolha = folhaRepository.save(novaFolha);

        gerarSalariosIndividuais(novaFolha, empresaId);
        return novaFolha;
    }

    private void gerarSalariosIndividuais(FolhaProcessamento folha, Long empresaId) {
        List<Colaborador> colaboradores = colaboradorRepository.findByEmpresa_Id(empresaId);
        for (Colaborador col : colaboradores) {
            SalarioProcessado sp = calcularSalario(col, folha);
            salarioRepository.save(sp);
        }
    }

    public SalarioProcessado calcularSalario(Colaborador col, FolhaProcessamento folha) {
        SalarioProcessado sp = new SalarioProcessado();
        sp.setFolhaProcessamento(folha);
        sp.setColaborador(col);
        sp.setSalarioBase(col.getSalarioBase());
        sp.setSubsidioFerias(0.0);
        sp.setSubsidioNatal(0.0);

        // Copiar subsídios dinâmicos do colaborador para o snapshot do salário processado
        for (ColaboradorSubsidio cs : col.getSubsidios()) {
            SalarioProcessadoSubsidio sps = new SalarioProcessadoSubsidio(sp, cs.getSubsidio(), cs.getValor());
            sp.getSubsidios().add(sps);
        }

        recalcularSalarioInterno(sp);
        return sp;
    }

    public ParametroPayroll getOrCreateParametros(Long empresaId) {
        return parametroPayrollRepository.findByEmpresaId(empresaId)
                .orElseGet(() -> {
                    Empresa emp = empresaRepository.findById(empresaId).orElse(null);
                    if (emp == null) return new ParametroPayroll();
                    ParametroPayroll p = new ParametroPayroll(emp);
                    return parametroPayrollRepository.save(p);
                });
    }

    public void inicializarEscaloesPadraoIrtSeNecessario(Long empresaId) {
        List<EscalaoIrt> escaloes = escalaoIrtRepository.findByEmpresaIdOrderByLimiteInferiorAsc(empresaId);
        if (escaloes.isEmpty()) {
            Empresa emp = empresaRepository.findById(empresaId).orElse(null);
            if (emp != null) {
                escalaoIrtRepository.save(new EscalaoIrt(emp, 0.0, 100000.0, 0.0, 0.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 100000.0, 150000.0, 3000.0, 10.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 150000.0, 200000.0, 8000.0, 13.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 200000.0, 300000.0, 14500.0, 16.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 300000.0, 500000.0, 30500.0, 18.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 500000.0, 1000000.0, 66500.0, 20.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 1000000.0, 1500000.0, 166500.0, 21.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 1500000.0, 2000000.0, 271500.0, 22.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 2000000.0, 5000000.0, 381500.0, 23.0));
                escalaoIrtRepository.save(new EscalaoIrt(emp, 5000000.0, null, 1071500.0, 25.0));
            }
        }
    }

    public double calcularIrtDinamico(double valor, Long empresaId) {
        inicializarEscaloesPadraoIrtSeNecessario(empresaId);
        List<EscalaoIrt> escaloes = escalaoIrtRepository.findByEmpresaIdOrderByLimiteInferiorAsc(empresaId);
        
        for (EscalaoIrt esc : escaloes) {
            double inf = esc.getLimiteInferior();
            Double sup = esc.getLimiteSuperior();
            
            if (valor > inf && (sup == null || valor <= sup)) {
                double excesso = valor - inf;
                double imposto = esc.getParcelaFixa() + (excesso * (esc.getTaxaExcesso() / 100.0));
                return imposto;
            }
        }
        return 0.0;
    }

    public void recalcularSalarioInterno(SalarioProcessado sp) {
        double base = sp.getSalarioBase();
        double ferias = sp.getSubsidioFerias();
        double natal = sp.getSubsidioNatal();
        Long empresaId = sp.getFolhaProcessamento().getEmpresa().getId();
        ParametroPayroll params = getOrCreateParametros(empresaId);

        // 1. Rendimento Ilíquido: base + todos os subsídios dinâmicos + férias + natal
        double iliquido = base + ferias + natal;
        double rendimentoSujeitoInss = base; // férias e natal são isentos de INSS
        double rendimentoSujeitoIrt = base + ferias + natal;

        for (SalarioProcessadoSubsidio sps : sp.getSubsidios()) {
            Subsidio sub = sps.getSubsidio();
            double val = sps.getValor();
            iliquido += val;

            if (sub.isSujeitoInss()) {
                double excessoInss = Math.max(0.0, val - sub.getLimiteIsencaoInss());
                rendimentoSujeitoInss += excessoInss;
            }
            if (sub.isSujeitoIrt()) {
                double excessoIrt = Math.max(0.0, val - sub.getLimiteIsencaoIrt());
                rendimentoSujeitoIrt += excessoIrt;
            }
        }

        sp.setRendimentoIliquido(iliquido);

        // 2. INSS
        double inssTrabalhador = rendimentoSujeitoInss * (params.getTaxaInssTrabalhador() / 100.0);
        double inssEmpresa = rendimentoSujeitoInss * (params.getTaxaInssEmpresa() / 100.0);
        sp.setDescontoSegurancaSocial(inssTrabalhador);
        sp.setEncargoEmpresaSegurancaSocial(inssEmpresa);

        // 3. IRT – Matéria Coletável = Rendimento Sujeito IRT - INSS
        double materiaColetavel = Math.max(0.0, rendimentoSujeitoIrt - inssTrabalhador);
        double irtBruto = calcularIrtDinamico(materiaColetavel, empresaId);

        // Desconto por dependentes
        if (sp.getColaborador().getDependentes() > 0) {
            double descPorc = params.getDescontoIrtDependente();
            irtBruto = irtBruto * (1.0 - (descPorc / 100.0));
        }
        sp.setDescontoIrt(irtBruto);

        // 4. Salário Líquido
        sp.setSalarioLiquido(iliquido - inssTrabalhador - irtBruto);
    }

    @Transactional
    public void processarFolha(Long folhaId) {
        FolhaProcessamento folha = folhaRepository.findById(folhaId)
                .orElseThrow(() -> new IllegalArgumentException("Folha não encontrada"));
        folha.setEstado("PROCESSADO");
        folhaRepository.save(folha);
    }

    @Transactional
    public void pagarFolha(Long folhaId) {
        FolhaProcessamento folha = folhaRepository.findById(folhaId)
                .orElseThrow(() -> new IllegalArgumentException("Folha não encontrada"));
        folha.setEstado("PAGO");
        folhaRepository.save(folha);
    }

    @Transactional
    public void eliminarFolha(Long folhaId) {
        FolhaProcessamento folha = folhaRepository.findById(folhaId)
                .orElseThrow(() -> new IllegalArgumentException("Folha não encontrada"));
        if (!"RASCUNHO".equals(folha.getEstado())) {
            throw new IllegalStateException("Apenas folhas em estado RASCUNHO podem ser eliminadas.");
        }
        List<SalarioProcessado> salarios = salarioRepository.findByFolhaProcessamento_Id(folhaId);
        salarioRepository.deleteAll(salarios);
        folhaRepository.delete(folha);
    }

    public byte[] gerarReciboPdf(Long salarioId) throws Exception {
        SalarioProcessado sp = salarioRepository.findById(salarioId)
                .orElseThrow(() -> new IllegalArgumentException("Registo de salário não encontrado"));

        Colaborador col = sp.getColaborador();
        Empresa emp = col.getEmpresa();

        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        // Cores do Primavera (sóbrio, cinza e azul escuro)
        java.awt.Color priBlue = new java.awt.Color(27, 38, 59); // Azul escuro Primavera
        java.awt.Color priBorder = new java.awt.Color(180, 180, 180); // Cinza para bordas
        java.awt.Color priBgLight = new java.awt.Color(245, 245, 245); // Fundo cinza claro

        // Fontes do Primavera (pequenas e limpas)
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, priBlue);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new java.awt.Color(100, 100, 100));
        Font valFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new java.awt.Color(30, 30, 30));
        Font valBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(30, 30, 30));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, java.awt.Color.WHITE);

        // 1. Cabeçalho Principal (Empresa e Recibo) em Caixa Única
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{5.5f, 4.5f});
        
        // Célula da Empresa
        PdfPCell empCell = new PdfPCell();
        empCell.setBorder(Rectangle.BOX);
        empCell.setBorderColor(priBorder);
        empCell.setPadding(8f);
        
        Paragraph pEmpName = new Paragraph(emp.getNome().toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, priBlue));
        pEmpName.setSpacingAfter(4f);
        empCell.addElement(pEmpName);
        empCell.addElement(new Paragraph("NIF: " + emp.getNif(), valFont));
        empCell.addElement(new Paragraph(emp.getEndereco(), valFont));
        headerTable.addCell(empCell);

        // Célula do Recibo
        PdfPCell recCell = new PdfPCell();
        recCell.setBorder(Rectangle.BOX);
        recCell.setBorderColor(priBorder);
        recCell.setPadding(8f);
        
        Paragraph pRecTitle = new Paragraph("RECIBO DE VENCIMENTO", titleFont);
        pRecTitle.setSpacingAfter(4f);
        recCell.addElement(pRecTitle);
        recCell.addElement(new Paragraph("Período de Processamento: " + String.format("%02d/%d", sp.getFolhaProcessamento().getMes(), sp.getFolhaProcessamento().getAno()), valBold));
        recCell.addElement(new Paragraph("Data de Emissão: " + LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), valFont));
        recCell.addElement(new Paragraph("Via: Original", valFont));
        headerTable.addCell(recCell);

        document.add(headerTable);

        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(5);
        document.add(spacer);

        // 2. Quadro do Trabalhador (Borda externa cinza)
        PdfPTable workerTable = new PdfPTable(4);
        workerTable.setWidthPercentage(100f);
        workerTable.setWidths(new float[]{1.5f, 4.5f, 2f, 2f});

        workerTable.addCell(createPrimaveraCell("Cód. Trab.", String.valueOf(col.getId()), labelFont, valBold, priBorder));
        workerTable.addCell(createPrimaveraCell("Nome do Trabalhador", col.getNome(), labelFont, valBold, priBorder));
        workerTable.addCell(createPrimaveraCell("N.º Contr. (NIF)", col.getNif(), labelFont, valFont, priBorder));
        workerTable.addCell(createPrimaveraCell("N.º Seg. Social", col.getNif() != null && col.getNif().length() > 8 ? "SS-" + col.getNif().substring(0, 8) : "-", labelFont, valFont, priBorder));

        workerTable.addCell(createPrimaveraCell("Categoria / Cargo", col.getCargo(), labelFont, valFont, priBorder));
        workerTable.addCell(createPrimaveraCell("Secção / Depto.", col.getDepartamento() != null ? col.getDepartamento().getNome() : "-", labelFont, valFont, priBorder));
        workerTable.addCell(createPrimaveraCell("Data Admissão", col.getDataAdmissao().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")), labelFont, valFont, priBorder));
        workerTable.addCell(createPrimaveraCell("IBAN para Pagamento", col.getIban() != null && !col.getIban().isEmpty() ? col.getIban() : "Não indicado", labelFont, valFont, priBorder));

        document.add(workerTable);
        document.add(spacer);

        // 3. Tabela de Movimentos (Vencimentos e Descontos)
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100f);
        table.setWidths(new float[]{1f, 4.5f, 1.5f, 1.5f, 1.5f});

        String[] headers = {"Cód.", "Descrição", "Quant./Dias", "Abonos (Kz)", "Descontos (Kz)"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(priBlue);
            cell.setPadding(6f);
            cell.setBorderColor(priBlue);
            if (header.contains("Kz")) {
                cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            } else if (header.equals("Quant./Dias") || header.equals("Cód.")) {
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            }
            table.addCell(cell);
        }

        // Adicionar Linhas de Movimentos
        addPrimaveraRow(table, "100", "Salário Base", "30", sp.getSalarioBase(), 0.0, valFont, priBorder);
        
        // Subsídios dinâmicos
        for (SalarioProcessadoSubsidio sps : sp.getSubsidios()) {
            if (sps.getValor() > 0) {
                String cod = sps.getSubsidio().getCodigo() != null ? sps.getSubsidio().getCodigo() : "100";
                addPrimaveraRow(table, cod, sps.getSubsidio().getNome(), "30", sps.getValor(), 0.0, valFont, priBorder);
            }
        }

        if (sp.getSubsidioFerias() > 0) {
            addPrimaveraRow(table, "103", "Subsídio de Férias", "1", sp.getSubsidioFerias(), 0.0, valFont, priBorder);
        }
        if (sp.getSubsidioNatal() > 0) {
            addPrimaveraRow(table, "104", "Subsídio de Natal", "1", sp.getSubsidioNatal(), 0.0, valFont, priBorder);
        }

        // Descontos
        addPrimaveraRow(table, "500", "Segurança Social (Desconto 3%)", "-", 0.0, sp.getDescontoSegurancaSocial(), valFont, priBorder);
        if (sp.getDescontoIrt() > 0) {
            addPrimaveraRow(table, "501", "I.R.T. (Imposto de Trabalho)", "-", 0.0, sp.getDescontoIrt(), valFont, priBorder);
        }

        // Adicionar Linhas Vazias de preenchimento para simular o estilo Primavera (pelo menos 12 linhas no total)
        int currentRows = 1 + sp.getSubsidios().stream().filter(sps -> sps.getValor() > 0).mapToInt(sps -> 1).sum()
                + (sp.getSubsidioFerias() > 0 ? 1 : 0) + (sp.getSubsidioNatal() > 0 ? 1 : 0)
                + 1 + (sp.getDescontoIrt() > 0 ? 1 : 0);
        
        for (int i = currentRows; i < 12; i++) {
            addPrimaveraRow(table, "", "", "", 0.0, 0.0, valFont, priBorder);
        }

        document.add(table);

        // 4. Rodapé de Totais
        PdfPTable totalsTable = new PdfPTable(3);
        totalsTable.setWidthPercentage(100f);
        totalsTable.setWidths(new float[]{3.5f, 3.5f, 3f});
        totalsTable.setSpacingBefore(10f);
        
        // Total Abonos
        PdfPCell cAb = new PdfPCell();
        cAb.setBorder(Rectangle.BOX);
        cAb.setBorderColor(priBorder);
        cAb.setPadding(6f);
        cAb.addElement(new Paragraph("TOTAL ABONOS", labelFont));
        Paragraph pAbVal = new Paragraph("Kz " + DF.format(sp.getRendimentoIliquido()), valBold);
        pAbVal.setAlignment(Element.ALIGN_RIGHT);
        cAb.addElement(pAbVal);
        totalsTable.addCell(cAb);

        // Total Descontos
        PdfPCell cDsc = new PdfPCell();
        cDsc.setBorder(Rectangle.BOX);
        cDsc.setBorderColor(priBorder);
        cDsc.setPadding(6f);
        cDsc.addElement(new Paragraph("TOTAL DESCONTOS", labelFont));
        Paragraph pDescVal = new Paragraph("Kz " + DF.format(sp.getDescontoSegurancaSocial() + sp.getDescontoIrt()), valBold);
        pDescVal.setAlignment(Element.ALIGN_RIGHT);
        cDsc.addElement(pDescVal);
        totalsTable.addCell(cDsc);

        // Líquido Transferido
        PdfPCell cLiq = new PdfPCell();
        cLiq.setBorder(Rectangle.BOX);
        cLiq.setBorderColor(priBorder);
        cLiq.setBackgroundColor(priBgLight);
        cLiq.setPadding(6f);
        cLiq.addElement(new Paragraph("LÍQUIDO A RECEBER", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, priBlue)));
        Paragraph pLiqVal = new Paragraph("Kz " + DF.format(sp.getSalarioLiquido()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, priBlue));
        pLiqVal.setAlignment(Element.ALIGN_RIGHT);
        cLiq.addElement(pLiqVal);
        totalsTable.addCell(cLiq);

        document.add(totalsTable);

        // 5. Declaração de Recebido e Assinatura
        Paragraph signSpacer = new Paragraph(" ");
        signSpacer.setSpacingBefore(15f);
        document.add(signSpacer);

        PdfPTable footTable = new PdfPTable(2);
        footTable.setWidthPercentage(100f);
        footTable.setWidths(new float[]{6f, 4f});

        PdfPCell statementCell = new PdfPCell();
        statementCell.setBorder(Rectangle.NO_BORDER);
        Paragraph pStatement = new Paragraph("Declaro ter recebido a importância líquida acima referida.", FontFactory.getFont(FontFactory.HELVETICA, 7, new java.awt.Color(80, 80, 80)));
        statementCell.addElement(pStatement);
        statementCell.addElement(new Paragraph("\nData: ____/____/________", FontFactory.getFont(FontFactory.HELVETICA, 8, new java.awt.Color(80, 80, 80))));
        footTable.addCell(statementCell);

        PdfPCell signatureCell = new PdfPCell();
        signatureCell.setBorder(Rectangle.NO_BORDER);
        signatureCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pSignLine = new Paragraph("\n\n_____________________________________\nAssinatura do Trabalhador", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(80, 80, 80)));
        pSignLine.setAlignment(Element.ALIGN_CENTER);
        footTable.addCell(signatureCell);

        document.add(footTable);

        document.close();
        return out.toByteArray();
    }

    private PdfPCell createPrimaveraCell(String label, String value, Font labelFont, Font valFont, java.awt.Color borderCol) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(borderCol);
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(4f);
        
        Paragraph pLabel = new Paragraph(label, labelFont);
        pLabel.setSpacingAfter(1f);
        Paragraph pValue = new Paragraph(value != null ? value : "-", valFont);
        
        cell.addElement(pLabel);
        cell.addElement(pValue);
        return cell;
    }

    private void addPrimaveraRow(PdfPTable table, String code, String desc, String qty, double inc, double dec, Font font, java.awt.Color borderCol) {
        PdfPCell cCode = new PdfPCell(new Phrase(code, font));
        cCode.setPadding(4f);
        cCode.setBorderColor(borderCol);
        cCode.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cCode);

        PdfPCell cDesc = new PdfPCell(new Phrase(desc, font));
        cDesc.setPadding(4f);
        cDesc.setBorderColor(borderCol);
        table.addCell(cDesc);

        PdfPCell cQty = new PdfPCell(new Phrase(qty, font));
        cQty.setPadding(4f);
        cQty.setBorderColor(borderCol);
        cQty.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cQty);

        String incStr = inc > 0 ? DF.format(inc) : "";
        PdfPCell cInc = new PdfPCell(new Phrase(incStr, font));
        cInc.setPadding(4f);
        cInc.setBorderColor(borderCol);
        cInc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cInc);

        String decStr = dec > 0 ? DF.format(dec) : "";
        PdfPCell cDec = new PdfPCell(new Phrase(decStr, font));
        cDec.setPadding(4f);
        cDec.setBorderColor(borderCol);
        cDec.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cDec);
    }

    private PdfPCell createWorkerInfoCell(String label, String value, Font labelFont, Font valFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6f);
        cell.setBorderColor(new java.awt.Color(226, 232, 240));
        cell.setBackgroundColor(new java.awt.Color(248, 250, 252));
        cell.addElement(new Phrase(label, labelFont));
        cell.addElement(new Phrase(value, valFont));
        return cell;
    }

    private void addCalculatedRow(PdfPTable table, String code, String desc, double inc, double dec, Font font, java.awt.Color bg) {
        java.awt.Color border = new java.awt.Color(226, 232, 240);
        
        PdfPCell cCode = new PdfPCell(new Phrase(code, font));
        cCode.setPadding(6f);
        cCode.setBorderColor(border);
        cCode.setBackgroundColor(bg);
        table.addCell(cCode);

        PdfPCell cDesc = new PdfPCell(new Phrase(desc, font));
        cDesc.setPadding(6f);
        cDesc.setBorderColor(border);
        cDesc.setBackgroundColor(bg);
        table.addCell(cDesc);

        String incStr = inc > 0 ? DF.format(inc) : "-";
        PdfPCell cInc = new PdfPCell(new Phrase(incStr, font));
        cInc.setPadding(6f);
        cInc.setBorderColor(border);
        cInc.setBackgroundColor(bg);
        cInc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cInc);

        String decStr = dec > 0 ? DF.format(dec) : "-";
        PdfPCell cDec = new PdfPCell(new Phrase(decStr, font));
        cDec.setPadding(6f);
        cDec.setBorderColor(border);
        cDec.setBackgroundColor(bg);
        cDec.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cDec);
    }

    private PdfPCell createTotalCell(String label, String value, Font labelFont, Font valFont, boolean isRed) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6f);
        cell.setBorder(Rectangle.NO_BORDER);
        
        Paragraph p = new Paragraph();
        p.add(new Phrase(label + " ", labelFont));
        Font colorFont = new Font(valFont);
        if (isRed) {
            colorFont.setColor(new java.awt.Color(239, 68, 68)); // red
        } else {
            colorFont.setColor(new java.awt.Color(16, 185, 129)); // green
        }
        p.add(new Phrase(value, colorFont));
        cell.addElement(p);
        return cell;
    }

    public String gerarFolhaInssCsv(Long folhaId) {
        FolhaProcessamento folha = folhaRepository.findById(folhaId)
                .orElseThrow(() -> new IllegalArgumentException("Folha não encontrada"));

        List<SalarioProcessado> salarios = salarioRepository.findByFolhaProcessamento_Id(folhaId);
        StringBuilder sb = new StringBuilder();

        sb.append("NIF_EMPRESA;NOME_EMPRESA;PERIODO;NOME_TRABALHADOR;NIF_TRABALHADOR;SALARIO_BASE;RENDIMENTO_SUJEITO_SS;INSS_TRABALHADOR_3;INSS_PATRONAL_8;TOTAL_11\n");

        Empresa emp = folha.getEmpresa();
        String period = String.format("%02d/%d", folha.getMes(), folha.getAno());

        for (SalarioProcessado sp : salarios) {
            Colaborador col = sp.getColaborador();
            // Rendimento sujeito a INSS calculado dinamicamente
            double rendimentoSujeitoInss = sp.getSalarioBase();
            for (SalarioProcessadoSubsidio sps : sp.getSubsidios()) {
                if (sps.getSubsidio().isSujeitoInss()) {
                    rendimentoSujeitoInss += Math.max(0.0, sps.getValor() - sps.getSubsidio().getLimiteIsencaoInss());
                }
            }

            sb.append(emp.getNif()).append(";")
              .append(emp.getNome()).append(";")
              .append(period).append(";")
              .append(col.getNome()).append(";")
              .append(col.getNif()).append(";")
              .append(String.format("%.2f", sp.getSalarioBase())).append(";")
              .append(String.format("%.2f", rendimentoSujeitoInss)).append(";")
              .append(String.format("%.2f", sp.getDescontoSegurancaSocial())).append(";")
              .append(String.format("%.2f", sp.getEncargoEmpresaSegurancaSocial())).append(";")
              .append(String.format("%.2f", sp.getDescontoSegurancaSocial() + sp.getEncargoEmpresaSegurancaSocial())).append("\n");
        }

        return sb.toString();
    }

    public byte[] gerarGuiaIrtPdf(Long folhaId) throws Exception {
        FolhaProcessamento folha = folhaRepository.findById(folhaId)
                .orElseThrow(() -> new IllegalArgumentException("Folha não encontrada"));

        Empresa emp = folha.getEmpresa();
        List<SalarioProcessado> salarios = salarioRepository.findByFolhaProcessamento_Id(folhaId);

        double totalMateriaColetavel = 0.0;
        double totalIrt = 0.0;
        int numTrabalhadores = salarios.size();

        for (SalarioProcessado sp : salarios) {
            // Rendimento sujeito a IRT calculado dinamicamente
            double rendimentoSujeitoIrt = sp.getSalarioBase() + sp.getSubsidioFerias() + sp.getSubsidioNatal();
            for (SalarioProcessadoSubsidio sps : sp.getSubsidios()) {
                if (sps.getSubsidio().isSujeitoIrt()) {
                    rendimentoSujeitoIrt += Math.max(0.0, sps.getValor() - sps.getSubsidio().getLimiteIsencaoIrt());
                }
            }
            double mat = Math.max(0.0, rendimentoSujeitoIrt - sp.getDescontoSegurancaSocial());
            totalMateriaColetavel += mat;
            totalIrt += sp.getDescontoIrt();
        }

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);

        document.open();

        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new java.awt.Color(67, 97, 238));
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new java.awt.Color(30, 41, 59));
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new java.awt.Color(71, 85, 105));
        Font normalBold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new java.awt.Color(51, 65, 85));
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new java.awt.Color(51, 65, 85));
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, java.awt.Color.WHITE);

        // Header
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100f);
        headerTable.setWidths(new float[]{6f, 4f});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("GUIA DE RETENÇÃO NA FONTE - IRT", brandFont));
        leftCell.addElement(new Paragraph("Administração Geral Tributária (AGT)\nAngola", subtitleFont));
        headerTable.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(new Paragraph(emp.getNome(), normalBold));
        rightCell.addElement(new Paragraph("NIF: " + emp.getNif(), normalFont));
        rightCell.addElement(new Paragraph(emp.getEndereco(), normalFont));
        headerTable.addCell(rightCell);

        document.add(headerTable);

        Paragraph spacer = new Paragraph(" ");
        spacer.setSpacingAfter(15);
        document.add(spacer);

        // Detalhes da Guia
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100f);
        detailsTable.setSpacingAfter(20f);

        detailsTable.addCell(createWorkerInfoCell("Período Fiscal:", String.format("%02d/%d", folha.getMes(), folha.getAno()), normalBold, normalFont));
        detailsTable.addCell(createWorkerInfoCell("Imposto:", "IRT - Rendimento de Trabalho (Conta Outrem)", normalBold, normalFont));
        detailsTable.addCell(createWorkerInfoCell("Nº de Trabalhadores:", String.valueOf(numTrabalhadores), normalBold, normalFont));
        detailsTable.addCell(createWorkerInfoCell("Data de Retenção:", folha.getDataProcessamento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalBold, normalFont));

        document.add(detailsTable);

        // Resumo de Valores
        PdfPTable valuesTable = new PdfPTable(2);
        valuesTable.setWidthPercentage(100f);
        valuesTable.setWidths(new float[]{7f, 3f});
        valuesTable.setSpacingAfter(25f);

        addValueRow(valuesTable, "Total Matéria Coletável:", "Kz " + DF.format(totalMateriaColetavel), normalFont, false);
        addValueRow(valuesTable, "Total Imposto Retido (IRT a Pagar):", "Kz " + DF.format(totalIrt), normalBold, true);

        document.add(valuesTable);

        // Instruções de Pagamento e Notas Fiscais
        PdfPTable infoTable = new PdfPTable(1);
        infoTable.setWidthPercentage(100f);
        
        PdfPCell infoCell = new PdfPCell();
        infoCell.setPadding(10f);
        infoCell.setBackgroundColor(new java.awt.Color(254, 243, 199)); // amber light
        infoCell.setBorderColor(new java.awt.Color(217, 119, 6));
        infoCell.setBorderWidth(1f);
        
        Paragraph infoTitle = new Paragraph("INSTRUÇÕES DE PAGAMENTO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, new java.awt.Color(180, 83, 9)));
        Paragraph infoText = new Paragraph("Esta guia deve ser liquidada junto dos bancos comerciais autorizados ou via Portal do Contribuinte da AGT até ao final do mês seguinte ao do processamento dos salários.\n\n" +
                "Código do Imposto: 1040 - IRT Retenção na Fonte\n" +
                "A presente guia serve de comprovativo para fins de preenchimento do Modelo 2 do IRT.", normalFont);
        
        infoCell.addElement(infoTitle);
        infoCell.addElement(infoText);
        infoTable.addCell(infoCell);
        
        document.add(infoTable);

        document.close();
        return out.toByteArray();
    }

    private void addValueRow(PdfPTable table, String label, String value, Font font, boolean highlight) {
        java.awt.Color border = new java.awt.Color(226, 232, 240);
        java.awt.Color bg = highlight ? new java.awt.Color(220, 252, 231) : java.awt.Color.WHITE;

        PdfPCell cLabel = new PdfPCell(new Phrase(label, font));
        cLabel.setPadding(8f);
        cLabel.setBorderColor(border);
        cLabel.setBackgroundColor(bg);
        table.addCell(cLabel);

        PdfPCell cVal = new PdfPCell(new Phrase(value, font));
        cVal.setPadding(8f);
        cVal.setBorderColor(border);
        cVal.setBackgroundColor(bg);
        cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cVal);
    }
}
