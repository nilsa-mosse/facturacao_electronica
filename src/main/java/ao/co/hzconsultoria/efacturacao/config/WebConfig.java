package ao.co.hzconsultoria.efacturacao.config;

import ao.co.hzconsultoria.efacturacao.security.AcessoModuloInterceptor;
import ao.co.hzconsultoria.efacturacao.security.LicencaInterceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AcessoModuloInterceptor acessoModuloInterceptor;

    @Autowired
    private LicencaInterceptor licencaInterceptor;

    @Value("${app.upload.logo.dir:./uploads/logo/}")
    private String logoUploadDir;

    @Value("${app.upload.dir:./uploads/produtos/}")
    private String produtosUploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licencaInterceptor);
        registry.addInterceptor(acessoModuloInterceptor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve o caminho absoluto da pasta raiz de uploads
        // A URL /uploads/** cobre tanto /uploads/logo/ como /uploads/produtos/
        String uploadsRoot = Paths.get(logoUploadDir)
                .toAbsolutePath().normalize().getParent().toString()
                .replace("\\", "/");
        if (!uploadsRoot.endsWith("/"))
            uploadsRoot += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadsRoot);
    }
}
