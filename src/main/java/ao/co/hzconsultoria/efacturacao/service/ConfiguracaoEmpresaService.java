package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Serviço para gerenciar configurações específicas de cada empresa
 * Cada empresa pode ter suas próprias configurações de:
 * - Email/SMTP
 * - Storage
 * - Segurança
 * - Integração AGT
 * - Preferências de documentos
 */
@Service
@Transactional
public class ConfiguracaoEmpresaService {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracaoEmpresaService.class);

    @Autowired
    private ConfiguracaoEmpresaRepository configuracaoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    /**
     * Obtém a configuração de uma empresa
     * Se não existir, cria uma com valores padrão
     */
    public ConfiguracaoEmpresa obterConfiguracao(Long empresaId) {
        Optional<ConfiguracaoEmpresa> optional = configuracaoRepository.findByEmpresa_Id(empresaId);

        if (optional.isPresent()) {
            return optional.get();
        }

        // Cria configuração padrão se não existir
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada: " + empresaId));

        ConfiguracaoEmpresa novaConfig = new ConfiguracaoEmpresa();
        novaConfig.setEmpresa(empresa);
        
        return configuracaoRepository.save(novaConfig);
    }

    /**
     * Obtém a configuração de uma empresa a partir do objeto Empresa
     */
    public ConfiguracaoEmpresa obterConfiguracao(Empresa empresa) {
        if (empresa == null || empresa.getId() == null) {
            throw new RuntimeException("Empresa inválida");
        }
        return obterConfiguracao(empresa.getId());
    }

    /**
     * Salva a configuração de uma empresa
     */
    public ConfiguracaoEmpresa salvarConfiguracao(ConfiguracaoEmpresa configuracao) {
        if (configuracao.getEmpresa() == null || configuracao.getEmpresa().getId() == null) {
            throw new RuntimeException("Configuração deve estar associada a uma empresa");
        }
        return configuracaoRepository.save(configuracao);
    }

    /**
     * Atualiza a configuração de email de uma empresa
     */
    public void atualizarConfiguracaoEmail(Long empresaId, String smtpHost, int smtpPorta,
                                          String smtpUsername, String smtpPassword,
                                          String segurancaTipo, String remetente, String nomeRemetente) {
        ConfiguracaoEmpresa config = obterConfiguracao(empresaId);
        config.setEmailSmtpHost(smtpHost);
        config.setEmailSmtpPorta(smtpPorta);
        config.setEmailSmtpUsername(smtpUsername);
        config.setEmailSmtpPassword(smtpPassword);
        config.setEmailSegurancaTipo(segurancaTipo);
        config.setEmailRemetente(remetente);
        config.setEmailNomeRemetente(nomeRemetente);
        config.setEmailHabilitado(true);
        
        configuracaoRepository.save(config);
        log.info("Configuração de email atualizada para empresa: {}", empresaId);
    }

    /**
     * Atualiza a configuração de storage de uma empresa
     */
    public void atualizarConfiguracaoStorage(Long empresaId, String storageTipo, String caminhoBase,
                                            int tamanhoMaxFicheiro, int tamanhoMaxRequest) {
        ConfiguracaoEmpresa config = obterConfiguracao(empresaId);
        config.setStorageTipo(storageTipo);
        config.setStorageCaminhoBase(caminhoBase);
        config.setStorageTamanhoMaxFicheiro(tamanhoMaxFicheiro);
        config.setStorageTamanhoMaxRequest(tamanhoMaxRequest);
        
        configuracaoRepository.save(config);
        log.info("Configuração de storage atualizada para empresa: {}", empresaId);
    }

    /**
     * Atualiza a integração com AGT (Autoridade Geral Tributária)
     */
    public void atualizarConfiguracaoAGT(Long empresaId, boolean habilitada, String urlServico,
                                        String usuario, String senha, String certificado) {
        ConfiguracaoEmpresa config = obterConfiguracao(empresaId);
        config.setAgtIntegracaoHabilitada(habilitada);
        config.setAgtUrlServico(urlServico);
        config.setAgtUsuario(usuario);
        config.setAgtSenha(senha);
        config.setAgtCertificado(certificado);
        
        configuracaoRepository.save(config);
        log.info("Configuração AGT atualizada para empresa: {}", empresaId);
    }

    /**
     * Atualiza a política de segurança de uma empresa
     */
    public void atualizarPoliticaSeguranca(Long empresaId, int tempoExpiracaoSessao,
                                          boolean twoFactorAtivo, boolean requireUppercase,
                                          boolean requireNumbers, boolean requireSpecialChars,
                                          int comprimentoMinPassword) {
        ConfiguracaoEmpresa config = obterConfiguracao(empresaId);
        config.setSegTempoExpiracaoSessao(tempoExpiracaoSessao);
        config.setSegTwoFactorAtivo(twoFactorAtivo);
        config.setSegRequireUppercase(requireUppercase);
        config.setSegRequireNumbers(requireNumbers);
        config.setSegRequireSpecialChars(requireSpecialChars);
        config.setSegComprimentoMinPassword(comprimentoMinPassword);
        
        configuracaoRepository.save(config);
        log.info("Política de segurança atualizada para empresa: {}", empresaId);
    }

    /**
     * Verifica se um email está configurado para uma empresa
     */
    public boolean temEmailConfigurado(Long empresaId) {
        ConfiguracaoEmpresa config = obterConfiguracao(empresaId);
        return config.isEmailHabilitado() && 
               config.getEmailSmtpHost() != null && !config.getEmailSmtpHost().isEmpty() &&
               config.getEmailSmtpUsername() != null && !config.getEmailSmtpUsername().isEmpty();
    }

    /**
     * Verifica se a integração com AGT está ativa
     */
    public boolean temAgtConfigurada(Long empresaId) {
        ConfiguracaoEmpresa config = obterConfiguracao(empresaId);
        return config.isAgtIntegracaoHabilitada() &&
               config.getAgtUrlServico() != null && !config.getAgtUrlServico().isEmpty() &&
               config.getAgtUsuario() != null && !config.getAgtUsuario().isEmpty();
    }
}
