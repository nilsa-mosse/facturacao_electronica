package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import ao.co.hzconsultoria.efacturacao.service.FolhaSalarioService;
import ao.co.hzconsultoria.efacturacao.service.IrtImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Controller
@RequestMapping("/rh")
public class RhController {

    @Autowired private ColaboradorRepository colaboradorRepo;
    @Autowired private FolhaProcessamentoRepository folhaRepo;
    @Autowired private SalarioProcessadoRepository salarioRepo;
    @Autowired private EmpresaRepository empresaRepo;
    @Autowired private DepartamentoRepository departamentoRepo;
    @Autowired private SubsidioRepository subsidioRepo;
    @Autowired private ColaboradorSubsidioRepository colaboradorSubsidioRepo;
    @Autowired private SalarioProcessadoSubsidioRepository spSubsidioRepo;
    @Autowired private FolhaSalarioService folhaService;
    @Autowired private ParametroPayrollRepository parametroPayrollRepo;
    @Autowired private EscalaoIrtRepository escalaoIrtRepo;
    @Autowired private IrtImportService irtImportService;

    // ==========================================
    // COLABORADORES
    // ==========================================

    @GetMapping("/colaboradores")
    public String listarColaboradores(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("colaboradores", colaboradorRepo.findByEmpresa_Id(empresaId));
        return "rh/listarColaboradores";
    }

    @GetMapping("/colaboradores/novo")
    public String novoColaboradorForm(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Colaborador colaborador = new Colaborador();
        colaborador.setDataAdmissao(LocalDate.now());
        model.addAttribute("colaborador", colaborador);
        model.addAttribute("departamentos", departamentoRepo.findByEmpresaId(empresaId));
        model.addAttribute("todosSubsidios", subsidioRepo.findByEmpresaId(empresaId));
        return "rh/cadastroColaborador";
    }

