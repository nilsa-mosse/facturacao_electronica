package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.dto.TpaRequestDTO;
import ao.co.hzconsultoria.efacturacao.dto.TpaResponseDTO;
import ao.co.hzconsultoria.efacturacao.service.tpa.TpaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tpa")
public class TpaController {

    @Autowired
    private TpaService tpaService;

    // Configurações padrão que podem ser injetadas do application.properties
    // ou trazidas via Database para cada Loja / PDV
    @Value("${pdv.hardware.tpa.ip:192.168.1.100}")
    private String tpaDefaultIp;

    @Value("${pdv.hardware.tpa.porta:9000}")
    private int tpaDefaultPorta;

    /**
     * Endpoint chamado pelo PDV em Javascript.
     * Este endpoint ficara PENDENTE enquanto a operacao nao terminar fisiamente
     * na maquina TPA ao lado do cliente!
     */
    @PostMapping("/processar-pagamento")
    public ResponseEntity<TpaResponseDTO> processarPagamentoEmTpaReal(@RequestBody TpaRequestDTO request) {

        if (request.getValor() == null || request.getValor() <= 0) {
            TpaResponseDTO erro = new TpaResponseDTO();
            erro.setSucesso(false);
            erro.setMensagem("Valor da venda não confere ou nulo.");
            return ResponseEntity.badRequest().body(erro);
        }

        // --- DISPARO FÍSICO COM O HARDWARE ---
        // Aqui o Java prende (thread segura o connection Http)
        // e vai à rede tentar falar com o TPA por Sockets.
        TpaResponseDTO resposta = tpaService.enviarPagamentoParaTPA(request, tpaDefaultIp, tpaDefaultPorta);

        // Retornamos um 200 OK mesmo que tenha falhado na máquina fisica (ex: sem
        // saldo)
        // O FrontEnd JavaScript (React/Vue/Thymeleaf-JS) lera a flag `sucesso` para
        // decidir o modal a mostrar.
        return ResponseEntity.ok(resposta);
    }
}
