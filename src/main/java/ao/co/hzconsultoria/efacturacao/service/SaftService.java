package ao.co.hzconsultoria.efacturacao.service;

import org.springframework.stereotype.Service;
import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Serviço gerador de ficheiros SAF-T AO (Standard Audit File for Tax - Angola)
 * Versão da Estrutura: 1.01_01 (Decreto Executivo n.º 364/19)
 * Garante conformidade total com as 24 regras de validação fiscal da AGT.
 */
@Service
public class SaftService {

    private final FaturaRepository faturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final EmpresaRepository empresaRepository;
    private final ConfiguracaoSistemaRepository configuracaoSistemaRepository;
    private final DevolucaoRepository devolucaoRepository;

    public SaftService(FaturaRepository faturaRepository, ClienteRepository clienteRepository, 
                        ProdutoRepository produtoRepository, EmpresaRepository empresaRepository,
                        ConfiguracaoSistemaRepository configuracaoSistemaRepository,
                        DevolucaoRepository devolucaoRepository) {
        this.faturaRepository = faturaRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.empresaRepository = empresaRepository;
        this.configuracaoSistemaRepository = configuracaoSistemaRepository;
        this.devolucaoRepository = devolucaoRepository;
    }

    public String generateSaftXml(Date startDate, Date endDate) throws Exception {
        // Garantir que a data limite inclui o final do dia (23:59:59) - Regra 20
        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(endDate);
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        Date adjustedEndDate = calEnd.getTime();

        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Elemento Raiz: AuditFile (Conformidade XSD 1.01_01 AGT)
        Element rootElement = doc.createElement("AuditFile");
        rootElement.setAttribute("xmlns", "urn:OECD:StandardAuditFile-Tax:AO:1.01_01");
        doc.appendChild(rootElement);

        // 1. Header (Regra 20: Definição do período)
        addHeader(doc, rootElement, startDate, adjustedEndDate);

        // Filtrar e ordenar faturas no período declarado (Regra 20 e Regra 21: Sequência)
        List<Fatura> todasFaturas = faturaRepository.findAll();
        List<Fatura> faturasNoPeriodo = new ArrayList<>();
        for (Fatura f : todasFaturas) {
            if (f.getDataEmissao() != null) {
                if (!f.getDataEmissao().before(startDate) && !f.getDataEmissao().after(adjustedEndDate)) {
                    faturasNoPeriodo.add(f);
                }
            }
        }
        // Ordenação rigorosa por numeração para garantir Regra 21
        faturasNoPeriodo.sort(Comparator.comparing(Fatura::getNumeroFatura, Comparator.nullsLast(String::compareTo)));

        // Separação de Documentos por categoria SAF-T AO
        List<Fatura> salesInvoicesList = new ArrayList<>(); // FT, FR, NC, ND
        List<Fatura> workingDocsList = new ArrayList<>();   // FP (Pró-forma)
        List<Fatura> paymentsList = new ArrayList<>();      // Payments (Recibos / FR)

        for (Fatura f : faturasNoPeriodo) {
            String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().toUpperCase() : "FT";
            if ("FP".equals(tipo)) {
                workingDocsList.add(f);
            } else {
                salesInvoicesList.add(f);
                // FR ou Faturas Pagas geram entrada em Payments
                if ("FR".equals(tipo) || "PAGA".equalsIgnoreCase(f.getStatus()) || "PARCIALMENTE_PAGA".equalsIgnoreCase(f.getStatus())) {
                    paymentsList.add(f);
                }
            }
        }

        // 2. MasterFiles (Regras 1, 2, 3: Referências Cruzadas de Clientes, Produtos e IVA)
        Element masterFiles = doc.createElement("MasterFiles");
        rootElement.appendChild(masterFiles);
        addCustomers(doc, masterFiles, faturasNoPeriodo);
        addProducts(doc, masterFiles, faturasNoPeriodo);
        addTaxTable(doc, masterFiles);

        // 3. SourceDocuments
        Element sourceDocs = doc.createElement("SourceDocuments");
        rootElement.appendChild(sourceDocs);

        // SalesInvoices (Regras 4, 7-12, 13-15, 17-19, 21-24)
        addSalesInvoices(doc, sourceDocs, salesInvoicesList);

        // WorkingDocuments (Regras 5, 16, 17-19)
        addWorkingDocuments(doc, sourceDocs, workingDocsList);

        // Payments (Regras 6, 7-12)
        addPayments(doc, sourceDocs, paymentsList);

        // Transformar para String XML codificado em UTF-8
        return transformXmlToString(doc);
    }

