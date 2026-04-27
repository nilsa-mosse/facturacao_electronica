package ao.co.hzconsultoria.efacturacao.service;

import org.springframework.stereotype.Service;
import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Service
public class SaftService {

    private final FaturaRepository faturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final EmpresaRepository empresaRepository;

    public SaftService(FaturaRepository faturaRepository, ClienteRepository clienteRepository, 
                        ProdutoRepository produtoRepository, EmpresaRepository empresaRepository) {
        this.faturaRepository = faturaRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.empresaRepository = empresaRepository;
    }

    public String generateSaftXml(Date startDate, Date endDate) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Root Element: AuditFile
        Element rootElement = doc.createElement("AuditFile");
        rootElement.setAttribute("xmlns", "urn:OECD:StandardAuditFile-Tax:AO:1.01_01");
        doc.appendChild(rootElement);

        // Header
        addHeader(doc, rootElement, startDate, endDate);

        // MasterFiles
        Element masterFiles = doc.createElement("MasterFiles");
        rootElement.appendChild(masterFiles);
        addCustomers(doc, masterFiles);
        addProducts(doc, masterFiles);
        addTaxTable(doc, masterFiles);

        // SourceDocuments
        Element sourceDocs = doc.createElement("SourceDocuments");
        rootElement.appendChild(sourceDocs);
        addSalesInvoices(doc, sourceDocs, startDate, endDate);

        // Transform to String
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        appendChild(doc, header, "AuditFileVersion", "1.01_01");
        appendChild(doc, header, "CompanyID", emp.getNif());
        appendChild(doc, header, "TaxRegistrationNumber", emp.getNif());
        appendChild(doc, header, "TaxAccountingBasis", "F");
        appendChild(doc, header, "CompanyName", emp.getNome());
        appendChild(doc, header, "BusinessName", emp.getNome());
        
        Element address = doc.createElement("CompanyAddress");
        header.appendChild(address);
        appendChild(doc, address, "AddressDetail", emp.getEndereco());
        appendChild(doc, address, "City", "Luanda");
        appendChild(doc, address, "Country", "AO");

        appendChild(doc, header, "FiscalYear", new SimpleDateFormat("yyyy").format(start));
        appendChild(doc, header, "StartDate", sdf.format(start));
        appendChild(doc, header, "EndDate", sdf.format(end));
        appendChild(doc, header, "CurrencyCode", "AOA");
        appendChild(doc, header, "DateCreated", sdf.format(new Date()));
        appendChild(doc, header, "TaxEntity", "Global");
        appendChild(doc, header, "ProductCompanyId", "HZ Consultoria");
        appendChild(doc, header, "SoftwareCertificateNumber", "0");
    }

    private void addCustomers(Document doc, Element master) {
        List<Cliente> clientes = clienteRepository.findAll();
        for (Cliente c : clientes) {
            Element customer = doc.createElement("Customer");
            master.appendChild(customer);
            appendChild(doc, customer, "CustomerID", c.getId().toString());
            appendChild(doc, customer, "CustomerTaxID", c.getNif());
            appendChild(doc, customer, "AccountID", "Desconhecido");
            appendChild(doc, customer, "CompanyName", c.getNome());
            
            Element billingAddress = doc.createElement("BillingAddress");
            customer.appendChild(billingAddress);
            appendChild(doc, billingAddress, "AddressDetail", c.getEndereco());
            appendChild(doc, billingAddress, "City", "Luanda");
            appendChild(doc, billingAddress, "Country", "AO");
            
            appendChild(doc, customer, "SelfBillingIndicator", "0");
        }
    }

    private void addProducts(Document doc, Element master) {
        List<Produto> produtos = produtoRepository.findAll();
        for (Produto p : produtos) {
            Element product = doc.createElement("Product");
            master.appendChild(product);
            appendChild(doc, product, "ProductCode", p.getId().toString());
            appendChild(doc, product, "ProductDescription", p.getNome());
            appendChild(doc, product, "ProductNumberCode", p.getCodigoBarra() != null ? p.getCodigoBarra() : p.getId().toString());
        }
    }

    private void addTaxTable(Document doc, Element master) {
        Element taxTable = doc.createElement("TaxTable");
        master.appendChild(taxTable);

        Element taxEntry = doc.createElement("TaxTableEntry");
        taxTable.appendChild(taxEntry);
        appendChild(doc, taxEntry, "TaxType", "IVA");
        appendChild(doc, taxEntry, "TaxCode", "NOR");
        appendChild(doc, taxEntry, "Description", "Taxa Normal");
        appendChild(doc, taxEntry, "TaxPercentage", "14.00");
    }

    private void addSalesInvoices(Document doc, Element source, Date start, Date end) {
        List<Fatura> faturas = faturaRepository.findAll(); // Should filter by date in real scenario
        
        Element salesInv = doc.createElement("SalesInvoices");
        source.appendChild(salesInv);
        appendChild(doc, salesInv, "NumberOfEntries", String.valueOf(faturas.size()));
        
        double totalDebit = 0;
        double totalCredit = 0;
        
        for (Fatura f : faturas) {
            double totalFatura = f.getTotal() != null ? f.getTotal() : 0.0;
            double ivaFatura = f.getIva() != null ? f.getIva() : 0.0;
            totalCredit += totalFatura;
            
            Element invoice = doc.createElement("Invoice");
            salesInv.appendChild(invoice);
            appendChild(doc, invoice, "InvoiceNo", f.getNumeroFatura());
            appendChild(doc, invoice, "DocumentStatus", "N"); // Normal
            appendChild(doc, invoice, "Hash", f.getHash() != null ? f.getHash() : "0");
            appendChild(doc, invoice, "HashControl", "1");
            appendChild(doc, invoice, "Period", new SimpleDateFormat("MM").format(f.getDataEmissao()));
            appendChild(doc, invoice, "InvoiceDate", new SimpleDateFormat("yyyy-MM-dd").format(f.getDataEmissao()));
            appendChild(doc, invoice, "InvoiceType", "FT");
            appendChild(doc, invoice, "SelfBillingIndicator", "0");
            appendChild(doc, invoice, "SystemEntryDate", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(f.getDataEmissao()));
            appendChild(doc, invoice, "CustomerID", (f.getCompra() != null && f.getCompra().getCliente() != null) ? f.getCompra().getCliente().getId().toString() : "1");

            // Simplified lines and totals
            Element totals = doc.createElement("DocumentTotals");
            invoice.appendChild(totals);
            appendChild(doc, totals, "TaxPayable", String.format("%.2f", ivaFatura).replace(",", "."));
            appendChild(doc, totals, "NetTotal", String.format("%.2f", totalFatura - ivaFatura).replace(",", "."));
            appendChild(doc, totals, "GrossTotal", String.format("%.2f", totalFatura).replace(",", "."));
        }
        
        appendChild(doc, salesInv, "TotalDebit", "0.00");
        appendChild(doc, salesInv, "TotalCredit", String.format("%.2f", totalCredit).replace(",", "."));
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
