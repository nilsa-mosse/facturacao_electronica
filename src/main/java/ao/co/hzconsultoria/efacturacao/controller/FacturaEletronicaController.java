package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import ao.co.hzconsultoria.efacturacao.service.FaturaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/factura-eletronica")
public class FacturaEletronicaController {

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private FaturaService faturaService;

    @Autowired
    private EmpresaRepository empresaRepository;

    // Endpoint para ver e listar o estado atual das facturas baseado no banco de dados
    @GetMapping("/estado")
    public String mostrarEstadoFacturas(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataInicio,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dataFim,
            @RequestParam(required = false) String nif,
            Model model) {

        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();

        // Normalizar filtros para a query
        String statusFilter = (status != null && !status.isEmpty() && !status.equals("TODOS")) ? status : null;
        String nifFilter = (nif != null && !nif.isEmpty()) ? nif : null;

        List<Fatura> facturas = faturaRepository.findByFilters(empresaId, statusFilter, dataInicio, dataFim, nifFilter);

        // Estatísticas para os KPI Cards
        List<Fatura> todasDaEmpresa = faturaRepository.findByEmpresa_Id(empresaId);
        long totalCount = todasDaEmpresa.size();
        long validadasCount = todasDaEmpresa.stream().filter(f -> "VALIDADA".equals(f.getStatus())).count();
        long falhasCount = todasDaEmpresa.stream().filter(f -> "FALHA_ENVIO".equals(f.getStatus())).count();
        long pendentesCount = todasDaEmpresa.stream().filter(f -> "PENDENTE".equals(f.getStatus())).count();

        // Dados da Empresa para o Regime Fiscal
        Empresa empresa = (empresaId != null) ? empresaRepository.findById(empresaId).orElse(null) : null;
        String regimeFiscal = (empresa != null) ? empresa.getRegimeFiscal() : "Regime Geral";

        model.addAttribute("facturas", facturas);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("validadasCount", validadasCount);
        model.addAttribute("falhasCount", falhasCount);
        model.addAttribute("pendentesCount", pendentesCount);
        model.addAttribute("regimeFiscal", regimeFiscal);

        // Manter os filtros no formulário
        model.addAttribute("statusAtual", status);
        model.addAttribute("dataInicioAtual", dataInicio);
        model.addAttribute("dataFimAtual", dataFim);
        model.addAttribute("nifAtual", nif);

        return "estadoFacturasAGT";
    }

    @GetMapping("/reenviar/{id}")
    public String reenviarFatura(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Fatura fatura = faturaService.reenviarFatura(id);
            if (fatura.isEnviadaAGT()) {
                redirectAttributes.addFlashAttribute("mensagemSucesso", "Fatura reenviada com sucesso para a AGT.");
            } else {
                redirectAttributes.addFlashAttribute("mensagemErro", "Falha ao reenviar fatura: " + fatura.getCodigoAgt());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao processar reenvio: " + e.getMessage());
        }
        return "redirect:/factura-eletronica/estado";
    }
}