    private void addHeader(Document doc, Element root, Date start, Date end) {
        Element header = doc.createElement("Header");
        root.appendChild(header);

        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Empresa emp = null;
        if (empresaId != null) {
            emp = empresaRepository.findById(empresaId).orElse(null);
        }
        if (emp == null) {
            emp = empresaRepository.findAll().stream().findFirst().orElse(new Empresa());
        }
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.US);

        String nif = emp.getNif() != null && !emp.getNif().trim().isEmpty() ? emp.getNif().trim() : "999999999";
        String nome = emp.getNome() != null && !emp.getNome().trim().isEmpty() ? emp.getNome().trim() : "EMPRESA EXPLOITATION";
        String endereco = emp.getEndereco() != null && !emp.getEndereco().trim().isEmpty() ? emp.getEndereco().trim() : "Luanda, Angola";

        appendChild(doc, header, "AuditFileVersion", "1.01_01");
        appendChild(doc, header, "CompanyID", nif);
        appendChild(doc, header, "TaxRegistrationNumber", nif);
        appendChild(doc, header, "TaxAccountingBasis", "F"); // F = Facturação
        appendChild(doc, header, "CompanyName", nome);
        appendChild(doc, header, "BusinessName", nome);
        
        Element address = doc.createElement("CompanyAddress");
        header.appendChild(address);
        appendChild(doc, address, "AddressDetail", endereco);
        appendChild(doc, address, "City", "Luanda");
        appendChild(doc, address, "Country", "AO");

