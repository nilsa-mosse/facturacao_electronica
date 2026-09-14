package ao.co.hzconsultoria.efacturacao.config;

import ao.co.hzconsultoria.efacturacao.security.AcessoModuloInterceptor;
import ao.co.hzconsultoria.efacturacao.security.LicencaInterceptor;
import ao.co.hzconsultoria.efacturacao.security.ForcePasswordChangeInterceptor;

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

    @Autowired
    private ForcePasswordChangeInterceptor forcePasswordChangeInterceptor;

    @Value("${app.upload.logo.dir:./uploads/logo/}")
    private String logoUploadDir;

    @Value("${app.upload.dir:./uploads/produtos/}")
    private String produtosUploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licencaInterceptor)
                .excludePathPatterns("/uploads/**", "/assets/**", "/plugins/**", "/css/**", "/js/**", "/images/**", "/img/**");
        registry.addInterceptor(acessoModuloInterceptor)
                .excludePathPatterns("/uploads/**", "/assets/**", "/plugins/**", "/css/**", "/js/**", "/images/**", "/img/**");
        registry.addInterceptor(forcePasswordChangeInterceptor)
                .excludePathPatterns("/uploads/**", "/assets/**", "/plugins/**", "/css/**", "/js/**", "/images/**", "/img/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve o caminho absoluto da pasta raiz de uploads
        // A URL /uploads/** cobre tanto /uploads/logo/ como /uploads/produtos/ e /uploads/faturas/
        String uploadsRoot = Paths.get(logoUploadDir)
                .toAbsolutePath().normalize().getParent().toString()
                .replace("\\", "/");
        if (!uploadsRoot.endsWith("/"))
            uploadsRoot += "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "file:" + uploadsRoot,
                        "file:///" + uploadsRoot,
                        "file:./uploads/",
                        "file:uploads/"
                );
    }
}
