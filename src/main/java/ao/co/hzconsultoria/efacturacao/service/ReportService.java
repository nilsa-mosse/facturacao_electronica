package ao.co.hzconsultoria.efacturacao.service;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.ReportRepository;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class ReportService 
{
	@Autowired
	private ReportRepository repository;

	public void gerarFactura(String caminho, String fileName) throws ClassNotFoundException, IOException, SQLException, JRException 
	{
		List<ItemCompra> dataList = repository.findAll();
		
		System.out.println("Resultado: "+ dataList.get(0).getNomeProduto());
		System.out.println("Resultado: "+ dataList.get(1).getNomeProduto());
		System.out.println("Resultado: "+ dataList.get(2).getNomeProduto());
		
		for(int i=0; i< dataList.size(); i++) 
		{
			ItemCompra dados = new ItemCompra();
			dados.setId( dataList.get(i).getId());
			dados.setNomeProduto( dataList.get(i).getNomeProduto());
			dados.setQuantidade( dataList.get(i).getQuantidade());
			dados.setPreco( dataList.get(i).getPreco());
			dados.setIva( dataList.get(i).getIva());
			dados.setSubtotal( dataList.get(i).getSubtotal());	
		}
		/*
		  List<ItemCompra> sample = new ArrayList<>();
		  ItemCompra d1 = new ItemCompra();
	        d1.setId(1L);
	        d1.setNomeProduto("Produto A");
	        d1.setQuantidade(2);
	        d1.setPreco(100.00);
	        d1.setIva(14.00);
	        d1.setSubtotal(200.00);
	        sample.add(d1);

	        ItemCompra d2 = new ItemCompra();
	        d1.setId(1L);
	        d1.setNomeProduto("Produto A");
	        d1.setQuantidade(2);
	        d1.setPreco(100.00);
	        d1.setIva(14.00);
	        d1.setSubtotal(200.00);
	        sample.add(d2);
	        */
	        
		
		JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(dataList);
		//JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(sample);
		Map<String, Object> parameters = new HashMap<String, Object>();
		
		try {
			//parameters.put("logo", caminho + "logo_coopera.jpg");
			JasperReport jasperReport = JasperCompileManager.compileReport(caminho + fileName +".jrxml" );
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, beanColDataSource);
			JasperExportManager.exportReportToPdfFile(jasperPrint, caminho + fileName +".pdf");
		} catch (JRException e) {
			e.printStackTrace();
		}
	}

	public String getCurrentDateTime() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy");
		LocalDateTime now = LocalDateTime.now();
		String dataFormatada = dtf.format(now);
		return dataFormatada;
	}
}