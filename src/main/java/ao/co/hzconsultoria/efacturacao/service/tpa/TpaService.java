package ao.co.hzconsultoria.efacturacao.service.tpa;

import ao.co.hzconsultoria.efacturacao.dto.TpaRequestDTO;
import ao.co.hzconsultoria.efacturacao.dto.TpaResponseDTO;

public interface TpaService {
    /**
     * Envia os dados de cobrança por rede para o Terminal TPA Físico ou Virtual (Gateway)
     * e aguarda a finalização da compra pelo utilizador (inserir cartão e pin).
     */
    TpaResponseDTO enviarPagamentoParaTPA(TpaRequestDTO request, String tpaIpAddress, int port);
}
