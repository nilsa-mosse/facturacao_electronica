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
                    double inf = limitesInferiores.get(i);
                    Double sup = null;
                    if (limitesSuperiores != null && i < limitesSuperiores.size() && limitesSuperiores.get(i) != null) {
                        sup = limitesSuperiores.get(i);
                        if (sup <= 0) {
                            sup = null;
                        }
                    }
                    double parc = (parcelasFixas != null && i < parcelasFixas.size()) ? parcelasFixas.get(i) : 0.0;
                    double taxa = (taxasExcesso != null && i < taxasExcesso.size()) ? taxasExcesso.get(i) : 0.0;
                    
                    EscalaoIrt esc = new EscalaoIrt(empresa, inf, sup, parc, taxa);
                    escalaoIrtRepo.save(esc);
                }
            }
            redirectAttrs.addFlashAttribute("success", "Tabela progressiva de IRT atualizada com sucesso.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao atualizar escalões de IRT: " + e.getMessage());
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
            redirectAttrs.addFlashAttribute("success", "Tabela progressiva de IRT redefinida para os valores oficiais da AGT.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao redefinir tabela: " + e.getMessage());
        }
        return "redirect:/rh/parametros";
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
}
