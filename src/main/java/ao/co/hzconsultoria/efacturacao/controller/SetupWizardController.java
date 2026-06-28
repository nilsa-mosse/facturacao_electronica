package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import ao.co.hzconsultoria.efacturacao.service.ConfiguracaoEmpresaService;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Controller
@RequestMapping("/configuracoes/setup-wizard")
public class SetupWizardController {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private RegimeFiscalRepository regimeFiscalRepository;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;

    @Autowired
    private SerieRepository serieRepository;

    @Autowired
    private MetodoPagamentoRepository metodoPagamentoRepository;

    @Autowired
    private ImpostoRepository impostoRepository;

    @Autowired
    private ConfiguracaoEmpresaService configuracaoEmpresaService;

    @GetMapping
    public String viewWizard(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            return "redirect:/login";
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(new Empresa());
        ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresaId);

        model.addAttribute("empresa", empresa);
        model.addAttribute("config", config);
        model.addAttribute("regimes", regimeFiscalRepository.findAll());
        
        // Load existing establishments
        List<Estabelecimento> ests = estabelecimentoRepository.findByEmpresa_Id(empresaId);
        model.addAttribute("estabelecimento", ests.isEmpty() ? new Estabelecimento() : ests.get(0));

        // Load existing series
        List<Serie> series = serieRepository.findByEmpresa_Id(empresaId);
        model.addAttribute("serie", series.isEmpty() ? new Serie() : series.get(0));

