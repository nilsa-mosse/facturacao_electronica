package ao.co.hzconsultoria.efacturacao.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Serviço utilitário para resolução de mensagens i18n nos documentos PDF gerados
 * programaticamente. Encapsula o MessageSource do Spring e garante que a ausência
 * de uma chave retorna a própria chave (sem lançar excepção).
 */
@Service
public class PdfTranslationService {

    @Autowired
    private MessageSource messageSource;

    /**
     * Resolve uma chave de tradução para o locale fornecido.
     *
     * @param key    chave no bundle de mensagens (ex: "pdf.fatura.titulo")
     * @param locale locale do utilizador obtido da sessão HTTP
     * @return texto traduzido, ou a própria chave se não encontrada
     */
    public String t(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }

    /**
     * Resolve uma chave com argumentos de formatação (ex: "Página {0} de {1}").
     *
     * @param key    chave no bundle de mensagens
     * @param args   argumentos de substituição
     * @param locale locale do utilizador
     * @return texto traduzido com argumentos aplicados
     */
    public String t(String key, Object[] args, Locale locale) {
        return messageSource.getMessage(key, args, key, locale);
    }
}
