package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para ConfiguracaoEmpresaService
 * 
 * Testes unitários para validar:
 * - Criação de configurações padrão para nova empresa
 * - Isolamento de configurações entre empresas
 * - Atualização de diferentes tipos de configuração
 * - Validação de configurações
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConfiguracaoEmpresaServiceTest {

    @Autowired
    private ConfiguracaoEmpresaService configuracaoEmpresaService;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ConfiguracaoEmpresaRepository configuracaoRepository;

    private Empresa empresa1;
    private Empresa empresa2;

    @BeforeEach
    void setUp() {
        // Criar empresas de teste
        empresa1 = new Empresa();
        empresa1.setNome("Empresa 1");
        empresa1.setNif("999999991");
        empresa1.setEndereco("Endereço 1");
        empresa1.setEmail("empresa1@test.com");
        empresa1.setTelefone("+244912345678");
        empresa1 = empresaRepository.save(empresa1);

        empresa2 = new Empresa();
        empresa2.setNome("Empresa 2");
        empresa2.setNif("999999992");
        empresa2.setEndereco("Endereço 2");
        empresa2.setEmail("empresa2@test.com");
        empresa2.setTelefone("+244987654321");
        empresa2 = empresaRepository.save(empresa2);
    }

    @Test
    void testObtenerConfiguracaoPadrao() {
        // Quando obter configuração de empresa sem configuração existente
        ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());

        // Então deve criar com valores padrão
        assertNotNull(config);
        assertNotNull(config.getId());
        assertEquals(empresa1.getId(), config.getEmpresa().getId());
        assertEquals("smtp.gmail.com", config.getEmailSmtpHost());
        assertEquals(587, config.getEmailSmtpPorta());
        assertEquals("TLS", config.getEmailSegurancaTipo());
        assertEquals("LOCAL", config.getStorageTipo());
        assertTrue(config.isSegRequireUppercase());
        assertTrue(config.isSegRequireNumbers());
        assertFalse(config.isSegRequireSpecialChars());
    }

    @Test
    void testIsolamentoConfiguracao() {
        // Quando obter configurações de duas empresas
        ConfiguracaoEmpresa config1 = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());
        ConfiguracaoEmpresa config2 = configuracaoEmpresaService.obterConfiguracao(empresa2.getId());

        // Então devem ter IDs diferentes
        assertNotEquals(config1.getId(), config2.getId());
        assertEquals(empresa1.getId(), config1.getEmpresa().getId());
        assertEquals(empresa2.getId(), config2.getEmpresa().getId());

        // E modificar uma não afeta a outra
        configuracaoEmpresaService.atualizarConfiguracaoEmail(
            empresa1.getId(), 
            "smtp.empresa1.com", 587, "user1@emp1.com", "pass1", "TLS", 
            "noreply@empresa1.com", "Empresa 1"
        );

        ConfiguracaoEmpresa config1Updated = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());
        ConfiguracaoEmpresa config2Unchanged = configuracaoEmpresaService.obterConfiguracao(empresa2.getId());

        assertEquals("smtp.empresa1.com", config1Updated.getEmailSmtpHost());
        assertEquals("smtp.gmail.com", config2Unchanged.getEmailSmtpHost());
    }

    @Test
    void testAtualizarConfiguracaoEmail() {
        ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());

        // Quando atualizar configuração de email
        configuracaoEmpresaService.atualizarConfiguracaoEmail(
            empresa1.getId(),
            "smtp.empresa.com",
            465,
            "usuario@empresa.com",
            "senhaSegura123",
            "SSL",
            "noreply@empresa.com",
            "Empresa Teste"
        );

        // Então os dados devem ser salvos
        ConfiguracaoEmpresa updated = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());
        assertEquals("smtp.empresa.com", updated.getEmailSmtpHost());
        assertEquals(465, updated.getEmailSmtpPorta());
        assertEquals("usuario@empresa.com", updated.getEmailSmtpUsername());
        assertEquals("senhaSegura123", updated.getEmailSmtpPassword());
        assertEquals("SSL", updated.getEmailSegurancaTipo());
        assertEquals("noreply@empresa.com", updated.getEmailRemetente());
        assertEquals("Empresa Teste", updated.getEmailNomeRemetente());
        assertTrue(updated.isEmailHabilitado());
    }

    @Test
    void testAtualizarConfiguracaoStorage() {
        // Quando atualizar configuração de storage
        configuracaoEmpresaService.atualizarConfiguracaoStorage(
            empresa1.getId(),
            "S3",
            "s3://meu-bucket/uploads/",
            50,
            100
        );

        // Então os dados devem ser salvos
        ConfiguracaoEmpresa updated = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());
        assertEquals("S3", updated.getStorageTipo());
        assertEquals("s3://meu-bucket/uploads/", updated.getStorageCaminhoBase());
        assertEquals(50, updated.getStorageTamanhoMaxFicheiro());
        assertEquals(100, updated.getStorageTamanhoMaxRequest());
    }

    @Test
    void testAtualizarPoliticaSeguranca() {
        // Quando atualizar política de segurança
        configuracaoEmpresaService.atualizarPoliticaSeguranca(
            empresa1.getId(),
            60,  // tempoExpiracaoSessao em minutos
            true,  // twoFactorAtivo
            false,  // requireUppercase
            true,  // requireNumbers
            true,  // requireSpecialChars
            12  // comprimentoMinPassword
        );

        // Então os dados devem ser salvos
        ConfiguracaoEmpresa updated = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());
        assertEquals(60, updated.getSegTempoExpiracaoSessao());
        assertTrue(updated.isSegTwoFactorAtivo());
        assertFalse(updated.isSegRequireUppercase());
        assertTrue(updated.isSegRequireNumbers());
        assertTrue(updated.isSegRequireSpecialChars());
        assertEquals(12, updated.getSegComprimentoMinPassword());
    }

    @Test
    void testTemEmailConfigurado() {
        // Quando empresa não tem email configurado
        assertFalse(configuracaoEmpresaService.temEmailConfigurado(empresa1.getId()));

        // Depois de configurar
        configuracaoEmpresaService.atualizarConfiguracaoEmail(
            empresa1.getId(),
            "smtp.empresa.com", 587, "user@empresa.com", "pass", "TLS",
            "noreply@empresa.com", "Empresa"
        );

        // Então deve retornar true
        assertTrue(configuracaoEmpresaService.temEmailConfigurado(empresa1.getId()));
    }

    @Test
    void testTemAgtConfigurada() {
        // Quando empresa não tem AGT configurada
        assertFalse(configuracaoEmpresaService.temAgtConfigurada(empresa1.getId()));

        // Depois de configurar
        configuracaoEmpresaService.atualizarConfiguracaoAGT(
            empresa1.getId(),
            true,
            "https://api.agt.gov.ao/v1",
            "usuario_agt",
            "senha_agt",
            "caminho/certificado.pem"
        );

        // Então deve retornar true
        assertTrue(configuracaoEmpresaService.temAgtConfigurada(empresa1.getId()));
    }

    @Test
    void testAtualizarConfiguracaoAGT() {
        // Quando atualizar configuração de AGT
        configuracaoEmpresaService.atualizarConfiguracaoAGT(
            empresa1.getId(),
            true,
            "https://api.agt.gov.ao/v1",
            "usuario_agt",
            "senha_agt",
            "caminho/certificado.pem"
        );

        // Então os dados devem ser salvos
        ConfiguracaoEmpresa updated = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());
        assertTrue(updated.isAgtIntegracaoHabilitada());
        assertEquals("https://api.agt.gov.ao/v1", updated.getAgtUrlServico());
        assertEquals("usuario_agt", updated.getAgtUsuario());
        assertEquals("senha_agt", updated.getAgtSenha());
        assertEquals("caminho/certificado.pem", updated.getAgtCertificado());
    }

    @Test
    void testSalvarConfiguracao() {
        ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresa1.getId());
        
        // Quando modificar propriedades e salvar
        config.setUsarLogotipoEmDocumentos(false);
        config.setUsarRodapéPersonalizadoEmDocumentos(true);
        config.setRodapePersonalizado("© 2026 Empresa Teste. Todos os direitos reservados.");
        
        ConfiguracaoEmpresa saved = configuracaoEmpresaService.salvarConfiguracao(config);

        // Então as modificações devem ser persistidas
        assertNotNull(saved.getId());
        assertFalse(saved.isUsarLogotipoEmDocumentos());
        assertTrue(saved.isUsarRodapéPersonalizadoEmDocumentos());
        assertEquals("© 2026 Empresa Teste. Todos os direitos reservados.", saved.getRodapePersonalizado());
    }

    @Test
    void testErroEmpresaInvalida() {
        // Quando tentar obter configuração com empresa nula
        assertThrows(RuntimeException.class, () -> {
            configuracaoEmpresaService.obterConfiguracao((Empresa) null);
        });

        // Quando tentar salvar configuração sem empresa
        ConfiguracaoEmpresa config = new ConfiguracaoEmpresa();
        assertThrows(RuntimeException.class, () -> {
            configuracaoEmpresaService.salvarConfiguracao(config);
        });
    }
}
