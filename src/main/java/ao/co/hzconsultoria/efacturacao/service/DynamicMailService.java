package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity;
import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoEmpresa;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoEmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.Optional;

/**
 * Serviço de email dinâmico que constrói o JavaMailSender em tempo de execução
 * a partir das configurações SMTP gravadas na tabela configuracao_sistema ou nas configurações específicas da empresa.
 */
@Service
public class DynamicMailService {

    private static final Logger log = LoggerFactory.getLogger(DynamicMailService.class);

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    @Autowired
    private ConfiguracaoEmpresaRepository configuracaoEmpresaRepository;

    // ─── Auxiliar para validar strings vazias (Java 1.8 compatível) ───

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ─── Construção dinâmica do sender ────────────────────────────────

    /**
     * Versão retrocompatível de buildSender()
     */
    public JavaMailSenderImpl buildSender() {
        return buildSender(null);
    }

    /**
     * Cria e devolve um JavaMailSender configurado com os valores actuais
     * da base de dados (da empresa especificada ou global). Lança IllegalStateException se o email não estiver
     * configurado.
     */
    public JavaMailSenderImpl buildSender(Long empresaId) {
        if (empresaId != null) {
            Optional<ConfiguracaoEmpresa> opt = configuracaoEmpresaRepository.findByEmpresa_Id(empresaId);
            if (opt.isPresent()) {
                ConfiguracaoEmpresa cfg = opt.get();
                String host = cfg.getEmailSmtpHost();
                String username = cfg.getEmailSmtpUsername();
                if (!isEmpty(host) && !isEmpty(username)) {
                    JavaMailSenderImpl sender = new JavaMailSenderImpl();
                    sender.setHost(host);
                    sender.setPort(cfg.getEmailSmtpPorta());
                    sender.setUsername(username);
                    sender.setPassword(cfg.getEmailSmtpPassword());
                    sender.setDefaultEncoding("UTF-8");

                    Properties props = sender.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");

                    String seguranca = cfg.getEmailSegurancaTipo() != null ? cfg.getEmailSegurancaTipo().toUpperCase() : "TLS";

                    switch (seguranca) {
                        case "SSL":
                            props.put("mail.smtp.ssl.enable", "true");
                            props.put("mail.smtp.starttls.enable", "false");
                            break;
                        case "NONE":
                            props.put("mail.smtp.ssl.enable", "false");
                            props.put("mail.smtp.starttls.enable", "false");
                            props.put("mail.smtp.auth", "false");
                            break;
                        default: // TLS (STARTTLS)
                            props.put("mail.smtp.starttls.enable", "true");
                            props.put("mail.smtp.starttls.required", "true");
                            props.put("mail.smtp.ssl.enable", "false");
                            break;
                    }

                    props.put("mail.smtp.connectiontimeout", "5000");
                    props.put("mail.smtp.timeout", "5000");
                    props.put("mail.smtp.writetimeout", "5000");

                    return sender;
                }
            }
        }

        // Fallback global
        ConfiguracaoSistemaEntity cfg = configuracaoSistemaRepository
                .findById(1L)
                .orElseThrow(() -> new IllegalStateException("Configuração do sistema não encontrada."));

        String host = cfg.getEmailSmtpHost();
        String username = cfg.getEmailSmtpUsername();

        if (isEmpty(host)) {
            throw new IllegalStateException("SMTP host não configurado. Aceda a /configuracoes/email para configurar.");
        }
        if (isEmpty(username)) {
            throw new IllegalStateException("SMTP username não configurado. Aceda a /configuracoes/email para configurar.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(cfg.getEmailSmtpPorta());
        sender.setUsername(username);
        sender.setPassword(cfg.getEmailSmtpPassword());
        sender.setDefaultEncoding("UTF-8");

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");

        String seguranca = cfg.getEmailSegurancaTipo() != null ? cfg.getEmailSegurancaTipo().toUpperCase() : "TLS";

        switch (seguranca) {
            case "SSL":
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
                break;
            case "NONE":
                props.put("mail.smtp.ssl.enable", "false");
                props.put("mail.smtp.starttls.enable", "false");
                props.put("mail.smtp.auth", "false");
                break;
            default: // TLS (STARTTLS)
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.ssl.enable", "false");
                break;
        }

        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return sender;
    }

    // ─── Verificar se o email está configurado ────────────────────────

    /**
     * Versão retrocompatível de isEmailConfigurado()
     */
    public boolean isEmailConfigurado() {
        return isEmailConfigurado(null);
    }

    public boolean isEmailConfigurado(Long empresaId) {
        if (empresaId != null) {
            Optional<ConfiguracaoEmpresa> opt = configuracaoEmpresaRepository.findByEmpresa_Id(empresaId);
            if (opt.isPresent()) {
                ConfiguracaoEmpresa cfg = opt.get();
                if (cfg.isEmailHabilitado() 
                    && !isEmpty(cfg.getEmailSmtpHost()) 
                    && !isEmpty(cfg.getEmailSmtpUsername()) 
                    && !isEmpty(cfg.getEmailSmtpPassword())) {
                    return true;
                }
            }
        }

        return configuracaoSistemaRepository.findById(1L)
                .map(cfg -> !isEmpty(cfg.getEmailSmtpHost())
                         && !isEmpty(cfg.getEmailSmtpUsername())
                         && !isEmpty(cfg.getEmailSmtpPassword()))
                .orElse(false);
    }

    // ─── Envio de email simples (texto) ──────────────────────────────

    /**
     * Versão retrocompatível de enviarEmail()
     */
    public void enviarEmail(String para, String assunto, String mensagem) throws Exception {
        enviarEmail(null, para, assunto, mensagem);
    }

    /**
     * Envia um email de texto simples com base no ID de empresa especificado.
     */
    public void enviarEmail(Long empresaId, String para, String assunto, String mensagem) throws Exception {
        JavaMailSenderImpl sender = buildSender(empresaId);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(para);
        msg.setSubject(assunto);
        msg.setText(mensagem);

        // Remetente configurável
        String remetente = null;
        String nomeRemetente = null;

        if (empresaId != null) {
            Optional<ConfiguracaoEmpresa> opt = configuracaoEmpresaRepository.findByEmpresa_Id(empresaId);
            if (opt.isPresent()) {
                ConfiguracaoEmpresa cfg = opt.get();
                if (!isEmpty(cfg.getEmailRemetente())) {
                    remetente = cfg.getEmailRemetente();
                    nomeRemetente = cfg.getEmailNomeRemetente();
                }
            }
        }

        if (isEmpty(remetente)) {
            ConfiguracaoSistemaEntity cfgGlobal = configuracaoSistemaRepository
                    .findById(1L)
                    .orElseThrow(() -> new IllegalStateException("Configuração do sistema não encontrada."));
            remetente = cfgGlobal.getEmailRemetente();
            nomeRemetente = cfgGlobal.getEmailNomeRemetente();
        }

        if (!isEmpty(remetente)) {
            if (!isEmpty(nomeRemetente)) {
                msg.setFrom(nomeRemetente + " <" + remetente + ">");
            } else {
                msg.setFrom(remetente);
            }
        }

        sender.send(msg);
        log.info("Email enviado com sucesso para: {}", para);
    }

    // ─── Envio de email HTML ──────────────────────────────────────────

    /**
     * Versão retrocompatível de enviarEmailHtml()
     */
    public void enviarEmailHtml(String para, String assunto, String htmlBody) throws Exception {
        enviarEmailHtml(null, para, assunto, htmlBody);
    }

    /**
     * Envia um email com conteúdo HTML com base no ID de empresa especificado.
     */
    public void enviarEmailHtml(Long empresaId, String para, String assunto, String htmlBody) throws Exception {
        JavaMailSenderImpl sender = buildSender(empresaId);
        MimeMessage mimeMsg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, true, "UTF-8");

        helper.setTo(para);
        helper.setSubject(assunto);
        helper.setText(htmlBody, true);

        String remetente = null;
        String nomeRemetente = null;

        if (empresaId != null) {
            Optional<ConfiguracaoEmpresa> opt = configuracaoEmpresaRepository.findByEmpresa_Id(empresaId);
            if (opt.isPresent()) {
                ConfiguracaoEmpresa cfg = opt.get();
                if (!isEmpty(cfg.getEmailRemetente())) {
                    remetente = cfg.getEmailRemetente();
                    nomeRemetente = cfg.getEmailNomeRemetente();
                }
            }
        }

        if (isEmpty(remetente)) {
            ConfiguracaoSistemaEntity cfgGlobal = configuracaoSistemaRepository
                    .findById(1L)
                    .orElseThrow(() -> new IllegalStateException("Configuração do sistema não encontrada."));
            remetente = cfgGlobal.getEmailRemetente();
            nomeRemetente = cfgGlobal.getEmailNomeRemetente();
        }

        if (!isEmpty(remetente)) {
            helper.setFrom(remetente, nomeRemetente != null ? nomeRemetente : "Sistema de Facturação");
        }

        sender.send(mimeMsg);
        log.info("Email HTML enviado com sucesso para: {}", para);
    }

    // ─── Teste de conexão SMTP ────────────────────────────────────────

    /**
     * Versão retrocompatível de testarConexao()
     */
    public boolean testarConexao() {
        return testarConexao(null);
    }

    /**
     * Testa a ligação SMTP com as configs actuais.
     */
    public boolean testarConexao(Long empresaId) {
        try {
            JavaMailSenderImpl sender = buildSender(empresaId);
            sender.testConnection();
            log.info("Teste de conexão SMTP bem sucedido.");
            return true;
        } catch (Exception e) {
            log.warn("Falha no teste de conexão SMTP: {}", e.getMessage());
            return false;
        }
    }
}
