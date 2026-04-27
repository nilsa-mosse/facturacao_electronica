package ao.co.hzconsultoria.efacturacao.service.tpa;

import ao.co.hzconsultoria.efacturacao.dto.TpaRequestDTO;
import ao.co.hzconsultoria.efacturacao.dto.TpaResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Service
public class TcpTpaServiceImpl implements TpaService {

    private static final Logger logger = LoggerFactory.getLogger(TcpTpaServiceImpl.class);

    @Override
    public TpaResponseDTO enviarPagamentoParaTPA(TpaRequestDTO request, String tpaIpAddress, int port) {
        logger.info("MODO SIMULAÇÃO ATIVO: Ignorando comunicação com TPA físico em {}:{} e aprovando venda de {} Kz.", 
                    tpaIpAddress, port, request.getValor());
        
        return simulateSuccess();
    }

    private TpaResponseDTO simulateSuccess() {
        TpaResponseDTO response = new TpaResponseDTO();
        response.setSucesso(true);
        response.setCodigoResposta("00");
        response.setMensagem("Simulação de Pagamento Aprovado (Hardware Offline).");
        response.setReferenciaBorderou(generateSimulatedRef());
        response.setBancoEmissor("BAI (Simulado)");
        response.setNumCartaoMascarado("XXXX-XXXX-XXXX-8888");
        return response;
    }

    /**
     * Aplica os bytes de controle do protocolo Serial/TCP (STX=0x02 e ETX=0x03)
     * normalmente exigido por TPAs
     */
    private byte[] commBuild(String datagram) {
        String finalCommand = (char) 0x02 + datagram + (char) 0x03;
        return finalCommand.getBytes(); // Pode necessitar charset específico (Cp1252, Windows-1252)
    }

    /**
     * Lê as chaves devolvidas pela máquina.
     * Cada Vendor (ProxyTPA) terá a sua assinatura JSON / URL ENCODED / FIX LENGTH
     */
    private void parseResposta(String respostaRaw, TpaResponseDTO response) {
        try {
            // Parser Simples Simulando resposta-padrão EMIS / Gateway
            if (respostaRaw.contains("RESULT=APROVADO") || respostaRaw.contains("00")) {
                response.setSucesso(true);
                response.setCodigoResposta("00");
                response.setMensagem("Pagamento Aprovado pela EMIS.");

                String ref = extractField(respostaRaw, "REF=");
                response.setReferenciaBorderou(ref != null ? ref : generateSimulatedRef());

                response.setBancoEmissor(extractField(respostaRaw, "BANCO="));
                response.setNumCartaoMascarado(extractField(respostaRaw, "NUMERO_CARTAO="));
            } else {
                response.setSucesso(false);
                response.setCodigoResposta("NN");
                String errorMsg = extractField(respostaRaw, "ERRO=");
                response.setMensagem(
                        errorMsg != null ? errorMsg : "Pagamento recusado na máquina (PIN Errado ou S/ Saldo).");
            }
        } catch (Exception e) {
            response.setSucesso(false);
            response.setMensagem("Falha ao interpretar talão de impressão eletrónico do TPA.");
        }
    }

    private String extractField(String source, String key) {
        int idx = source.indexOf(key);
        if (idx == -1)
            return null;
        int end = source.indexOf(";", idx);
        if (end == -1)
            end = source.indexOf("&", idx);
        if (end == -1)
            end = source.length();
        return source.substring(idx + key.length(), end).trim();
    }

    private String generateSimulatedRef() {
        return String.valueOf((long) (10000000L + Math.random() * 90000000L));
    }
}