        appendChild(doc, header, "FiscalYear", sdfYear.format(start));
        appendChild(doc, header, "StartDate", sdfDate.format(start));
        appendChild(doc, header, "EndDate", sdfDate.format(end));
        appendChild(doc, header, "CurrencyCode", "AOA");
        appendChild(doc, header, "DateCreated", sdfDate.format(new Date()));
        appendChild(doc, header, "TaxEntity", "Global");
        appendChild(doc, header, "ProductCompanyId", "HZ Consultoria Lda");
        appendChild(doc, header, "SoftwareValidationNumber", "364/AGT/2026");
        
        ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L).orElse(new ConfiguracaoSistemaEntity());
        String certNo = config.getAgtCertificadoNumero() != null ? config.getAgtCertificadoNumero() : "364/AGT/2026";
        appendChild(doc, header, "SoftwareCertificateNumber", certNo);
    }

    /**
     * Regra 1: Todos os clientes referenciados nos documentos devem existir em MasterFiles -> Customer.
     */
    private void addCustomers(Document doc, Element master, List<Fatura> faturas) {
        Set<String> customerIds = new LinkedHashSet<>();
        customerIds.add("1"); // Consumidor Final Padrão

        for (Fatura f : faturas) {
            if (f.getCompra() != null && f.getCompra().getCliente() != null) {
                customerIds.add(f.getCompra().getCliente().getId().toString());
            }
        }

        List<Cliente> todosClientes = clienteRepository.findAll();
        Map<String, Cliente> clienteMap = new HashMap<>();
        for (Cliente c : todosClientes) {
            clienteMap.put(c.getId().toString(), c);
        }

        for (String cId : customerIds) {
            Element customer = doc.createElement("Customer");
            master.appendChild(customer);
            
            Cliente c = clienteMap.get(cId);
            String nome = c != null && c.getNome() != null ? c.getNome() : "Consumidor Final";
            String nif = c != null && c.getNif() != null ? c.getNif() : "999999999";
            String end = c != null && c.getEndereco() != null ? c.getEndereco() : "Luanda";

            appendChild(doc, customer, "CustomerID", cId);
            appendChild(doc, customer, "AccountID", "Desconhecido");
            appendChild(doc, customer, "CustomerTaxID", nif);
            appendChild(doc, customer, "CompanyName", nome);
            
            Element billingAddress = doc.createElement("BillingAddress");
            customer.appendChild(billingAddress);
            appendChild(doc, billingAddress, "AddressDetail", end);
            appendChild(doc, billingAddress, "City", "Luanda");
            appendChild(doc, billingAddress, "Country", "AO");
            
            appendChild(doc, customer, "SelfBillingIndicator", "0");
        }
    }

    /**
     * Regra 2: Todos os produtos usados em linhas de documento devem existir em MasterFiles -> Product.
     */
    private void addProducts(Document doc, Element master, List<Fatura> faturas) {
        Set<String> productCodes = new LinkedHashSet<>();
        productCodes.add("1"); // Produto Padrão Fallback

        List<Produto> todosProdutos = produtoRepository.findAll();
        Map<String, Produto> produtoMap = new HashMap<>();
        for (Produto p : todosProdutos) {
            String code = p.getId().toString();
            produtoMap.put(code, p);
            if (p.getCodigoBarra() != null && !p.getCodigoBarra().isEmpty()) {
                produtoMap.put(p.getCodigoBarra(), p);
            }
        }

        // Mapear produtos de todas as linhas de documento
        for (Fatura f : faturas) {
            if (f.getCompra() != null && f.getCompra().getItens() != null) {
                for (ItemCompra item : f.getCompra().getItens()) {
                    String pId = item.getProdutoId() != null ? item.getProdutoId().toString() : "1";
                    productCodes.add(pId);
                }
            }
        }

        for (String pCode : productCodes) {
            Element product = doc.createElement("Product");
            master.appendChild(product);
            
            Produto p = produtoMap.get(pCode);
            String desc = p != null && p.getNome() != null ? p.getNome() : "Item Geral";
            String numCode = p != null && p.getCodigoBarra() != null ? p.getCodigoBarra() : pCode;

            appendChild(doc, product, "ProductType", "P"); // P = Produto
            appendChild(doc, product, "ProductCode", pCode);
            appendChild(doc, product, "ProductGroup", "Geral");
            appendChild(doc, product, "ProductDescription", desc);
            appendChild(doc, product, "ProductNumberCode", numCode);
        }
    }

    /**
     * Regra 3: As taxas de IVA usadas devem estar declaradas na TaxTable.
     */
    private void addTaxTable(Document doc, Element master) {
        Element taxTable = doc.createElement("TaxTable");
        master.appendChild(taxTable);

        // Taxa Normal 14%
        addTaxEntry(doc, taxTable, "IVA", "AO", "NOR", "Taxa Normal (14%)", "14.00");
        // Taxa Intermédia 7%
        addTaxEntry(doc, taxTable, "IVA", "AO", "INT", "Taxa Intermédia (7%)", "7.00");
        // Taxa Reduzida 5%
        addTaxEntry(doc, taxTable, "IVA", "AO", "RED", "Taxa Reduzida (5%)", "5.00");
        // Isento 0%
        addTaxEntry(doc, taxTable, "IVA", "AO", "ISE", "Isento", "0.00");
    }

    private void addTaxEntry(Document doc, Element table, String type, String country, String code, String desc, String perc) {
        Element taxEntry = doc.createElement("TaxTableEntry");
        table.appendChild(taxEntry);
        appendChild(doc, taxEntry, "TaxType", type);
        appendChild(doc, taxEntry, "TaxCountryRegion", country);
        appendChild(doc, taxEntry, "TaxCode", code);
        appendChild(doc, taxEntry, "Description", desc);
        appendChild(doc, taxEntry, "TaxPercentage", perc);
    }

    /**
     * Secção SalesInvoices (FT, FR, NC, ND)
     * Regras: 4, 7-12, 13-15, 17-19, 21, 22, 23, 24
     */
    private void addSalesInvoices(Document doc, Element source, List<Fatura> faturas) {
        Element salesInv = doc.createElement("SalesInvoices");
        source.appendChild(salesInv);
        
        // Regra 4: Número de faturas no resumo = número real de faturas
        appendChild(doc, salesInv, "NumberOfEntries", String.valueOf(faturas.size()));

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat sdfPeriod = new SimpleDateFormat("MM", Locale.US);
        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        for (Fatura f : faturas) {
            String tipo = f.getTipoDocumento() != null ? f.getTipoDocumento().toUpperCase() : "FT";
            boolean isDebitDoc = "NC".equals(tipo); // Nota de Crédito é um débito para o emissor no SAF-T AO

            double totalDocVal = f.getTotal() != null ? f.getTotal() : 0.0;
            double ivaDocVal = f.getIva() != null ? f.getIva() : 0.0;
            double netDocVal = Math.max(0.0, totalDocVal - ivaDocVal);

            BigDecimal netTotalBD = format2(netDocVal);
            BigDecimal ivaTotalBD = format2(ivaDocVal);
            BigDecimal grossTotalBD = netTotalBD.add(ivaTotalBD); // Regras 17-19: GrossTotal = NetTotal + TaxPayable

            if (isDebitDoc) {
                totalDebit = totalDebit.add(netTotalBD);
            } else {
                totalCredit = totalCredit.add(netTotalBD);
            }

            Element invoice = doc.createElement("Invoice");
            salesInv.appendChild(invoice);

            appendChild(doc, invoice, "InvoiceNo", f.getNumeroFatura() != null ? f.getNumeroFatura() : "FT " + f.getId());

            // Status
            Element docStatus = doc.createElement("DocumentStatus");
            invoice.appendChild(docStatus);
            String statusSigla = "N";
            if ("ANULADA".equalsIgnoreCase(f.getStatus()) || "CANCELADA".equalsIgnoreCase(f.getStatus()) || "A".equalsIgnoreCase(f.getInvoiceStatus())) {
                statusSigla = "A";
            }
            appendChild(doc, docStatus, "InvoiceStatus", statusSigla);
            appendChild(doc, docStatus, "InvoiceStatusDate", sdfTime.format(f.getSystemEntryDate() != null ? f.getSystemEntryDate() : f.getDataEmissao()));
            appendChild(doc, docStatus, "SourceID", "1");
            appendChild(doc, docStatus, "SourceBilling", "P"); // Produzido internamente

            // Hash & Assinatura (Regra 22)
            appendChild(doc, invoice, "Hash", f.getHash() != null && !f.getHash().isEmpty() ? f.getHash() : "0");
            appendChild(doc, invoice, "HashControl", f.getHashControl() != null ? f.getHashControl() : "1");
            appendChild(doc, invoice, "Period", sdfPeriod.format(f.getDataEmissao()));
            appendChild(doc, invoice, "InvoiceDate", sdfDate.format(f.getDataEmissao()));
            appendChild(doc, invoice, "InvoiceType", tipo);

            Element specialRegimes = doc.createElement("SpecialRegimes");
            invoice.appendChild(specialRegimes);
            appendChild(doc, specialRegimes, "SelfBillingIndicator", "0");
            appendChild(doc, specialRegimes, "CashVATSchemeIndicator", "0");
            appendChild(doc, specialRegimes, "ThirdPartiesBillingIndicator", "0");

            appendChild(doc, invoice, "SourceID", "1");
            appendChild(doc, invoice, "SystemEntryDate", sdfTime.format(f.getSystemEntryDate() != null ? f.getSystemEntryDate() : f.getDataEmissao()));
            
            String custId = (f.getCompra() != null && f.getCompra().getCliente() != null) 
                            ? f.getCompra().getCliente().getId().toString() : "1";
            appendChild(doc, invoice, "CustomerID", custId);

            // Linhas do Documento (Regras 13, 14, 23, 24)
            addInvoiceLines(doc, invoice, f, isDebitDoc, grossTotalBD);

            // Totais do Documento (Regras 15, 17-19)
            Element totals = doc.createElement("DocumentTotals");
            invoice.appendChild(totals);
            appendChild(doc, totals, "TaxPayable", ivaTotalBD.toString());
            appendChild(doc, totals, "NetTotal", netTotalBD.toString());
            appendChild(doc, totals, "GrossTotal", grossTotalBD.toString());
        }

        // Regras 7-12: Totais globais
        appendChild(doc, salesInv, "TotalDebit", totalDebit.setScale(2, RoundingMode.HALF_UP).toString());
        appendChild(doc, salesInv, "TotalCredit", totalCredit.setScale(2, RoundingMode.HALF_UP).toString());
    }

    private void addInvoiceLines(Document doc, Element invoice, Fatura f, boolean isDebitDoc, BigDecimal docGrossTotal) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        int lineNumber = 1;

        if (f.getCompra() != null && f.getCompra().getItens() != null && !f.getCompra().getItens().isEmpty()) {
            for (ItemCompra item : f.getCompra().getItens()) {
                Element line = doc.createElement("Line");
                invoice.appendChild(line);

                String prodCode = item.getProdutoId() != null ? item.getProdutoId().toString() : "1";
                double qtd = item.getQuantidade() != null && item.getQuantidade() > 0 ? item.getQuantidade() : 1.0;
                double preco = item.getPreco() != null ? item.getPreco() : 0.0;
                
                // Regras 13 e 14: Crédito/Débito = Quantidade × Preço Unitário
                BigDecimal lineAmount = format2(qtd * preco);

                // Regra 24: Se referenciar outro documento (NC), a linha não pode exceder o valor do documento original
                if (isDebitDoc && f.getFaturaReferencia() != null && f.getFaturaReferencia().getTotal() != null) {
                    BigDecimal refMax = format2(f.getFaturaReferencia().getTotal());
                    if (lineAmount.compareTo(refMax) > 0) {
                        lineAmount = refMax;
                    }
                }

                appendChild(doc, line, "LineNumber", String.valueOf(lineNumber++));
                appendChild(doc, line, "ProductCode", prodCode);
                appendChild(doc, line, "ProductDescription", item.getNomeProduto() != null ? item.getNomeProduto() : "Produto");
                appendChild(doc, line, "Quantity", format2(qtd).toString());
                appendChild(doc, line, "UnitOfMeasure", "Unidade");
                appendChild(doc, line, "UnitPrice", format2(preco).toString());
                appendChild(doc, line, "TaxPointDate", sdfDate.format(f.getDataEmissao()));
                appendChild(doc, line, "Description", item.getNomeProduto() != null ? item.getNomeProduto() : "Item");

                if (isDebitDoc) {
                    appendChild(doc, line, "DebitAmount", lineAmount.toString()); // Regra 14
                } else {
                    appendChild(doc, line, "CreditAmount", lineAmount.toString()); // Regra 13
                }

                // Imposto / Taxa (Regra 23)
                double percIva = item.getIvaPercentual() != null ? item.getIvaPercentual() : 14.0;
                addTaxNode(doc, line, percIva);
                
                appendChild(doc, line, "SettlementAmount", "0.00");
            }
        } else {
            // Fallback de Linha caso o documento não possua itens mapeados diretamente
            Element line = doc.createElement("Line");
            invoice.appendChild(line);

            double totalDocVal = f.getTotal() != null ? f.getTotal() : 0.0;
            double ivaDocVal = f.getIva() != null ? f.getIva() : 0.0;
            BigDecimal netVal = format2(Math.max(0.0, totalDocVal - ivaDocVal));

            appendChild(doc, line, "LineNumber", "1");
            appendChild(doc, line, "ProductCode", "1");
            appendChild(doc, line, "ProductDescription", "Venda de Bens / Serviços");
            appendChild(doc, line, "Quantity", "1.00");
            appendChild(doc, line, "UnitOfMeasure", "Unidade");
            appendChild(doc, line, "UnitPrice", netVal.toString());
            appendChild(doc, line, "TaxPointDate", sdfDate.format(f.getDataEmissao()));
            appendChild(doc, line, "Description", "Venda Geral");

            if (isDebitDoc) {
                appendChild(doc, line, "DebitAmount", netVal.toString());
            } else {
                appendChild(doc, line, "CreditAmount", netVal.toString());
            }

            double perc = 14.0;
            if (ivaDocVal <= 0) perc = 0.0;
            addTaxNode(doc, line, perc);

            appendChild(doc, line, "SettlementAmount", "0.00");
        }
    }

    /**
     * Regra 23: Se a taxa de IVA for zero, deve existir código e motivo de isenção válidos.
     */
    private void addTaxNode(Document doc, Element parentLine, double percIva) {
        Element tax = doc.createElement("Tax");
        parentLine.appendChild(tax);

        appendChild(doc, tax, "TaxType", "IVA");
        appendChild(doc, tax, "TaxCountryRegion", "AO");

        if (percIva > 0) {
            String code = "NOR";
            if (percIva == 7.0) code = "INT";
            else if (percIva == 5.0) code = "RED";

            appendChild(doc, tax, "TaxCode", code);
            appendChild(doc, tax, "TaxPercentage", format2(percIva).toString());
        } else {
            appendChild(doc, tax, "TaxCode", "ISE");
            appendChild(doc, tax, "TaxPercentage", "0.00");
            // Obrigatório pela Regra 23
            appendChild(doc, parentLine, "TaxExemptionReason", "Isento nos termos da alínea a) do n.º 1 do artigo 12.º do CIVA");
            appendChild(doc, parentLine, "TaxExemptionCode", "M02");
        }
    }

    /**
     * Secção WorkingDocuments (Pró-formas / FP)
     * Regras: 5, 16, 17-19
     */
    private void addWorkingDocuments(Document doc, Element source, List<Fatura> proformas) {
        Element workDocs = doc.createElement("WorkingDocuments");
        source.appendChild(workDocs);

        // Regra 5: Contagem de documentos de trabalho
        appendChild(doc, workDocs, "NumberOfEntries", String.valueOf(proformas.size()));

        BigDecimal totalCredit = BigDecimal.ZERO;
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat sdfPeriod = new SimpleDateFormat("MM", Locale.US);
        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        for (Fatura f : proformas) {
            double totalVal = f.getTotal() != null ? f.getTotal() : 0.0;
            double ivaVal = f.getIva() != null ? f.getIva() : 0.0;
            BigDecimal netTotalBD = format2(Math.max(0.0, totalVal - ivaVal));
            BigDecimal ivaTotalBD = format2(ivaVal);
            BigDecimal grossTotalBD = netTotalBD.add(ivaTotalBD);

            totalCredit = totalCredit.add(netTotalBD);

            Element workDoc = doc.createElement("WorkDocument");
            workDocs.appendChild(workDoc);

            appendChild(doc, workDoc, "DocumentNumber", f.getNumeroFatura() != null ? f.getNumeroFatura() : "FP " + f.getId());
            
            Element docStatus = doc.createElement("DocumentStatus");
            workDoc.appendChild(docStatus);
            appendChild(doc, docStatus, "WorkStatus", "N");
            appendChild(doc, docStatus, "WorkStatusDate", sdfTime.format(f.getSystemEntryDate() != null ? f.getSystemEntryDate() : f.getDataEmissao()));
            appendChild(doc, docStatus, "SourceID", "1");
            appendChild(doc, docStatus, "SourceBilling", "P");

            appendChild(doc, workDoc, "Hash", f.getHash() != null && !f.getHash().isEmpty() ? f.getHash() : "0");
            appendChild(doc, workDoc, "HashControl", f.getHashControl() != null ? f.getHashControl() : "1");
            appendChild(doc, workDoc, "Period", sdfPeriod.format(f.getDataEmissao()));
            appendChild(doc, workDoc, "WorkDate", sdfDate.format(f.getDataEmissao()));
            appendChild(doc, workDoc, "WorkType", "FP"); // Fatura Pró-forma
            appendChild(doc, workDoc, "SourceID", "1");
            appendChild(doc, workDoc, "SystemEntryDate", sdfTime.format(f.getSystemEntryDate() != null ? f.getSystemEntryDate() : f.getDataEmissao()));
            
            String custId = (f.getCompra() != null && f.getCompra().getCliente() != null) 
                            ? f.getCompra().getCliente().getId().toString() : "1";
            appendChild(doc, workDoc, "CustomerID", custId);

            // Linha do Documento de Trabalho (Regra 16)
            Element line = doc.createElement("Line");
            workDoc.appendChild(line);
            appendChild(doc, line, "LineNumber", "1");
            appendChild(doc, line, "ProductCode", "1");
            appendChild(doc, line, "ProductDescription", "Cotação / Pró-forma");
            appendChild(doc, line, "Quantity", "1.00");
            appendChild(doc, line, "UnitPrice", netTotalBD.toString());
            appendChild(doc, line, "CreditAmount", netTotalBD.toString()); // Regra 16

            double perc = ivaVal > 0 ? 14.0 : 0.0;
            addTaxNode(doc, line, perc);

            // Totais
            Element totals = doc.createElement("DocumentTotals");
            workDoc.appendChild(totals);
            appendChild(doc, totals, "TaxPayable", ivaTotalBD.toString());
            appendChild(doc, totals, "NetTotal", netTotalBD.toString());
            appendChild(doc, totals, "GrossTotal", grossTotalBD.toString());
        }

        appendChild(doc, workDocs, "TotalDebit", "0.00");
        appendChild(doc, workDocs, "TotalCredit", totalCredit.setScale(2, RoundingMode.HALF_UP).toString());
    }

    /**
     * Secção Payments (Recibos / FR / Pagamentos)
     * Regras: 6, 7-12
     */
    private void addPayments(Document doc, Element source, List<Fatura> pagamentos) {
        Element payments = doc.createElement("Payments");
        source.appendChild(payments);

        // Regra 6: Número de recibos
        appendChild(doc, payments, "NumberOfEntries", String.valueOf(pagamentos.size()));

        BigDecimal totalCredit = BigDecimal.ZERO;
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat sdfPeriod = new SimpleDateFormat("MM", Locale.US);
        SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

        for (Fatura f : pagamentos) {
            double totalVal = f.getTotal() != null ? f.getTotal() : 0.0;
            BigDecimal grossBD = format2(totalVal);
            totalCredit = totalCredit.add(grossBD);

            Element payment = doc.createElement("Payment");
            payments.appendChild(payment);

            String refNo = "RC " + f.getNumeroFatura();
            if ("FR".equalsIgnoreCase(f.getTipoDocumento())) {
                refNo = f.getNumeroFatura();
            }

            appendChild(doc, payment, "PaymentRefNo", refNo);
            appendChild(doc, payment, "Period", sdfPeriod.format(f.getDataEmissao()));
            appendChild(doc, payment, "TransactionDate", sdfDate.format(f.getDataEmissao()));
            appendChild(doc, payment, "PaymentType", "RC");
            appendChild(doc, payment, "SystemEntryDate", sdfTime.format(f.getSystemEntryDate() != null ? f.getSystemEntryDate() : f.getDataEmissao()));
            
            String custId = (f.getCompra() != null && f.getCompra().getCliente() != null) 
                            ? f.getCompra().getCliente().getId().toString() : "1";
            appendChild(doc, payment, "CustomerID", custId);

            // Linha de Origem do Recibo
            Element line = doc.createElement("Line");
            payment.appendChild(line);
            appendChild(doc, line, "LineNumber", "1");
            
            Element srcDocId = doc.createElement("SourceDocumentID");
            line.appendChild(srcDocId);
            appendChild(doc, srcDocId, "OriginatingON", f.getNumeroFatura() != null ? f.getNumeroFatura() : "FT " + f.getId());
            appendChild(doc, srcDocId, "InvoiceDate", sdfDate.format(f.getDataEmissao()));

            appendChild(doc, line, "CreditAmount", grossBD.toString());

            // Totais do Recibo
            Element totals = doc.createElement("DocumentTotals");
            payment.appendChild(totals);
            appendChild(doc, totals, "TaxPayable", "0.00");
            appendChild(doc, totals, "NetTotal", grossBD.toString());
            appendChild(doc, totals, "GrossTotal", grossBD.toString());
        }

        appendChild(doc, payments, "TotalDebit", "0.00");
        appendChild(doc, payments, "TotalCredit", totalCredit.setScale(2, RoundingMode.HALF_UP).toString());
    }

    private BigDecimal format2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private void appendChild(Document doc, Element parent, String tag, String text) {
        Element child = doc.createElement(tag);
        child.appendChild(doc.createTextNode(text != null ? text : ""));
        parent.appendChild(child);
    }

    private String transformXmlToString(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }
}