    @PostMapping("/colaboradores/salvar")
    public String salvarColaborador(
            @ModelAttribute Colaborador colaborador,
            @RequestParam(value = "departamentoId", required = false) Long departamentoId,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepo.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));

            colaborador.setEmpresa(empresa);

            // Associar departamento
            if (departamentoId != null) {
                departamentoRepo.findById(departamentoId).ifPresent(colaborador::setDepartamento);
            }

            // Primeiro guarda o colaborador para obter ID
            Colaborador saved = colaboradorRepo.save(colaborador);

            // Limpar subsídios antigos
            colaboradorSubsidioRepo.deleteByColaboradorId(saved.getId());

            // Processar subsídios dinâmicos: campos com prefixo "subsidio_"
            List<Subsidio> todosSubsidios = subsidioRepo.findByEmpresaId(empresaId);
            for (Subsidio sub : todosSubsidios) {
                String key = "subsidioVal_" + sub.getId();
                if (allParams.containsKey(key)) {
                    String valStr = allParams.get(key).trim().replace(",", ".");
                    double val = valStr.isEmpty() ? 0.0 : Double.parseDouble(valStr);
                    if (val > 0) {
                        ColaboradorSubsidio cs = new ColaboradorSubsidio(saved, sub, val);
                        colaboradorSubsidioRepo.save(cs);
                    }
                }
            }

            redirectAttrs.addFlashAttribute("success", "Colaborador guardado com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao guardar colaborador: " + e.getMessage());
        }
        return "redirect:/rh/colaboradores";
    }

    @GetMapping("/colaboradores/editar/{id}")
    public String editarColaboradorForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Optional<Colaborador> colOpt = colaboradorRepo.findById(id);
        if (!colOpt.isPresent() || !colOpt.get().getEmpresa().getId().equals(empresaId)) {
            redirectAttrs.addFlashAttribute("error", "Colaborador não encontrado ou sem permissão.");
            return "redirect:/rh/colaboradores";
        }
        model.addAttribute("colaborador", colOpt.get());
        model.addAttribute("departamentos", departamentoRepo.findByEmpresaId(empresaId));
        model.addAttribute("todosSubsidios", subsidioRepo.findByEmpresaId(empresaId));
        return "rh/cadastroColaborador";
    }

    @GetMapping("/colaboradores/eliminar/{id}")
    public String eliminarColaborador(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<Colaborador> colOpt = colaboradorRepo.findById(id);
            if (!colOpt.isPresent() || !colOpt.get().getEmpresa().getId().equals(empresaId)) {
                redirectAttrs.addFlashAttribute("error", "Colaborador não encontrado.");
                return "redirect:/rh/colaboradores";
            }
            colaboradorRepo.delete(colOpt.get());
            redirectAttrs.addFlashAttribute("success", "Colaborador removido com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao remover colaborador: " + e.getMessage());
        }
        return "redirect:/rh/colaboradores";
    }

    // ==========================================
    // DEPARTAMENTOS
    // ==========================================

    @GetMapping("/departamentos")
    public String listarDepartamentos(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("departamentos", departamentoRepo.findByEmpresaId(empresaId));
        model.addAttribute("departamento", new Departamento());
        return "rh/listarDepartamentos";
    }

    @PostMapping("/departamentos/salvar")
    public String salvarDepartamento(@ModelAttribute Departamento departamento,
            @RequestParam(value = "id", required = false) Long id,
            RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepo.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
            if (id != null) {
                departamento.setId(id);
            }
            departamento.setEmpresa(empresa);
            departamentoRepo.save(departamento);
            redirectAttrs.addFlashAttribute("success", "Departamento guardado com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro: " + e.getMessage());
        }
        return "redirect:/rh/departamentos";
    }

    @GetMapping("/departamentos/eliminar/{id}")
    public String eliminarDepartamento(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            departamentoRepo.deleteById(id);
            redirectAttrs.addFlashAttribute("success", "Departamento eliminado.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao eliminar: " + e.getMessage());
        }
        return "redirect:/rh/departamentos";
    }

    // ==========================================
    // SUBSÍDIOS
    // ==========================================

    @GetMapping("/subsidios")
    public String listarSubsidios(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("subsidios", subsidioRepo.findByEmpresaId(empresaId));
        model.addAttribute("subsidio", new Subsidio());
        return "rh/listarSubsidios";
    }

    @PostMapping("/subsidios/salvar")
    public String salvarSubsidio(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "codigo", required = false) String codigo,
            @RequestParam(value = "limiteIsencaoInss", required = false) String limiteIsencaoInssStr,
            @RequestParam(value = "limiteIsencaoIrt", required = false) String limiteIsencaoIrtStr,
            @RequestParam(value = "sujeitoInss", required = false) String sujeitoInssStr,
            @RequestParam(value = "sujeitoIrt", required = false) String sujeitoIrtStr,
            RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepo.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));

            Subsidio subsidio;
            if (id != null) {
                subsidio = subsidioRepo.findById(id).orElse(new Subsidio());
                subsidio.setId(id);
            } else {
                subsidio = new Subsidio();
            }

            // Nome e código
            subsidio.setNome(nome != null ? nome.trim() : null);
            subsidio.setCodigo(codigo != null ? codigo.trim() : null);

            // Limites: aceitar strings com vírgula ou ponto; vazio -> 0.0
            double limInss = 0.0;
            if (limiteIsencaoInssStr != null) {
                String s = limiteIsencaoInssStr.trim().replace(',', '.');
                if (!s.isEmpty()) {
                    try { limInss = Double.parseDouble(s); } catch (NumberFormatException ex) { limInss = 0.0; }
                }
            }
            double limIrt = 0.0;
            if (limiteIsencaoIrtStr != null) {
                String s = limiteIsencaoIrtStr.trim().replace(',', '.');
                if (!s.isEmpty()) {
                    try { limIrt = Double.parseDouble(s); } catch (NumberFormatException ex) { limIrt = 0.0; }
                }
            }
            subsidio.setLimiteIsencaoInss(limInss);
            subsidio.setLimiteIsencaoIrt(limIrt);

            // Flags: checkbox sends 'true' when checked, otherwise hidden 'false' is present
            // Only overwrite existing values when parameter is present; this preserves
            // existing DB values if the param is missing for any reason.
            if (sujeitoInssStr != null) {
                boolean sujeitoInss = sujeitoInssStr.contains("true") || sujeitoInssStr.contains("on");
                subsidio.setSujeitoInss(sujeitoInss);
            }
            if (sujeitoIrtStr != null) {
                boolean sujeitoIrt = sujeitoIrtStr.contains("true") || sujeitoIrtStr.contains("on");
                subsidio.setSujeitoIrt(sujeitoIrt);
            }

            subsidio.setEmpresa(empresa);
            subsidioRepo.save(subsidio);
            redirectAttrs.addFlashAttribute("success", "Subsídio guardado com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro: " + e.getMessage());
        }
        return "redirect:/rh/subsidios";
    }

    @GetMapping("/subsidios/eliminar/{id}")
    public String eliminarSubsidio(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            subsidioRepo.deleteById(id);
            redirectAttrs.addFlashAttribute("success", "Tipo de subsídio eliminado.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao eliminar: " + e.getMessage());
        }
        return "redirect:/rh/subsidios";
    }

    // ==========================================
    // FOLHA DE PROCESSAMENTO
    // ==========================================

    @GetMapping("/processamento")
    public String listarProcessamentos(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("folhas", folhaRepo.findByEmpresa_Id(empresaId));
        model.addAttribute("mesAtual", LocalDate.now().getMonthValue());
        model.addAttribute("anoAtual", LocalDate.now().getYear());
        return "rh/listarProcessamentos";
    }

    @PostMapping("/processamento/novo")
    public String novoProcessamento(@RequestParam("mes") int mes, @RequestParam("ano") int ano,
            RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            FolhaProcessamento folha = folhaService.criarRascunhoFolha(mes, ano, empresaId);
            redirectAttrs.addFlashAttribute("success", "Folha de salários criada como rascunho.");
            return "redirect:/rh/processamento/detalhes/" + folha.getId();
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao iniciar processamento: " + e.getMessage());
            return "redirect:/rh/processamento";
        }
    }

    @GetMapping("/processamento/detalhes/{id}")
    public String detalhesProcessamento(@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Optional<FolhaProcessamento> folhaOpt = folhaRepo.findById(id);
        if (!folhaOpt.isPresent() || !folhaOpt.get().getEmpresa().getId().equals(empresaId)) {
            redirectAttrs.addFlashAttribute("error", "Folha de processamento não encontrada.");
            return "redirect:/rh/processamento";
        }

        FolhaProcessamento folha = folhaOpt.get();
        List<SalarioProcessado> salarios = salarioRepo.findByFolhaProcessamento_Id(folha.getId());

        model.addAttribute("folha", folha);
        model.addAttribute("salarios", salarios);

        double totalBase = salarios.stream().mapToDouble(SalarioProcessado::getSalarioBase).sum();
        double totalIliquido = salarios.stream().mapToDouble(SalarioProcessado::getRendimentoIliquido).sum();
        double totalInssCol = salarios.stream().mapToDouble(SalarioProcessado::getDescontoSegurancaSocial).sum();
        double totalInssPat = salarios.stream().mapToDouble(SalarioProcessado::getEncargoEmpresaSegurancaSocial).sum();
        double totalIrt = salarios.stream().mapToDouble(SalarioProcessado::getDescontoIrt).sum();
        double totalLiquido = salarios.stream().mapToDouble(SalarioProcessado::getSalarioLiquido).sum();

        model.addAttribute("totalBase", totalBase);
        model.addAttribute("totalIliquido", totalIliquido);
        model.addAttribute("totalInssCol", totalInssCol);
        model.addAttribute("totalInssPat", totalInssPat);
        model.addAttribute("totalIrt", totalIrt);
        model.addAttribute("totalLiquido", totalLiquido);

        return "rh/detalhesProcessamento";
    }

    @PostMapping("/processamento/detalhes/{id}/recalcular")
    public String recalcularSalarioColaborador(
            @PathVariable("id") Long folhaId,
            @RequestParam("salarioId") Long salarioId,
            @RequestParam("subsidioFerias") double subFer,
            @RequestParam("subsidioNatal") double subNat,
            @RequestParam Map<String, String> allParams,
            RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<SalarioProcessado> spOpt = salarioRepo.findById(salarioId);
            if (!spOpt.isPresent() || !spOpt.get().getFolhaProcessamento().getId().equals(folhaId)
                    || !spOpt.get().getFolhaProcessamento().getEmpresa().getId().equals(empresaId)) {
                redirectAttrs.addFlashAttribute("error", "Registo de salário não encontrado ou sem permissão.");
                return "redirect:/rh/processamento/detalhes/" + folhaId;
            }

            SalarioProcessado sp = spOpt.get();
            if (!"RASCUNHO".equals(sp.getFolhaProcessamento().getEstado())) {
                redirectAttrs.addFlashAttribute("error", "Não é possível alterar salários fora do estado RASCUNHO.");
                return "redirect:/rh/processamento/detalhes/" + folhaId;
            }

            sp.setSubsidioFerias(subFer);
            sp.setSubsidioNatal(subNat);

            // Actualizar valores dos subsídios dinâmicos
            for (SalarioProcessadoSubsidio sps : sp.getSubsidios()) {
                String key = "subsidioVal_" + sps.getSubsidio().getId();
                if (allParams.containsKey(key)) {
                    String valStr = allParams.get(key).trim().replace(",", ".");
                    double val = valStr.isEmpty() ? 0.0 : Double.parseDouble(valStr);
                    sps.setValor(val);
                }
            }

            folhaService.recalcularSalarioInterno(sp);
            salarioRepo.save(sp);

            redirectAttrs.addFlashAttribute("success", "Cálculos recalculados para " + sp.getColaborador().getNome());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao recalcular vencimento: " + e.getMessage());
        }
        return "redirect:/rh/processamento/detalhes/" + folhaId;
    }

    @PostMapping("/processamento/detalhes/{id}/fechar")
    public String fecharProcessamento(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<FolhaProcessamento> folhaOpt = folhaRepo.findById(id);
            if (!folhaOpt.isPresent() || !folhaOpt.get().getEmpresa().getId().equals(empresaId)) {
                redirectAttrs.addFlashAttribute("error", "Folha não encontrada.");
                return "redirect:/rh/processamento";
            }
            folhaService.processarFolha(id);
            redirectAttrs.addFlashAttribute("success", "Folha processada com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao processar folha: " + e.getMessage());
        }
        return "redirect:/rh/processamento/detalhes/" + id;
    }

    @PostMapping("/processamento/detalhes/{id}/pagar")
    public String pagarProcessamento(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<FolhaProcessamento> folhaOpt = folhaRepo.findById(id);
            if (!folhaOpt.isPresent() || !folhaOpt.get().getEmpresa().getId().equals(empresaId)) {
                redirectAttrs.addFlashAttribute("error", "Folha não encontrada.");
                return "redirect:/rh/processamento";
            }
            folhaService.pagarFolha(id);
            redirectAttrs.addFlashAttribute("success", "Folha paga com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao pagar folha: " + e.getMessage());
        }
        return "redirect:/rh/processamento/detalhes/" + id;
    }

    @PostMapping("/processamento/detalhes/{id}/eliminar")
    public String eliminarProcessamento(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<FolhaProcessamento> folhaOpt = folhaRepo.findById(id);
            if (!folhaOpt.isPresent() || !folhaOpt.get().getEmpresa().getId().equals(empresaId)) {
                redirectAttrs.addFlashAttribute("error", "Folha não encontrada.");
                return "redirect:/rh/processamento";
            }
            folhaService.eliminarFolha(id);
            redirectAttrs.addFlashAttribute("success", "Rascunho eliminado com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao eliminar folha: " + e.getMessage());
        }
        return "redirect:/rh/processamento";
    }

    // ==========================================
    // EXPORTAÇÕES E DOCUMENTOS
    // ==========================================

    @GetMapping("/recibo/{salarioId}")
    public ResponseEntity<byte[]> downloadRecibo(@PathVariable Long salarioId) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<SalarioProcessado> spOpt = salarioRepo.findById(salarioId);
            if (!spOpt.isPresent() || !spOpt.get().getFolhaProcessamento().getEmpresa().getId().equals(empresaId)) {
                return ResponseEntity.notFound().build();
            }
            byte[] pdf = folhaService.gerarReciboPdf(salarioId);
            String filename = "Recibo_" + spOpt.get().getColaborador().getNome().replaceAll("\\s+", "_") + "_"
                    + String.format("%02d_%d", spOpt.get().getFolhaProcessamento().getMes(),
                            spOpt.get().getFolhaProcessamento().getAno()) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/processamento/{id}/inss")
    public ResponseEntity<byte[]> downloadInssCsv(@PathVariable Long id) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<FolhaProcessamento> folhaOpt = folhaRepo.findById(id);
            if (!folhaOpt.isPresent() || !folhaOpt.get().getEmpresa().getId().equals(empresaId)) {
                return ResponseEntity.notFound().build();
            }
            String csvContent = folhaService.gerarFolhaInssCsv(id);
            byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
            String filename = "Folha_INSS_" + String.format("%02d_%d", folhaOpt.get().getMes(), folhaOpt.get().getAno()) + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8)).body(csvBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/processamento/{id}/irt")
    public ResponseEntity<byte[]> downloadIrtPdf(@PathVariable Long id) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Optional<FolhaProcessamento> folhaOpt = folhaRepo.findById(id);
            if (!folhaOpt.isPresent() || !folhaOpt.get().getEmpresa().getId().equals(empresaId)) {
                return ResponseEntity.notFound().build();
            }
            byte[] pdf = folhaService.gerarGuiaIrtPdf(id);
            String filename = "Guia_IRT_" + String.format("%02d_%d", folhaOpt.get().getMes(), folhaOpt.get().getAno()) + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ==========================================
    // PARÂMETROS DE PROCESSAMENTO
    // ==========================================

    @GetMapping("/parametros")
    public String exibirParametros(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        folhaService.inicializarEscaloesPadraoIrtSeNecessario(empresaId);
        
        ParametroPayroll parametros = folhaService.getOrCreateParametros(empresaId);
        List<EscalaoIrt> escaloes = escalaoIrtRepo.findByEmpresaIdOrderByLimiteInferiorAsc(empresaId);

        model.addAttribute("parametros", parametros);
        model.addAttribute("escaloes", escaloes);
        return "rh/parametros";
    }

    @PostMapping("/parametros/salvar")
    public String salvarParametrosGerais(
            @RequestParam("taxaInssTrabalhador") double taxaInssTrab,
            @RequestParam("taxaInssEmpresa") double taxaInssEmp,
            @RequestParam(value = "descontoIrtDependente", required = false) Double descDependente,
            @RequestParam("diasPadraoProcessamento") int diasPadrao,
            RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            ParametroPayroll params = folhaService.getOrCreateParametros(empresaId);
            
            params.setTaxaInssTrabalhador(taxaInssTrab);
            params.setTaxaInssEmpresa(taxaInssEmp);
            if (descDependente != null) {
                params.setDescontoIrtDependente(descDependente);
            }
            params.setDiasPadraoProcessamento(diasPadrao);
            
            parametroPayrollRepo.save(params);
            redirectAttrs.addFlashAttribute("success", "Parâmetros gerais de processamento atualizados com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao guardar parâmetros gerais: " + e.getMessage());
        }
        return "redirect:/rh/parametros";
    }

    @PostMapping("/parametros/irt/salvar")
    @org.springframework.transaction.annotation.Transactional
    public String salvarEscaloesIrt(
            @RequestParam(value = "limiteInferior", required = false) List<Double> limitesInferiores,
            @RequestParam(value = "limiteSuperior", required = false) List<Double> limitesSuperiores,
            @RequestParam(value = "parcelaFixa", required = false) List<Double> parcelasFixas,
            @RequestParam(value = "taxaExcesso", required = false) List<Double> taxasExcesso,
            RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepo.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
            
            escalaoIrtRepo.deleteByEmpresaId(empresaId);
            
            if (limitesInferiores != null) {
                for (int i = 0; i < limitesInferiores.size(); i++) {
                    Double infVal = limitesInferiores.get(i);
                    double inf = infVal != null ? infVal : 0.0;

                    Double sup = null;
                    if (limitesSuperiores != null && i < limitesSuperiores.size() && limitesSuperiores.get(i) != null) {
                        sup = limitesSuperiores.get(i);
                        if (sup <= 0) {
                            sup = null;
                        }
                    }

                    Double parcVal = (parcelasFixas != null && i < parcelasFixas.size()) ? parcelasFixas.get(i) : null;
                    double parc = parcVal != null ? parcVal : 0.0;

                    Double taxaVal = (taxasExcesso != null && i < taxasExcesso.size()) ? taxasExcesso.get(i) : null;
                    double taxa = taxaVal != null ? taxaVal : 0.0;
                    
                    EscalaoIrt esc = new EscalaoIrt(empresa, inf, sup, parc, taxa);
                    escalaoIrtRepo.save(esc);
                }
            }
            redirectAttrs.addFlashAttribute("success", "Tabela progressiva de IRT atualizada com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            redirectAttrs.addFlashAttribute("error", "Erro ao atualizar escalões de IRT: " + message);
        }
        return "redirect:/rh/parametros";
    }

    @PostMapping("/parametros/irt/redefinir")
    @org.springframework.transaction.annotation.Transactional
    public String redefinirIrtPadrao(RedirectAttributes redirectAttrs) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            escalaoIrtRepo.deleteByEmpresaId(empresaId);
            folhaService.inicializarEscaloesPadraoIrtSeNecessario(empresaId);
            
            redirectAttrs.addFlashAttribute("success", "Tabela progressiva de IRT redefinida para os parâmetros padrão oficiais da AGT.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao redefinir tabela: " + e.getMessage());
        }
        return "redirect:/rh/parametros";
    }

    @GetMapping("/parametros/irt/modelo-excel")
    public ResponseEntity<byte[]> descarregarModeloExcelIrt() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Tabela IRT");

            // Estilo cabeçalho
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Estilo dados/exemplo
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Cabeçalhos
            String[] headers = {
                "Escalão",
                "Limite Inferior (Kz) *",
                "Limite Superior (Kz)",
                "Parcela Fixa (Kz) *",
                "Taxa (%) *"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6500);
            }

            // Linhas pré-preenchidas com os 11 escalões oficiais da AGT
            double[][] defaultTiers = {
                {0, 150000, 0, 0},
                {150000, 200000, 12500, 16},
                {200000, 300000, 31250, 18},
                {300000, 500000, 49250, 19},
                {500000, 1000000, 87250, 20},
                {1000000, 1500000, 187250, 21},
                {1500000, 2000000, 292250, 22},
                {2000000, 2500000, 402250, 23},
                {2500000, 5000000, 517250, 24},
                {5000000, 10000000, 1117250, 24.5},
                {10000000, 0, 2342250, 25}
            };

            for (int r = 0; r < defaultTiers.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.createCell(0).setCellValue((r + 1) + "º Escalão");
                row.createCell(1).setCellValue(defaultTiers[r][0]);
                row.createCell(2).setCellValue(defaultTiers[r][1] > 0 ? defaultTiers[r][1] : 0);
                row.createCell(3).setCellValue(defaultTiers[r][2]);
                row.createCell(4).setCellValue(defaultTiers[r][3]);
                for (int c = 0; c < 5; c++) {
                    row.getCell(c).setCellStyle(dataStyle);
                }
            }

            workbook.write(out);
            byte[] bytes = out.toByteArray();

            HttpHeaders respHeaders = new HttpHeaders();
            respHeaders.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            respHeaders.setContentDispositionFormData("attachment", "modelo_tabela_irt.xlsx");
            return ResponseEntity.ok().headers(respHeaders).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/parametros/irt/modelo-pdf")
    public ResponseEntity<byte[]> descarregarModeloPdfIrt() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, out);
            doc.open();

            // Título
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD, new java.awt.Color(0, 32, 96));
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("TABELA PROGRESSIVA DO IRT - ANGOLA (AGT)", titleFont);
            title.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            doc.add(title);

            com.lowagie.text.Font subFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.ITALIC, new java.awt.Color(100, 100, 100));
            com.lowagie.text.Paragraph sub = new com.lowagie.text.Paragraph("Modelo preenchido com os escalões da AGT para verificação e importação no sistema.", subFont);
            sub.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            sub.setSpacingAfter(12f);
            doc.add(sub);

            // Tabela com 5 colunas (incluindo número do escalão)
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{15f, 25f, 25f, 20f, 15f});

            java.awt.Color headerBg = new java.awt.Color(0, 32, 96);
            java.awt.Color headerFg = java.awt.Color.WHITE;
            java.awt.Color evenBg = new java.awt.Color(240, 248, 255);
            java.awt.Color oddBg = java.awt.Color.WHITE;
            java.awt.Color borderColor = new java.awt.Color(180, 180, 180);

            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.BOLD, headerFg);
            com.lowagie.text.Font dataFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 9, com.lowagie.text.Font.NORMAL, java.awt.Color.BLACK);

            String[] headers = {"Escalão", "Limite Inferior (Kz) *", "Limite Superior (Kz)", "Parcela Fixa (Kz) *", "Taxa (%) *"};
            for (String h : headers) {
                com.lowagie.text.pdf.PdfPCell hCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(h, headerFont));
                hCell.setBackgroundColor(headerBg);
                hCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                hCell.setPadding(6f);
                hCell.setBorderColor(borderColor);
                table.addCell(hCell);
            }

            double[][] tiers = {
                {0, 150000, 0, 0},
                {150000, 200000, 12500, 16},
                {200000, 300000, 31250, 18},
                {300000, 500000, 49250, 19},
                {500000, 1000000, 87250, 20},
                {1000000, 1500000, 187250, 21},
                {1500000, 2000000, 292250, 22},
                {2000000, 2500000, 402250, 23},
                {2500000, 5000000, 517250, 24},
                {5000000, 10000000, 1117250, 24.5},
                {10000000, 0, 2342250, 25}
            };

            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("pt", "AO"));
            nf.setGroupingUsed(true);
            nf.setMaximumFractionDigits(2);

            for (int i = 0; i < tiers.length; i++) {
                java.awt.Color rowBg = (i % 2 == 0) ? evenBg : oddBg;
                String[] vals = {
                    (i + 1) + "º Escalão",
                    nf.format(tiers[i][0]),
                    tiers[i][1] > 0 ? nf.format(tiers[i][1]) : "—",
                    nf.format(tiers[i][2]),
                    tiers[i][3] > 0 ? tiers[i][3] + "%" : "0%"
                };
                for (String val : vals) {
                    com.lowagie.text.pdf.PdfPCell dCell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(val, dataFont));
                    dCell.setBackgroundColor(rowBg);
                    dCell.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);
                    dCell.setPadding(5f);
                    dCell.setBorderColor(borderColor);
                    table.addCell(dCell);
                }
            }

            doc.add(table);

            // Nota rodapé
            com.lowagie.text.Font noteFont2 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 8, com.lowagie.text.Font.ITALIC, new java.awt.Color(120, 120, 120));
            com.lowagie.text.Paragraph note = new com.lowagie.text.Paragraph(
                "\nNOTA: (*) campos obrigatórios. Para o último escalão, deixe 'Limite Superior' em branco ou com valor 0. " +
                "Todos os valores monetários estão expressos em Kwanzas (Kz).", noteFont2);
            note.setSpacingBefore(10f);
            doc.add(note);

            doc.close();
            byte[] bytes = out.toByteArray();

            HttpHeaders respHeaders = new HttpHeaders();
            respHeaders.setContentType(MediaType.APPLICATION_PDF);
            respHeaders.setContentDispositionFormData("attachment", "modelo_tabela_irt.pdf");
            return ResponseEntity.ok().headers(respHeaders).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/parametros/irt/importar")
    @ResponseBody
    public ResponseEntity<?> importarIrt(@RequestParam("file") MultipartFile file) {
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepo.findById(empresaId)
                    .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada"));
            List<EscalaoIrt> escaloes = irtImportService.parseFile(file, empresa);
            
            List<Map<String, Object>> response = new ArrayList<>();
            for (EscalaoIrt esc : escaloes) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("limiteInferior", esc.getLimiteInferior());
                map.put("limiteSuperior", esc.getLimiteSuperior() != null ? esc.getLimiteSuperior() : 0.0);
                map.put("parcelaFixa", esc.getParcelaFixa());
                map.put("taxaExcesso", esc.getTaxaExcesso());
                response.add(map);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", e.getMessage()));
        }
    }

    // ==========================================
    // IMPORTAÇÃO DE COLABORADORES
    // ==========================================

    @GetMapping("/colaboradores/modelo-excel")
    public ResponseEntity<byte[]> descarregarModeloExcelColaboradores() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Colaboradores");

            // Estilo para cabeçalho
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Cabeçalhos
            String[] headers = {
                "Nome Completo *", "NIF *", "E-mail", "Telefone", "Endereço", "IBAN", 
                "Salário Base (Kz) *", "Cargo *", "Nº Dependentes (ex: 0)", "Tipo Contrato (Efectivo/Prazo/Prestação)"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Linha de Exemplo 1
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("Manuel António da Silva");
            row1.createCell(1).setCellValue("123456789");
            row1.createCell(2).setCellValue("manuel.silva@empresa.ao");
            row1.createCell(3).setCellValue("923000001");
            row1.createCell(4).setCellValue("Maianga, Luanda");
            row1.createCell(5).setCellValue("AO06004000001111222233334");
            row1.createCell(6).setCellValue(150000.0);
            row1.createCell(7).setCellValue("Técnico de TI");
            row1.createCell(8).setCellValue(2);
            row1.createCell(9).setCellValue("Efectivo");

            // Linha de Exemplo 2
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Maria Domingos");
            row2.createCell(1).setCellValue("987654321");
            row2.createCell(2).setCellValue("maria.domingos@empresa.ao");
            row2.createCell(3).setCellValue("931000002");
            row2.createCell(4).setCellValue("Talatona, Luanda");
            row2.createCell(5).setCellValue("AO06000600005555666677778");
            row2.createCell(6).setCellValue(250000.0);
            row2.createCell(7).setCellValue("Contabilista");
            row2.createCell(8).setCellValue(0);
            row2.createCell(9).setCellValue("Prazo Certo");

            // Redimensionar colunas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Modelo_Importacao_Colaboradores.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/colaboradores/importar-excel")
    @org.springframework.transaction.annotation.Transactional
    public String importarExcelColaboradores(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttrs) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            redirectAttrs.addFlashAttribute("error", "Erro: Sessão expirada ou empresa não identificada.");
            return "redirect:/rh/colaboradores";
        }

        if (file == null || file.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Por favor, selecione um ficheiro Excel válido.");
            return "redirect:/rh/colaboradores";
        }

        Empresa empresa = empresaRepo.findById(empresaId).orElse(null);
        if (empresa == null) {
            redirectAttrs.addFlashAttribute("error", "Empresa não encontrada.");
            return "redirect:/rh/colaboradores";
        }

        int importados = 0;
        int ignorados = 0;
        List<String> erros = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) {
                redirectAttrs.addFlashAttribute("error", "A folha de cálculo não contém dados para importar.");
                return "redirect:/rh/colaboradores";
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nome = getCellValueAsString(row.getCell(0));
                String nif = getCellValueAsString(row.getCell(1));
                String email = getCellValueAsString(row.getCell(2));
                String telefone = getCellValueAsString(row.getCell(3));
                String endereco = getCellValueAsString(row.getCell(4));
                String iban = getCellValueAsString(row.getCell(5));
                Double salarioBase = getCellValueAsDouble(row.getCell(6));
                String cargo = getCellValueAsString(row.getCell(7));
                Double dependentesDouble = getCellValueAsDouble(row.getCell(8));
                String tipoContrato = getCellValueAsString(row.getCell(9));

                if (nome == null || nome.trim().isEmpty()) {
                    continue; // Linha vazia ou sem nome
                }

                if (nif == null || nif.trim().isEmpty()) {
                    ignorados++;
                    erros.add("Linha " + (i + 1) + " ('" + nome + "'): NIF em falta.");
                    continue;
                }

                if (salarioBase == null || salarioBase <= 0) {
                    ignorados++;
                    erros.add("Linha " + (i + 1) + " ('" + nome + "'): Salário base inválido.");
                    continue;
                }

                // Verificar duplicados na empresa
                if (!colaboradorRepo.findByNomeIgnoreCaseAndEmpresa_Id(nome.trim(), empresaId).isEmpty()) {
                    ignorados++;
                    erros.add("Linha " + (i + 1) + " ('" + nome + "'): Já existe colaborador com este nome no sistema.");
                    continue;
                }
                if (!colaboradorRepo.findByNifAndEmpresa_Id(nif.trim(), empresaId).isEmpty()) {
                    ignorados++;
                    erros.add("Linha " + (i + 1) + " ('" + nome + "'): Já existe colaborador com o NIF '" + nif.trim() + "'.");
                    continue;
                }

                Colaborador col = new Colaborador();
                col.setNome(nome.trim());
                col.setNif(nif.trim());
                col.setEmail(email != null ? email.trim() : "");
                col.setTelefone(telefone != null ? telefone.trim() : "");
                col.setEndereco(endereco != null ? endereco.trim() : "");
                col.setIban(iban != null ? iban.trim() : "");
                col.setSalarioBase(salarioBase);
                col.setCargo(cargo != null ? cargo.trim() : "Colaborador");
                col.setDependentes(dependentesDouble != null ? dependentesDouble.intValue() : 0);
                col.setTipoContrato(tipoContrato != null ? tipoContrato.trim() : "Efectivo");
                col.setDataAdmissao(LocalDate.now());
                col.setEmpresa(empresa);

                colaboradorRepo.save(col);
                importados++;
            }

            if (importados > 0) {
                String msg = "Importação concluída com sucesso! " + importados + " colaborador(es) cadastrado(s).";
                if (ignorados > 0) {
                    msg += " (" + ignorados + " registo(s) ignorado(s))";
                }
                redirectAttrs.addFlashAttribute("success", msg);
            } else {
                redirectAttrs.addFlashAttribute("error", "Nenhum colaborador foi importado. Verifique os dados.");
            }

            if (!erros.isEmpty()) {
                redirectAttrs.addFlashAttribute("errosImportacao", erros);
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Erro ao processar ficheiro Excel: " + e.getMessage());
        }

        return "redirect:/rh/colaboradores";
    }

    @PostMapping("/colaboradores/importar-pdf")
    @org.springframework.transaction.annotation.Transactional
    public String importarPdfColaboradores(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttrs) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            redirectAttrs.addFlashAttribute("error", "Erro: Sessão expirada ou empresa não identificada.");
            return "redirect:/rh/colaboradores";
        }

        if (file == null || file.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Por favor, selecione um ficheiro PDF válido.");
            return "redirect:/rh/colaboradores";
        }

        Empresa empresa = empresaRepo.findById(empresaId).orElse(null);
        if (empresa == null) {
            redirectAttrs.addFlashAttribute("error", "Empresa não encontrada.");
            return "redirect:/rh/colaboradores";
        }

        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            List<Colaborador> extraidos = parsePdfText(text, empresa);
            if (extraidos.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Não foi possível extrair nenhum colaborador do PDF. Verifique se o ficheiro contém texto legível.");
                return "redirect:/rh/colaboradores";
            }

            int importados = 0;
            int ignorados = 0;
            List<String> erros = new ArrayList<>();

            for (Colaborador col : extraidos) {
                // Validar duplicados
                if (!colaboradorRepo.findByNomeIgnoreCaseAndEmpresa_Id(col.getNome(), empresaId).isEmpty()) {
                    ignorados++;
                    erros.add("Colaborador '" + col.getNome() + "' já existe no sistema.");
                    continue;
                }
                if (col.getNif() != null && !colaboradorRepo.findByNifAndEmpresa_Id(col.getNif(), empresaId).isEmpty()) {
                    ignorados++;
                    erros.add("NIF '" + col.getNif() + "' do colaborador '" + col.getNome() + "' já existe no sistema.");
                    continue;
                }

                colaboradorRepo.save(col);
                importados++;
            }

            if (importados > 0) {
                String msg = "Importação do PDF concluída! " + importados + " colaborador(es) importado(s).";
                if (ignorados > 0) {
                    msg += " (" + ignorados + " ignorado(s))";
                }
                redirectAttrs.addFlashAttribute("success", msg);
            } else {
                redirectAttrs.addFlashAttribute("error", "Nenhum colaborador foi importado do PDF.");
            }

            if (!erros.isEmpty()) {
                redirectAttrs.addFlashAttribute("errosImportacao", erros);
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Erro ao processar ficheiro PDF: " + e.getMessage());
        }

        return "redirect:/rh/colaboradores";
    }

    @PostMapping("/colaboradores/importar-erp")
    @org.springframework.transaction.annotation.Transactional
    public String importarErpColaboradores(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttrs) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            redirectAttrs.addFlashAttribute("error", "Erro: Sessão expirada ou empresa não identificada.");
            return "redirect:/rh/colaboradores";
        }

        if (file == null || file.isEmpty()) {
            redirectAttrs.addFlashAttribute("error", "Por favor, selecione um ficheiro de exportação válido (CSV, JSON ou XML).");
            return "redirect:/rh/colaboradores";
        }

        Empresa empresa = empresaRepo.findById(empresaId).orElse(null);
        if (empresa == null) {
            redirectAttrs.addFlashAttribute("error", "Empresa não encontrada.");
            return "redirect:/rh/colaboradores";
        }

        String fileName = file.getOriginalFilename();
        List<Map<String, Object>> registros = new ArrayList<>();

        try {
            if (fileName != null && fileName.endsWith(".json")) {
                ObjectMapper mapper = new ObjectMapper();
                registros = mapper.readValue(file.getInputStream(), new TypeReference<List<Map<String, Object>>>() {});
            } else if (fileName != null && fileName.endsWith(".csv")) {
                registros = parseCsv(file);
            } else if (fileName != null && fileName.endsWith(".xml")) {
                registros = parseXml(file);
            } else {
                redirectAttrs.addFlashAttribute("error", "Formato de ficheiro não suportado. Utilize JSON, XML ou CSV.");
                return "redirect:/rh/colaboradores";
            }

            if (registros.isEmpty()) {
                redirectAttrs.addFlashAttribute("error", "Nenhum registo encontrado no ficheiro de importação.");
                return "redirect:/rh/colaboradores";
            }

            int importados = 0;
            int ignorados = 0;
            List<String> erros = new ArrayList<>();

            for (int i = 0; i < registros.size(); i++) {
                Map<String, Object> reg = registros.get(i);

                String nome = getMappedVal(reg, "nome", "name", "full_name", "fullname", "colaborador", "employee", "primeiro_nome", "nome_completo");
                String nif = getMappedVal(reg, "nif", "vat", "nif_colaborador", "contribuinte", "nif_emp", "identificacao", "identificação");
                String email = getMappedVal(reg, "email", "e_mail", "e-mail", "mail");
                String telefone = getMappedVal(reg, "telefone", "phone", "telemovel", "mobile", "tel");
                String endereco = getMappedVal(reg, "endereco", "endereço", "address", "morada");
                String iban = getMappedVal(reg, "iban", "bank_account", "conta_bancaria", "contabancaria");
                String salarioStr = getMappedVal(reg, "salarioBase", "salario_base", "salario", "salário", "vencimento", "salary", "wage", "base_salary", "remuneracao");
                String cargo = getMappedVal(reg, "cargo", "funcao", "função", "job", "role", "position");
                String depStr = getMappedVal(reg, "dependentes", "filhos", "dependents", "n_dependentes");
                String tipoContrato = getMappedVal(reg, "tipoContrato", "tipo_contrato", "contrato", "contract_type", "contract");

                if (nome == null || nome.trim().isEmpty()) {
                    continue;
                }

                if (nif == null || nif.trim().isEmpty()) {
                    ignorados++;
                    erros.add("Registo " + (i + 1) + " ('" + nome + "'): NIF em falta.");
                    continue;
                }

                double salarioBase = 0.0;
                if (salarioStr != null) {
                    try {
                        salarioBase = Double.parseDouble(salarioStr.replace(",", ".").replaceAll("[^0-9.]", ""));
                    } catch (Exception ignored) {}
                }

                if (salarioBase <= 0) {
                    ignorados++;
                    erros.add("Registo " + (i + 1) + " ('" + nome + "'): Salário base inválido ou nulo.");
                    continue;
                }

                int dependentes = 0;
                if (depStr != null) {
                    try {
                        dependentes = Integer.parseInt(depStr.replaceAll("[^0-9]", ""));
                    } catch (Exception ignored) {}
                }

                // Verificar duplicados
                if (!colaboradorRepo.findByNomeIgnoreCaseAndEmpresa_Id(nome.trim(), empresaId).isEmpty()) {
                    ignorados++;
                    erros.add("Registo " + (i + 1) + " ('" + nome + "'): Já existe colaborador com este nome no sistema.");
                    continue;
                }
                if (!colaboradorRepo.findByNifAndEmpresa_Id(nif.trim(), empresaId).isEmpty()) {
                    ignorados++;
                    erros.add("Registo " + (i + 1) + " ('" + nome + "'): Já existe colaborador com o NIF '" + nif.trim() + "'.");
                    continue;
                }

                Colaborador col = new Colaborador();
                col.setNome(nome.trim());
                col.setNif(nif.trim());
                col.setEmail(email != null ? email.trim() : "");
                col.setTelefone(telefone != null ? telefone.trim() : "");
                col.setEndereco(endereco != null ? endereco.trim() : "");
                col.setIban(iban != null ? iban.trim() : "");
                col.setSalarioBase(salarioBase);
                col.setCargo(cargo != null ? cargo.trim() : "Colaborador");
                col.setDependentes(dependentes);
                col.setTipoContrato(tipoContrato != null ? tipoContrato.trim() : "Efectivo");
                col.setDataAdmissao(LocalDate.now());
                col.setEmpresa(empresa);

                colaboradorRepo.save(col);
                importados++;
            }

            if (importados > 0) {
                String msg = "Importação do ERP concluída com sucesso! " + importados + " colaborador(es) cadastrado(s).";
                if (ignorados > 0) {
                    msg += " (" + ignorados + " registo(s) ignorado(s))";
                }
                redirectAttrs.addFlashAttribute("success", msg);
            } else {
                redirectAttrs.addFlashAttribute("error", "Nenhum colaborador foi importado. Verifique os dados.");
            }

            if (!erros.isEmpty()) {
                redirectAttrs.addFlashAttribute("errosImportacao", erros);
            }

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttrs.addFlashAttribute("error", "Erro ao processar ficheiro do ERP: " + e.getMessage());
        }

        return "redirect:/rh/colaboradores";
    }

    // ==========================================
    // MÉTODOS AUXILIARES DE LEITURA E PARSING
    // ==========================================

    private List<Colaborador> parsePdfText(String text, Empresa empresa) {
        List<Colaborador> list = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            // Procurar NIF (exatamente 9 dígitos em Angola)
            java.util.regex.Matcher nifMatcher = java.util.regex.Pattern.compile("\\b\\d{9}\\b").matcher(line);
            if (nifMatcher.find()) {
                String nif = nifMatcher.group();

                // Procurar potencial Salário Base (ex: número > 1000 Kz)
                double salary = 0.0;
                java.util.regex.Matcher salMatcher = java.util.regex.Pattern.compile("\\b(\\d{1,3}(?:\\.?\\d{3})*(?:,\\d{2})?|\\d+(?:\\.\\d{2})?)\\b").matcher(line);
                while (salMatcher.find()) {
                    String valStr = salMatcher.group().replace(".", "").replace(",", ".");
                    try {
                        double val = Double.parseDouble(valStr);
                        if (val > 1000 && val != Double.parseDouble(nif)) {
                            salary = val;
                        }
                    } catch (Exception ignored) {}
                }

                // Extrair Nome removendo números, rótulos e pontuação
                String namePart = line.replaceAll("\\b\\d{9}\\b", "")
                                      .replaceAll("(?i)Nome:", "")
                                      .replaceAll("(?i)NIF:", "")
                                      .replaceAll("(?i)Salario:", "")
                                      .replaceAll("(?i)Salário Base:", "")
                                      .replaceAll("(?i)Vencimento:", "")
                                      .replaceAll("(?i)Cargo:", "")
                                      .replaceAll("(?i)IBAN:", "")
                                      .replaceAll("\\bAO06\\d+\\b", "")
                                      .replaceAll("[0-9.,]+", "")
                                      .replaceAll("[|/;:\\-_]", "")
                                      .trim();

                if (namePart.length() >= 3) {
                    Colaborador col = new Colaborador();
                    col.setNome(namePart.replaceAll("\\s+", " ").trim());
                    col.setNif(nif);
                    col.setSalarioBase(salary > 0 ? salary : 35000.0); // salário mínimo legal como fallback
                    col.setCargo("Colaborador");
                    col.setDataAdmissao(LocalDate.now());
                    col.setEmpresa(empresa);
                    list.add(col);
                }
            }
        }
        return list;
    }

    private List<Map<String, Object>> parseCsv(MultipartFile file) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null) return list;

            // Detectar separador (vírgula ou ponto e vírgula)
            String sep = headerLine.contains(";") ? ";" : ",";
            String[] headers = headerLine.split(sep);
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].replace("\"", "").trim();
            }

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(sep);
                Map<String, Object> map = new java.util.HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    if (i < values.length) {
                        map.put(headers[i], values[i].replace("\"", "").trim());
                    } else {
                        map.put(headers[i], "");
                    }
                }
                list.add(map);
            }
        }
        return list;
    }

    private List<Map<String, Object>> parseXml(MultipartFile file) throws Exception {
        List<Map<String, Object>> list = new ArrayList<>();
        String xmlContent = new String(file.getBytes(), StandardCharsets.UTF_8);

        // Parsing simples e robusto por regex para extrair elementos sem dependências pesadas
        java.util.regex.Matcher itemMatcher = java.util.regex.Pattern.compile("<colaborador>(.*?)</colaborador>", java.util.regex.Pattern.DOTALL).matcher(xmlContent);
        if (!itemMatcher.find()) {
            // Tentar tag genérica 'employee' ou 'record'
            itemMatcher = java.util.regex.Pattern.compile("<(?:employee|record)>(.*?)</(?:employee|record)>", java.util.regex.Pattern.DOTALL).matcher(xmlContent);
        }

        itemMatcher.reset();
        while (itemMatcher.find()) {
            String inner = itemMatcher.group(1);
            Map<String, Object> map = new java.util.HashMap<>();
            java.util.regex.Matcher fieldMatcher = java.util.regex.Pattern.compile("<(\\w+)>(.*?)</\\1>").matcher(inner);
            while (fieldMatcher.find()) {
                map.put(fieldMatcher.group(1), fieldMatcher.group(2).trim());
            }
            if (!map.isEmpty()) {
                list.add(map);
            }
        }
        return list;
    }

    private String getMappedVal(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            for (String mapKey : map.keySet()) {
                if (mapKey.equalsIgnoreCase(k) || mapKey.toLowerCase().replace("_", "").replace(" ", "").equals(k.toLowerCase().replace("_", "").replace(" ", ""))) {
                    Object obj = map.get(mapKey);
                    return obj != null ? obj.toString().trim() : null;
                }
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double doubleVal = cell.getNumericCellValue();
                if (doubleVal == (long) doubleVal) {
                    return String.format("%d", (long) doubleVal);
                } else {
                    return String.valueOf(doubleVal);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    String str = cell.getStringCellValue().trim().replace(",", ".");
                    return Double.parseDouble(str);
                } catch (NumberFormatException e) {
                    return null;
                }
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    return null;
                }
            default:
                return null;
        }
    }
}
