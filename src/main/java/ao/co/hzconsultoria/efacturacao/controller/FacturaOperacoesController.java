package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.service.FaturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para operações sobre facturas do tipo FT (Fatura)
 */
@RestController
@RequestMapping("/api/faturas/operacoes")
public class FacturaOperacoesController {

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private FaturaRepository faturaRepository;

    /**
     * Operação: Imprimir factura
     * POST /api/faturas/operacoes/{id}/imprimir
     */
    @PostMapping("/{id}/imprimir")
    public ResponseEntity<?> imprimirFatura(@PathVariable Long id) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            faturaService.imprimirFatura(id);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "Factura " + fatura.getNumeroFatura() + " marcada como impressa");
            resp.put("numerOfatura", fatura.getNumeroFatura());
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao imprimir: " + e.getMessage());
        }
    }

    /**
     * Operação: Enviar por Email
     * POST /api/faturas/operacoes/{id}/enviar-email
     */
    @PostMapping("/{id}/enviar-email")
    public ResponseEntity<?> enviarPorEmail(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            String emailDestino = payload.get("email");
            if (emailDestino == null || emailDestino.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Email de destino não fornecido");
            }

            faturaService.enviarPorEmail(id, emailDestino);

            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "Factura " + fatura.getNumeroFatura() + " enviada com sucesso para " + emailDestino);
            resp.put("numeroFatura", fatura.getNumeroFatura());
            resp.put("email", emailDestino);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao enviar email: " + e.getMessage());
        }
    }

    /**
     * Operação: Gerar/Descarregar PDF
     * GET /api/faturas/operacoes/{id}/pdf
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<?> gerarPdf(@PathVariable Long id) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            faturaService.gerarPdfFatura(fatura);

            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "PDF gerado com sucesso");
            resp.put("numeroFatura", fatura.getNumeroFatura());
            resp.put("urlPdf", "/uploads/faturas/" + fatura.getNumeroFatura() + ".pdf");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao gerar PDF: " + e.getMessage());
        }
    }

    /**
     * Operação: Registar Pagamento Parcial
     * POST /api/faturas/operacoes/{id}/pagamento-parcial
     */
    @PostMapping("/{id}/pagamento-parcial")
    public ResponseEntity<?> registarPagamentoParcial(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            Double valor = payload.get("valor") == null ? null : Double.valueOf(payload.get("valor").toString());
            String metodo = payload.get("metodo") == null ? "CASH" : payload.get("metodo").toString();
            String referencia = payload.get("referencia") == null ? null : payload.get("referencia").toString();

            if (valor == null || valor <= 0) {
                return ResponseEntity.badRequest().body("Valor de pagamento inválido");
            }

            Fatura faturaAtualizada = faturaService.registarPagamentoParcial(id, valor, metodo, referencia);

            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "Pagamento parcial registado com sucesso");
            resp.put("numeroFatura", faturaAtualizada.getNumeroFatura());
            resp.put("status", faturaAtualizada.getStatus());
            resp.put("valorPago", faturaAtualizada.getValorPago());
            resp.put("valorEmAberto", faturaAtualizada.getValorEmAberto());
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao registar pagamento: " + e.getMessage());
        }
    }

    /**
     * Operação: Registar Pagamento Total
     * POST /api/faturas/operacoes/{id}/pagamento-total
     */
    @PostMapping("/{id}/pagamento-total")
    public ResponseEntity<?> registarPagamentoTotal(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            String metodo = payload.get("metodo") == null ? "CASH" : payload.get("metodo");
            String referencia = payload.get("referencia");

            Fatura faturaAtualizada = faturaService.registarPagamentoTotal(id, metodo, referencia);

            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "Pagamento total registado com sucesso");
            resp.put("numeroFatura", faturaAtualizada.getNumeroFatura());
            resp.put("status", faturaAtualizada.getStatus());
            resp.put("valorPago", faturaAtualizada.getValorPago());
            resp.put("Total", faturaAtualizada.getTotal());
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao registar pagamento: " + e.getMessage());
        }
    }

    /**
     * Operação: Converter para Factura-Recibo
     * POST /api/faturas/operacoes/{id}/converter-recibo
     */
    @PostMapping("/{id}/converter-recibo")
    public ResponseEntity<?> converterParaFaturaRecibo(@PathVariable Long id) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            if (!"FT".equals(fatura.getTipoDocumento())) {
                return ResponseEntity.badRequest().body("Apenas facturas (FT) podem ser convertidas para Factura-Recibo");
            }

            Fatura recibo = faturaService.converterParaFaturaRecibo(id);

            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "Factura convertida para Factura-Recibo com sucesso");
            resp.put("faturaOriginal", fatura.getNumeroFatura());
            resp.put("reciboNumero", recibo.getNumeroFatura());
            resp.put("reciboStatus", recibo.getStatus());
            resp.put("urlPdf", "/uploads/faturas/" + recibo.getNumeroFatura() + ".pdf");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao converter para recibo: " + e.getMessage());
        }
    }

    /**
     * Operação: Emitir Nota de Crédito
     * POST /api/faturas/operacoes/{id}/nota-credito
     * Nota: Esta operação requer uma Devolucao, portanto será implementada em outro endpoint
     */

    /**
     * Operação: Emitir Nota de Débito
     * POST /api/faturas/operacoes/{id}/nota-debito
     */
    @PostMapping("/{id}/nota-debito")
    public ResponseEntity<?> emitirNotaDebito(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura de referência não encontrada");
            }

            if (!"FT".equals(fatura.getTipoDocumento())) {
                return ResponseEntity.badRequest().body("Nota de Débito só pode ser emitida contra uma Factura (FT)");
            }

            Double valor = payload.get("valor") == null ? null : Double.valueOf(payload.get("valor").toString());
            String motivo = payload.get("motivo") == null ? "Nota de Débito" : payload.get("motivo").toString();

            if (valor == null || valor <= 0) {
                return ResponseEntity.badRequest().body("Valor inválido para Nota de Débito");
            }

            Fatura notaDebito = faturaService.emitirNotaDebito(id, valor, motivo);

            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "Nota de Débito emitida com sucesso");
            resp.put("faturaReferencia", fatura.getNumeroFatura());
            resp.put("notaDebito", notaDebito.getNumeroFatura());
            resp.put("valor", valor);
            resp.put("motivo", motivo);
            resp.put("urlPdf", "/uploads/faturas/" + notaDebito.getNumeroFatura() + ".pdf");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao emitir Nota de Débito: " + e.getMessage());
        }
    }

    /**
     * Operação: Consultar Estado AGT
     * GET /api/faturas/operacoes/{id}/estado-agt
     */
    @GetMapping("/{id}/estado-agt")
    public ResponseEntity<?> consultarEstadoAgt(@PathVariable Long id) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            Map<String, Object> estado = faturaService.consultarEstadoAgt(id);
            return ResponseEntity.ok(estado);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao consultar estado AGT: " + e.getMessage());
        }
    }

    /**
     * Operação: Anular Factura
     * POST /api/faturas/operacoes/{id}/anular
     */
    @PostMapping("/{id}/anular")
    public ResponseEntity<?> anularFatura(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            Fatura fatura = faturaRepository.findById(id).orElse(null);
            if (fatura == null) {
                return ResponseEntity.badRequest().body("Factura não encontrada");
            }

            String motivo = payload.get("motivo") == null ? "" : payload.get("motivo");

            Fatura faturaAnulada = faturaService.anularFatura(id, motivo);

            Map<String, Object> resp = new HashMap<>();
            resp.put("mensagem", "Factura anulada com sucesso");
            resp.put("numeroFatura", faturaAnulada.getNumeroFatura());
            resp.put("status", faturaAnulada.getStatus());
            resp.put("motivo", motivo);
            return ResponseEntity.ok(resp);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao anular factura: " + e.getMessage());
        }
    }
}
