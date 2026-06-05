package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.ConfiguracaoSistemaEntity;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Serviço de email dinâmico que constrói o JavaMailSender em tempo de execução
 * a partir das configurações SMTP gravadas na tabela configuracao_sistema.
 *
 * Desta forma o administrador pode alterar as configs SMTP em
 * /configuracoes/email e os emails são enviados imediatamente com os novos
 * valores — sem necessidade de reiniciar a aplicação.
 */
@Service
public class DynamicMailService {

    private static final Logger log = LoggerFactory.getLogger(DynamicMailService.class);

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    // ─── Auxiliar para validar strings vazias (Java 1.8 compatível) ───

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    // ─── Construção dinâmica do sender ────────────────────────────────

    /**
     * Cria e devolve um JavaMailSender configurado com os valores actuais
     * da base de dados. Lança IllegalStateException se o email não estiver
     * configurado (host ou username vazio).
     */
    public JavaMailSenderImpl buildSender() {
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
            default: // TLS (STARTTLS) — padrão
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

    public boolean isEmailConfigurado() {
        return configuracaoSistemaRepository.findById(1L)
                .map(cfg -> cfg.getEmailSmtpHost() != null && !cfg.getEmailSmtpHost().trim().isEmpty()
                         && cfg.getEmailSmtpUsername() != null && !cfg.getEmailSmtpUsername().trim().isEmpty()
                         && cfg.getEmailSmtpPassword() != null && !cfg.getEmailSmtpPassword().trim().isEmpty())
                .orElse(false);
    }

    // ─── Envio de email simples (texto) ──────────────────────────────

    /**
     * Envia um email de texto simples.
     *
     * @param para       endereço destinatário
     * @param assunto    assunto do email
     * @param mensagem   corpo do email
     * @throws Exception se o envio falhar ou o email não estiver configurado
     */
    public void enviarEmail(String para, String assunto, String mensagem) throws Exception {
        ConfiguracaoSistemaEntity cfg = configuracaoSistemaRepository
                .findById(1L)
                .orElseThrow(() -> new IllegalStateException("Configuração do sistema não encontrada."));

        JavaMailSenderImpl sender = buildSender();

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(para);
        msg.setSubject(assunto);
        msg.setText(mensagem);

        // Remetente configurável
        String remetente = cfg.getEmailRemetente();
        String nomeRemetente = cfg.getEmailNomeRemetente();
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
     * Envia um email com conteúdo HTML.
     *
     * @param para       endereço destinatário
     * @param assunto    assunto do email
     * @param htmlBody   corpo HTML do email
     * @throws Exception se o envio falhar ou o email não estiver configurado
     */
    public void enviarEmailHtml(String para, String assunto, String htmlBody) throws Exception {
        ConfiguracaoSistemaEntity cfg = configuracaoSistemaRepository
                .findById(1L)
                .orElseThrow(() -> new IllegalStateException("Configuração do sistema não encontrada."));

        JavaMailSenderImpl sender = buildSender();
        MimeMessage mimeMsg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMsg, true, "UTF-8");

        helper.setTo(para);
        helper.setSubject(assunto);
        helper.setText(htmlBody, true);

        String remetente = cfg.getEmailRemetente();
        String nomeRemetente = cfg.getEmailNomeRemetente();
        if (!isEmpty(remetente)) {
            helper.setFrom(remetente, nomeRemetente != null ? nomeRemetente : "Sistema de Facturação");
        }

        sender.send(mimeMsg);
        log.info("Email HTML enviado com sucesso para: {}", para);
    }

    // ─── Teste de conexão SMTP ────────────────────────────────────────

    /**
     * Testa a ligação SMTP com as configs actuais.
     *
     * @return true se a ligação for bem sucedida, false caso contrário
     */
    public boolean testarConexao() {
        try {
            JavaMailSenderImpl sender = buildSender();
            sender.testConnection();
            log.info("Teste de conexão SMTP bem sucedido.");
            return true;
        } catch (Exception e) {
            log.warn("Falha no teste de conexão SMTP: {}", e.getMessage());
            return false;
        }
    }
}
