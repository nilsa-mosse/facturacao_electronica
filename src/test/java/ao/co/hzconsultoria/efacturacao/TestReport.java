package ao.co.hzconsultoria.efacturacao;

import ao.co.hzconsultoria.efacturacao.service.ReportService;

//import java.util.Collections;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;

public class TestReport {
    public static void main(String[] args) throws Exception 
    {
        // Ajuste o caminho e o nome do arquivo conforme seu projeto
		String caminho ="C:\\Users\\sebastiao.mosse\\JaspersoftWorkspace\\MyReports\\";
		String fileName = "Factura_POS";
        
        // Obtenha o contexto Spring para injetar o ReportService
        // Start SpringApplication but disable DataSource and JPA auto-configuration so we don't need a DB for this test
        SpringApplication app = new SpringApplication(EfaturacaoApplication.class);
        //app.setDefaultProperties(Collections.singletonMap("spring.autoconfigure.exclude",
            //    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration"));
        ConfigurableApplicationContext ctx = app.run();
        ReportService reportService = ctx.getBean(ReportService.class);
        reportService.gerarFactura(caminho, fileName);
        System.out.println("Relatório gerado com sucesso!");
        ctx.close();
    }
    
    
}