        return "configuracoes/setup_wizard";
    }

    @PostMapping("/salvar-empresa")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarEmpresa(@RequestParam String nome,
                                                            @RequestParam String nif,
                                                            @RequestParam String regimeFiscal,
                                                            @RequestParam(required = false) String telefone,
                                                            @RequestParam(required = false) String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa e = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
            e.setNome(nome);
            e.setNif(nif);
            e.setRegimeFiscal(regimeFiscal);
            if (telefone != null) e.setTelefone(telefone);
            if (email != null) e.setEmail(email);

            empresaRepository.save(e);
            response.put("sucesso", true);
            response.put("mensagem", "Dados da empresa guardados!");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/salvar-estabelecimento")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarEstabelecimento(@RequestParam String nome,
                                                                    @RequestParam String endereco,
                                                                    @RequestParam(required = false) String telefone) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

            List<Estabelecimento> existentes = estabelecimentoRepository.findByEmpresa_Id(empresaId);
            Estabelecimento est = existentes.isEmpty() ? new Estabelecimento() : existentes.get(0);
            
            est.setNome(nome);
            est.setEndereco(endereco);
            if (telefone != null) est.setTelefone(telefone);
            est.setEmpresa(empresa);

            estabelecimentoRepository.save(est);
            response.put("sucesso", true);
            response.put("mensagem", "Estabelecimento configurado com sucesso!");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/salvar-serie")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarSerie(@RequestParam String prefixo,
                                                           @RequestParam Integer ano,
                                                           @RequestParam(required = false) String descricao) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

            List<Serie> existentes = serieRepository.findByEmpresa_Id(empresaId);
            Serie s = existentes.isEmpty() ? new Serie() : existentes.get(0);
            
            s.setPrefixo(prefixo);
            s.setAno(ano);
            s.setProximoNumero(1);
            s.setActivo(true);
            s.setDescricao(descricao != null ? descricao : "Série padrão");
            s.setEmpresa(empresa);

            serieRepository.save(s);
            response.put("sucesso", true);
            response.put("mensagem", "Série de faturação configurada!");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/salvar-pagamentos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarPagamentos() {
        Map<String, Object> response = new HashMap<>();
        try {
            // Verificar se métodos padrão de pagamento já existem na BD
            List<MetodoPagamento> existentes = metodoPagamentoRepository.findAll();
            
            boolean temNumerario = existentes.stream().anyMatch(m -> "NU".equalsIgnoreCase(m.getCodigoAgt()));
            boolean temMulticaixa = existentes.stream().anyMatch(m -> "MC".equalsIgnoreCase(m.getCodigoAgt()));
            boolean temTransferencia = existentes.stream().anyMatch(m -> "TB".equalsIgnoreCase(m.getCodigoAgt()));

            if (!temNumerario) {
                MetodoPagamento nu = new MetodoPagamento();
                nu.setNome("Numerário");
                nu.setCodigoAgt("NU");
                nu.setActivo(true);
                metodoPagamentoRepository.save(nu);
            }

            if (!temMulticaixa) {
                MetodoPagamento mc = new MetodoPagamento();
                mc.setNome("Multicaixa");
                mc.setCodigoAgt("MC");
                mc.setActivo(true);
                metodoPagamentoRepository.save(mc);
            }

            if (!temTransferencia) {
                MetodoPagamento tb = new MetodoPagamento();
                tb.setNome("Transferência Bancária");
                tb.setCodigoAgt("TB");
                tb.setActivo(true);
                metodoPagamentoRepository.save(tb);
            }

            response.put("sucesso", true);
            response.put("mensagem", "Métodos de pagamento ativados!");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/obter-impostos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obterImpostos() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
            
            String regimeCodigo = empresa.getRegimeFiscal();
            RegimeFiscal regime = regimeFiscalRepository.findByCodigo(regimeCodigo).orElse(null);
            
            List<Imposto> todos = impostoRepository.findAll();
            List<Map<String, Object>> lista = new ArrayList<>();
            for (Imposto imp : todos) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", imp.getId());
                map.put("nome", imp.getNome());
                map.put("percentagem", imp.getPercentagem());
                map.put("codigoAgt", imp.getCodigoAgt());
                map.put("tipo", imp.getTipo());
                map.put("motivoIsencao", imp.getMotivoIsencao());
                map.put("associado", regime != null && regime.getImpostos().contains(imp));
                lista.add(map);
            }
            response.put("sucesso", true);
            response.put("impostos", lista);
            response.put("regimeNome", regime != null ? regime.getNome() : regimeCodigo);
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro ao obter impostos: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/salvar-novo-imposto")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarNovoImposto(@RequestParam String nome,
                                                                 @RequestParam BigDecimal percentagem,
                                                                 @RequestParam String codigoAgt,
                                                                 @RequestParam String tipo,
                                                                 @RequestParam(required = false) String motivoIsencao) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
            
            Imposto novo = new Imposto();
            novo.setNome(nome);
            novo.setPercentagem(percentagem);
            novo.setCodigoAgt(codigoAgt);
            novo.setTipo(tipo);
            novo.setMotivoIsencao(motivoIsencao != null ? motivoIsencao : "");
            
            impostoRepository.save(novo);
            
            // Associar ao regime da empresa
            String regimeCodigo = empresa.getRegimeFiscal();
            RegimeFiscal regime = regimeFiscalRepository.findByCodigo(regimeCodigo).orElse(null);
            if (regime != null) {
                regime.getImpostos().add(novo);
                regimeFiscalRepository.save(regime);
            }
            
            response.put("sucesso", true);
            response.put("mensagem", "Imposto registado e associado com sucesso!");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/salvar-impostos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarImpostos(@RequestParam(value = "impostoIds", required = false) List<Long> impostoIds) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            Empresa empresa = empresaRepository.findById(empresaId)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
            
            String regimeCodigo = empresa.getRegimeFiscal();
            RegimeFiscal regime = regimeFiscalRepository.findByCodigo(regimeCodigo).orElse(null);
            
            if (regime != null) {
                regime.getImpostos().clear();
                if (impostoIds != null && !impostoIds.isEmpty()) {
                    List<Imposto> impostosSelecionados = impostoRepository.findAllById(impostoIds);
                    regime.getImpostos().addAll(impostosSelecionados);
                }
                regimeFiscalRepository.save(regime);
            }
            
            response.put("sucesso", true);
            response.put("mensagem", "Impostos e regras fiscais configurados!");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/salvar-agt")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> salvarAgt(@RequestParam String urlServico,
                                                         @RequestParam String usuario,
                                                         @RequestParam String senha,
                                                         @RequestParam(required = false) String certificado) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            configuracaoEmpresaService.atualizarConfiguracaoAGT(empresaId, true, urlServico, usuario, senha, certificado != null ? certificado : "");
            
            response.put("sucesso", true);
            response.put("mensagem", "Configurações de integração AGT salvas!");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/finalizar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> finalizar() {
        Map<String, Object> response = new HashMap<>();
        try {
            Long empresaId = SecurityUtils.getCurrentEmpresaId();
            ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresaId);
            config.setSetupCompleto(true);
            configuracaoEmpresaService.salvarConfiguracao(config);

            response.put("sucesso", true);
            response.put("mensagem", "Configuração concluída com sucesso! Bem-vindo ao Kwanza ERP.");
        } catch (Exception err) {
            response.put("sucesso", false);
            response.put("mensagem", "Erro: " + err.getMessage());
        }
        return ResponseEntity.ok(response);
    }
}
