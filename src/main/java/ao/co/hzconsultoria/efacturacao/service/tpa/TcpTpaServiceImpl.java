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
        TpaResponseDTO response = new TpaResponseDTO();
        
        try {
            logger.info("A iniciar conexão TCP ao TPA real no endereço {}:{} com o valor de {} Kz", tpaIpAddress, port, request.getValor());

            /*
             * IMPLEMENTAÇÃO REAL (TCP STREAM SOCKET)
             * Em máquinas reais com protocolo TCP (ex: via Proxy de TPA da EMIS ou Protocolo C na placa de rede)
             * abre-se o socket directo para a porta de escuta da máquina (ex: Pax ou Ingenico conectada no switch da loja).
             */
            try (Socket socket = new Socket(tpaIpAddress, port)) {
                
                // Timeout de 60 ou 90 segundos. Um cliente demorará a inserir o cartão MULTICAIXA e marcar o PIN.
                socket.setSoTimeout(90000); 

                OutputStream out = socket.getOutputStream();
                InputStream in = socket.getInputStream();

                // 1. Cria o Payload ISO8583 ou DataGram que a máquina compreenda 
                // A biblioteca do vendor ditará o formato correcto de bytes (Verifone vs Pax)
                String amountStr = String.format("%.2f", request.getValor()).replace(",", ".");
                String idMsg = request.getIdVenda() != null ? request.getIdVenda() : "VENDA-NORMAL";
                
                // Exemplo de envio text-based simplificado:
                String commandStr = "CMD=COMPRAR&VALOR=" + amountStr + "&REF=" + idMsg;
                byte[] payload = commBuild(commandStr);

                // 2. Envia para o periférico! (O Ecrã da Máquina Acende na Loja)
                out.write(payload);
                out.flush();

                logger.info("Comando enviado! Aguardando o cliente introduzir o seu MULTICAIXA e PIN no TPA...");

                // 3. Lê o stream de retorno após o fecho da transação na máquina pelo cliente (e comunicação com a EMIS)
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);

                if (bytesRead != -1) {
                    String respostaRaw = new String(buffer, 0, bytesRead);
                    logger.info("RESPOSTA FÍSICA DO TPA: {}", respostaRaw);
                    
                    // 4. Analisa a estrutura devolvida (Parsing) do ticket da máquina
                    parseResposta(respostaRaw, response);
                } else {
                    response.setSucesso(false);
                    response.setMensagem("Resposta vazia / Conexão quebrada pelo TPA.");
                }
            }

        } catch (java.net.ConnectException ce) {
            logger.error("Sem resposta do TPA. Máquina desligada ou fora da rede WiFi/Cabo.");
            response.setSucesso(false);
            response.setMensagem("TPA inalcansável na rede. Ligue a máquina ou conecte-a ao Gateway.");
        } catch (java.net.SocketTimeoutException e) {
            logger.error("Tempo estourou. O cliente não digitou o PIN ou cancelou a venda pela máquina.");
            response.setSucesso(false);
            response.setMensagem("O tempo de pagamento esgotou. A operação foi abortada na máquina.");
        } catch (Exception e) {
            logger.error("Erro genérico ao comunicar com TPA TCP: ", e);
            response.setSucesso(false);
            response.setMensagem("Erro no protocolo de comunicação com o equipamento TPA.");
        }

        return response;
    }

    /**
     * Aplica os bytes de controle do protocolo Serial/TCP (STX=0x02 e ETX=0x03) normalmente exigido por TPAs
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
                response.setMensagem(errorMsg != null ? errorMsg : "Pagamento recusado na máquina (PIN Errado ou S/ Saldo).");
            }
        } catch (Exception e) {
            response.setSucesso(false);
            response.setMensagem("Falha ao interpretar talão de impressão eletrónico do TPA.");
        }
    }
    
    private String extractField(String source, String key) {
        int idx = source.indexOf(key);
        if (idx == -1) return null;
        int end = source.indexOf(";", idx);
        if (end == -1) end = source.indexOf("&", idx);
        if (end == -1) end = source.length();
        return source.substring(idx + key.length(), end).trim();
    }

    private String generateSimulatedRef() {
        return String.valueOf((long)(10000000L + Math.random() * 90000000L));
    }
}
