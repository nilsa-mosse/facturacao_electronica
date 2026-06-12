package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.service.FaturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/faturas")
public class FaturaApiController {

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private FaturaService faturaService;

    /** Registrar pagamento (parcial ou total) para uma fatura existente. Gera recibo (FR) quando pagamento é efetuado. */
    @PostMapping("/{id}/registrar-pagamento")
    public ResponseEntity<?> registrarPagamento(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Fatura fatura = faturaRepository.findById(id).orElse(null);
        if (fatura == null) return ResponseEntity.badRequest().body("Fatura não encontrada");

        if ("FP".equals(fatura.getTipoDocumento())) {
            return ResponseEntity.badRequest().body("Não é possível registar pagamento para uma pró-forma");
        }

        if ("FR".equals(fatura.getTipoDocumento())) {
            return ResponseEntity.badRequest().body("Factura-Recibo (FR) já representa pagamento imediato; não é possível registar pagamento novamente");
        }

        if (fatura.getStatus() != null && (fatura.getStatus().equalsIgnoreCase("ANULADA") || fatura.getStatus().equalsIgnoreCase("CANCELADA"))) {
            return ResponseEntity.badRequest().body("Não é possível pagar uma fatura anulada/cancelada");
        }

        Double amount = payload.get("amount") == null ? null : Double.valueOf(payload.get("amount").toString());
        String method = payload.get("method") == null ? "CASH" : payload.get("method").toString();
        String referencia = payload.get("referencia") == null ? null : payload.get("referencia").toString();

        if (amount == null || amount <= 0) return ResponseEntity.badRequest().body("Montante inválido");

        Compra compra = fatura.getCompra();
        if (compra == null) return ResponseEntity.badRequest().body("Compra associada não encontrada");

        Double paidCash = compra.getValorPagoCash() != null ? compra.getValorPagoCash() : 0.0;
        Double paidMcx = compra.getValorPagoMulticaixa() != null ? compra.getValorPagoMulticaixa() : 0.0;

        if ("MULTICAIXA".equalsIgnoreCase(method) || "TPA".equalsIgnoreCase(method) || "CARD".equalsIgnoreCase(method)) {
            paidMcx += amount;
            compra.setValorPagoMulticaixa(paidMcx);
            if (referencia != null) compra.setReferenciaMulticaixa(referencia);
        } else {
            paidCash += amount;
            compra.setValorPagoCash(paidCash);
        }

        compraRepository.save(compra);

        double total = fatura.getTotal() != null ? fatura.getTotal() : 0.0;
        double totalPaid = (compra.getValorPagoCash() != null ? compra.getValorPagoCash() : 0.0) + (compra.getValorPagoMulticaixa() != null ? compra.getValorPagoMulticaixa() : 0.0);

        if (totalPaid >= total) {
            fatura.setStatus("PAGA");
            compra.setStatus("PAGA");
        } else if (totalPaid > 0) {
            fatura.setStatus("PARCIALMENTE_PAGA");
            compra.setStatus("PARCIALMENTE_PAGA");
        }
        compraRepository.save(compra);
        faturaRepository.save(fatura);
        faturaService.gerarPdfFatura(fatura);

        // Gerar Factura-Recibo (FR) para o pagamento efetuado
        try {
            // Usar método dedicado que gera um recibo com o montante pago
            Fatura recibo = faturaService.emitirReciboPagamento(compra, amount);
            Map<String, Object> resp = new HashMap<>();
            resp.put("reciboNumero", recibo.getNumeroFatura());
            resp.put("reciboPdf", "/uploads/faturas/" + recibo.getNumeroFatura() + ".pdf");
            resp.put("faturaStatus", fatura.getStatus());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("message", "Pagamento registado, mas falha ao gerar recibo: " + e.getMessage());
            resp.put("faturaStatus", fatura.getStatus());
            return ResponseEntity.ok(resp);
        }
    }

    /** Converter uma pró-forma (FP) em Factura (FT) — realiza o envio à AGT conforme regras. */
    @PostMapping("/{id}/converter")
    public ResponseEntity<?> converterProforma(@PathVariable Long id) {
        Fatura fatura = faturaRepository.findById(id).orElse(null);
        if (fatura == null) return ResponseEntity.badRequest().body("Fatura não encontrada");
        if (!"FP".equals(fatura.getTipoDocumento())) return ResponseEntity.badRequest().body("Documento não é pró-forma");

        Compra compra = fatura.getCompra();
        if (compra == null) return ResponseEntity.badRequest().body("Compra associada não encontrada");

        // Emitir FT a partir da mesma compra
        try {
            Fatura nova = faturaService.emitirDocumento(compra, "FT");
            Map<String, Object> resp = new HashMap<>();
            resp.put("numeroFactura", nova.getNumeroFatura());
            resp.put("pdf", "/uploads/faturas/" + nova.getNumeroFatura() + ".pdf");
            resp.put("status", nova.getStatus());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao converter pró-forma: " + e.getMessage());
        }
    }
}
