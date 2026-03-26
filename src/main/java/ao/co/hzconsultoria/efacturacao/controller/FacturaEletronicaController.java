package ao.co.hzconsultoria.efacturacao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/factura-eletronica")
public class FacturaEletronicaController {

    // Lista simulada de facturas enviadas para AGT
    private static final List<FacturaSimulada> facturasEnviadas = new ArrayList<>();

    @GetMapping("/enviar")
    public String mostrarTelaEnvio(Model model) {
        // Adiciona atributo para garantir que o formulário use multipart
        model.addAttribute("formMultipart", true);
        return "enviarFacturaAGT";
    }

    @PostMapping("/enviar")
    public String enviarParaAGT(@RequestParam("numeroFactura") String numeroFactura,
                                @RequestParam("dataEmissao") String dataEmissao,
                                @RequestParam("nifCliente") String nifCliente,
                                @RequestParam("valorTotal") Double valorTotal,
                                @RequestParam("tipoDocumento") String tipoDocumento,
                                @RequestParam("serie") String serie,
                                @RequestParam("xml") MultipartFile xmlFile,
                                Model model) {
        // Simular integração com AGT: adicionar factura à lista com estado "EM_PROCESSAMENTO"
        FacturaSimulada factura = new FacturaSimulada();
        factura.setNumero(numeroFactura);
        factura.setDataEmissao(LocalDate.parse(dataEmissao));
        factura.setNomeCliente(nifCliente); // Em sistemas reais, buscar nome pelo NIF
        factura.setValorTotal(valorTotal);
        factura.setEstado("EM_PROCESSAMENTO");
        factura.setMensagemAGT("Factura recebida pela AGT. Em processamento.");
        synchronized (facturasEnviadas) {
            facturasEnviadas.add(factura);
        }
        model.addAttribute("mensagem", "Factura enviada para AGT com sucesso (simulado)." );
        return "enviarFacturaAGT";
    }

    @GetMapping("/estado")
    public String mostrarEstadoFacturas(Model model) {
        // Simular mudança de estado para algumas facturas
        synchronized (facturasEnviadas) {
            for (int i = 0; i < facturasEnviadas.size(); i++) {
                FacturaSimulada f = facturasEnviadas.get(i);
                if (i % 3 == 0) {
                    f.setEstado("VALIDADA");
                    f.setMensagemAGT("Factura validada com sucesso pela AGT.");
                } else if (i % 3 == 1) {
                    f.setEstado("REJEITADA");
                    f.setMensagemAGT("Erro: NIF do cliente inválido.");
                } else {
                    f.setEstado("EM_PROCESSAMENTO");
                    f.setMensagemAGT("Factura recebida pela AGT. Em processamento.");
                }
            }
        }
        model.addAttribute("facturas", facturasEnviadas);
        return "estadoFacturasAGT";
    }

    // Classe interna para simulação de facturas
    public static class FacturaSimulada {
        private String numero;
        private LocalDate dataEmissao;
        private String nomeCliente;
        private Double valorTotal;
        private String estado;
        private String mensagemAGT;
        public String getNumero() { return numero; }
        public void setNumero(String numero) { this.numero = numero; }
        public LocalDate getDataEmissao() { return dataEmissao; }
        public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
        public String getNomeCliente() { return nomeCliente; }
        public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
        public Double getValorTotal() { return valorTotal; }
        public void setValorTotal(Double valorTotal) { this.valorTotal = valorTotal; }
        public String getEstado() { return estado; }
        public void setEstado(String estado) { this.estado = estado; }
        public String getMensagemAGT() { return mensagemAGT; }
        public void setMensagemAGT(String mensagemAGT) { this.mensagemAGT = mensagemAGT; }
    }
}