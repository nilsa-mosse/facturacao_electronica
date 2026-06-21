package ao.co.hzconsultoria.efacturacao.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private CustomAuthenticationFailureHandler failureHandler;

    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .authorizeRequests()

                // === Recursos Públicos (Sem Autenticação) ===
                .antMatchers(
                    "/login",
                    "/forgot-password", "/reset-password",
                    "/alterar-senha-obrigatorio",
                    "/licenca-expirada", "/ativar-licenca",
                    "/manifest.json", "/sw.js",
                    "/img/**", "/uploads/**", "/assets/**", "/plugins/**",
                    "/css/**", "/js/**", "/images/**"
                ).permitAll()

                // === Área Exclusiva do SuperAdmin ===
                .antMatchers("/superadmin/**").hasRole("SUPERADMIN")

                // === APIs Administrativas (Apenas ADMIN e SUPERADMIN) ===
                .antMatchers(
                    "/api/limpeza/**",
                    "/api/saft/**",
                    "/api/inventario/ajuste/**",
                    "/configuracao/**"
                ).hasAnyRole("ADMIN", "SUPERADMIN")

                // === APIs do POS/Vendas (Qualquer utilizador autenticado) ===
                .antMatchers(
                    "/api/compras/**",
                    "/api/vendas-suspensas/**",
                    "/api/estoque/**",
                    "/api/categorias/**",
                    "/finalizarVenda"
                ).authenticated()

                // === Tudo o resto exige autenticação ===
                .anyRequest().authenticated()

            .and()
            .csrf()
                // Endpoints do POS que podem ser chamados por hardware externo (TPA físico)
                .ignoringAntMatchers(
                    "/api/compras", "/api/compras/single",
                    "/api/compras/proforma", "/api/compras/guia",
                    "/finalizarVenda",
                    "/api/vendas-suspensas/**",
                    "/ativar-licenca",
                    "/clientes/api/adicionar"
                )
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

            .and()
            .formLogin()
                .loginPage("/login")
                .failureHandler(failureHandler)
                .successHandler(successHandler)
                .permitAll()

            .and()
            .logout()
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()

            .and()
            .headers()
                .frameOptions().sameOrigin()
                .xssProtection().block(true)
                .and()
                .contentTypeOptions()
                .and()
                .referrerPolicy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                .and()
                .cacheControl();

        return http.build();
    }
}