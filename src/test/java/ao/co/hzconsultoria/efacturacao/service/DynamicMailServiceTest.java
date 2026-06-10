package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DynamicMailServiceTest {

    @Autowired
    private DynamicMailService dynamicMailService;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ConfiguracaoEmpresaRepository configuracaoEmpresaRepository;

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    private Empresa empresa;

    @BeforeEach
    void setUp() {
        // Criar empresa de teste
        empresa = new Empresa();
        empresa.setNome("Empresa de Teste");
        empresa.setNif("999999999");
        empresa.setEndereco("Endereço de Teste");
        empresa.setEmail("empresa@test.com");
        empresa.setTelefone("+244900000000");
        empresa = empresaRepository.save(empresa);

        // Garantir que existe a configuração do sistema ID 1
        if (!configuracaoSistemaRepository.existsById(1L)) {
            ConfiguracaoSistemaEntity sysConfig = new ConfiguracaoSistemaEntity();
            sysConfig.setId(1L);
            sysConfig.setEmailSmtpHost("smtp.sistema.com");
            sysConfig.setEmailSmtpPorta(587);
            sysConfig.setEmailSmtpUsername("sistema@test.com");
            sysConfig.setEmailSmtpPassword("sistemaPass");
            sysConfig.setEmailSegurancaTipo("TLS");
            sysConfig.setEmailRemetente("suporte@sistema.com");
            sysConfig.setEmailNomeRemetente("Suporte Sistema");
            configuracaoSistemaRepository.save(sysConfig);
        } else {
            ConfiguracaoSistemaEntity sysConfig = configuracaoSistemaRepository.findById(1L).get();
            sysConfig.setEmailSmtpHost("smtp.sistema.com");
            sysConfig.setEmailSmtpPorta(587);
            sysConfig.setEmailSmtpUsername("sistema@test.com");
            sysConfig.setEmailSmtpPassword("sistemaPass");
            sysConfig.setEmailSegurancaTipo("TLS");
            sysConfig.setEmailRemetente("suporte@sistema.com");
            sysConfig.setEmailNomeRemetente("Suporte Sistema");
            configuracaoSistemaRepository.save(sysConfig);
        }
    }

    @Test
    void testFallbackParaConfiguracaoSistema() {
        // Se a empresa não tem configuração de email específica criada/habilitada,
        // deve usar as definições globais do sistema.
        JavaMailSenderImpl sender = dynamicMailService.buildSender(empresa.getId());
        assertEquals("smtp.sistema.com", sender.getHost());
        assertEquals("sistema@test.com", sender.getUsername());
    }

    @Test
    void testConfiguracaoEmpresaEspecifica() {
        // Criar e salvar configuração específica para a empresa
        ConfiguracaoEmpresa configEmpresa = new ConfiguracaoEmpresa();
        configEmpresa.setEmpresa(empresa);
        configEmpresa.setEmailSmtpHost("smtp.empresa.com");
        configEmpresa.setEmailSmtpPorta(465);
        configEmpresa.setEmailSmtpUsername("user@empresa.com");
        configEmpresa.setEmailSmtpPassword("passEmpresa");
        configEmpresa.setEmailSegurancaTipo("SSL");
        configEmpresa.setEmailHabilitado(true);
        configuracaoEmpresaRepository.save(configEmpresa);

        // Deve carregar as configurações SMTP da empresa e não as do sistema
        JavaMailSenderImpl sender = dynamicMailService.buildSender(empresa.getId());
        assertEquals("smtp.empresa.com", sender.getHost());
        assertEquals(465, sender.getPort());
        assertEquals("user@empresa.com", sender.getUsername());
    }

    @Test
    void testIsEmailConfigurado() {
        // Inicialmente, se a empresa não tiver configuração específica mas o sistema tiver,
        // deve retornar true (porque há o fallback do sistema)
        assertTrue(dynamicMailService.isEmailConfigurado(empresa.getId()));

        // Se removermos as configurações do sistema para testar o isolamento
        ConfiguracaoSistemaEntity sysConfig = configuracaoSistemaRepository.findById(1L).get();
        sysConfig.setEmailSmtpHost("");
        sysConfig.setEmailSmtpUsername("");
        configuracaoSistemaRepository.save(sysConfig);

        // Agora não deve estar configurado
        assertFalse(dynamicMailService.isEmailConfigurado(empresa.getId()));

        // Habilitando na empresa
        ConfiguracaoEmpresa configEmpresa = new ConfiguracaoEmpresa();
        configEmpresa.setEmpresa(empresa);
        configEmpresa.setEmailSmtpHost("smtp.empresa.com");
        configEmpresa.setEmailSmtpPorta(587);
        configEmpresa.setEmailSmtpUsername("user@empresa.com");
        configEmpresa.setEmailSmtpPassword("pass");
        configEmpresa.setEmailHabilitado(true);
        configuracaoEmpresaRepository.save(configEmpresa);

        // Deve retornar true
        assertTrue(dynamicMailService.isEmailConfigurado(empresa.getId()));
    }
}
