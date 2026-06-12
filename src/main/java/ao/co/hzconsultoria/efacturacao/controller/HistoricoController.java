package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import ao.co.hzconsultoria.efacturacao.service.VendaService;
import ao.co.hzconsultoria.efacturacao.service.FaturaService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.repository.FaturaRepository;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

@Controller
public class HistoricoController {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ao.co.hzconsultoria.efacturacao.service.DynamicMailService dynamicMailService;

    @Autowired
    private FaturaService faturaService;

    @GetMapping("/historico-vendas")
    public String historicoVendas(
            Model model,
            @RequestParam(value = "dataInicio", required = false) String dataInicio,
            @RequestParam(value = "dataFim", required = false) String dataFim,
            @RequestParam(value = "pesquisa", required = false) String pesquisa,
            @RequestParam(value = "tipo", required = false, defaultValue = "emitidas") String tipo,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page) {
        int pageSize = 10;
        List<Compra> todasAsCompras = compraRepository.findAll();

        // Restrição por Utilizador (Operadores só vêm o seu histórico)
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));

        if (!isAdmin && auth.getPrincipal() instanceof ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) {
            Long userId = ((ao.co.hzconsultoria.efacturacao.security.CustomUserDetails) auth.getPrincipal()).getId();
            todasAsCompras = todasAsCompras.stream()
                    .filter(c -> c.getUsuario() != null && c.getUsuario().getId().equals(userId))
                    .collect(Collectors.toList());
        }

        // Estatísticas para os cards
        long totalVendas = todasAsCompras.size();
        long emitidasCount = todasAsCompras.stream().filter(c -> "EMITIDA".equals(c.getStatus())).count();
        long substituidasCount = todasAsCompras.stream().filter(c -> "SUBSTITUIDA".equals(c.getStatus())).count();
        long canceladasCount = todasAsCompras.stream().filter(c -> "CANCELADA".equals(c.getStatus())).count();

        List<Compra> filtradas = todasAsCompras;

        // Filtro por tipo (Tipo de Documento ou Status)
        if (!"todos".equalsIgnoreCase(tipo)) {
            if ("CANCELADA".equalsIgnoreCase(tipo) || "canceladas".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                        .filter(c -> "CANCELADA".equals(c.getStatus()))
                        .collect(Collectors.toList());
            } else if ("SUBSTITUIDA".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                        .filter(c -> "SUBSTITUIDA".equals(c.getStatus()))
                        .collect(Collectors.toList());
            } else if ("EMITIDA".equalsIgnoreCase(tipo) || "emitidas".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                        .filter(c -> "EMITIDA".equals(c.getStatus()))
                        .collect(Collectors.toList());
            } else if ("FT".equalsIgnoreCase(tipo)) {
                // Se pedir explicitamente FT, mostramos as que não são Pro-formas, de
                // preferência emitidas
                filtradas = filtradas.stream()
                        .filter(c -> !"FP".equals(c.getTipoDocumento()))
                        .collect(Collectors.toList());
            } else if ("FP".equalsIgnoreCase(tipo)) {
                filtradas = filtradas.stream()
                        .filter(c -> "FP".equals(c.getTipoDocumento()))
                        .collect(Collectors.toList());
            }
        }

        // Filtro por data
        if (dataInicio != null && !dataInicio.isEmpty()) {
            LocalDate inicio = LocalDate.parse(dataInicio);
            filtradas = filtradas.stream()
                    .filter(c -> c.getDataCompra() != null && !c.getDataCompra().toLocalDate().isBefore(inicio))
                    .collect(Collectors.toList());
        }
        if (dataFim != null && !dataFim.isEmpty()) {
            LocalDate fim = LocalDate.parse(dataFim);
            filtradas = filtradas.stream()
                    .filter(c -> c.getDataCompra() != null && !c.getDataCompra().toLocalDate().isAfter(fim))
                    .collect(Collectors.toList());
        }
        // Filtro por pesquisa
        if (pesquisa != null && !pesquisa.isEmpty()) {
            String pesq = pesquisa.toLowerCase();
            filtradas = filtradas.stream().filter(c -> String.valueOf(c.getId()).contains(pesq) ||
                    (c.getItens() != null && c.getItens().stream().anyMatch(
                            i -> i.getNomeProduto() != null && i.getNomeProduto().toLowerCase().contains(pesq))))
                    .collect(Collectors.toList());
        }
        // Paginação manual
        int totalPages = (int) Math.ceil((double) filtradas.size() / pageSize);
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(fromIndex + pageSize, filtradas.size());
        List<Compra> compras = filtradas.subList(fromIndex, toIndex);

        // Obter os IDs das compras cujas faturas foram enviadas para a AGT
        List<Long> comprasEnviadasAgt = faturaRepository.findAll().stream()
                .filter(f -> f.getCompra() != null && Boolean.TRUE.equals(f.isEnviadaAGT()))
                .map(f -> f.getCompra().getId())
                .collect(Collectors.toList());

        model.addAttribute("compras", compras);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalVendas", totalVendas);
        model.addAttribute("emitidasCount", emitidasCount);
        model.addAttribute("substituidasCount", substituidasCount);
        model.addAttribute("canceladasCount", canceladasCount);
        model.addAttribute("dataInicio", dataInicio);
        model.addAttribute("dataFim", dataFim);
        model.addAttribute("pesquisa", pesquisa);
        model.addAttribute("tipo", tipo);
        model.addAttribute("isAdmin", isAdmin); // Passar permissão para o template
        model.addAttribute("comprasEnviadasAgt", comprasEnviadasAgt);
        return "historicoVendas";
    }

    @PostMapping("/historico-vendas/anular")
    public String anularVenda(
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "motivo", required = false) String motivo,
            RedirectAttributes redirectAttributes) {

        // Validação de Segurança
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));
        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Acesso negado: Apenas administradores podem anular facturas.");
            return "redirect:/historico-vendas";
        }

        if (id == null || motivo == null || motivo.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Dados inválidos para anulação.");
            return "redirect:/historico-vendas";
        }

        // Bloqueio rígido se enviada para a AGT
        boolean enviadaAgt = faturaRepository.findAll().stream()
                .anyMatch(f -> f.getCompra() != null && f.getCompra().getId().equals(id)
                        && Boolean.TRUE.equals(f.isEnviadaAGT()));
        if (enviadaAgt) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Esta fatura já foi enviada para a AGT e não pode ser anulada, rectificada ou alterada.");
            return "redirect:/historico-vendas";
        }

        if (vendaService.cancelarVenda(id, motivo)) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura anulada com sucesso.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao anular factura.");
        }
        return "redirect:/historico-vendas?tipo=todos";
    }

    @GetMapping("/historico-vendas/rectificar/{id}")
    public String prepararRectificacao(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        // Validação de Segurança
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));
        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Acesso negado: Apenas administradores podem rectificar facturas.");
            return "redirect:/historico-vendas";
        }

        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent()) {
            Compra original = compraOpt.get();

            // Bloqueio rígido se enviada para a AGT
            boolean enviadaAgt = faturaRepository.findAll().stream()
                    .anyMatch(f -> f.getCompra() != null && f.getCompra().getId().equals(id)
                            && Boolean.TRUE.equals(f.isEnviadaAGT()));
            if (enviadaAgt) {
                redirectAttributes.addFlashAttribute("mensagemErro",
                        "Esta fatura já foi enviada para a AGT e não pode ser anulada, rectificada ou alterada.");
                return "redirect:/historico-vendas";
            }

            Compra nova = new Compra();
            nova.setCliente(original.getCliente());
            nova.setFaturaReferencia(original);
            nova.setNomeCliente(original.getNomeCliente());
            nova.setNifCliente(original.getNifCliente());
            nova.setFormaPagamento(original.getFormaPagamento());

            // Clonar itens
            List<ItemCompra> novosItens = original.getItens().stream().map(i -> {
                ItemCompra ni = new ItemCompra();
                ni.setNomeProduto(i.getNomeProduto());
                ni.setQuantidade(i.getQuantidade());
                ni.setPreco(i.getPreco());
                ni.setSubtotal(i.getSubtotal());
                ni.setIva(i.getIva());
                return ni;
            }).collect(Collectors.toList());
            nova.setItens(novosItens);

            model.addAttribute("compra", nova);
            model.addAttribute("clientes", clienteRepository.findAll());

            return "rectificarFactura";
        }
        redirectAttributes.addFlashAttribute("mensagemErro", "Factura não encontrada.");
        return "redirect:/historico-vendas";
    }

    @PostMapping("/historico-vendas/salvar-rectificacao")
    public String salvarRectificacao(Compra compra, RedirectAttributes redirectAttributes) {
        try {
            // Bloqueio rígido se enviada para a AGT
            if (compra.getFaturaReferencia() != null) {
                Long originalId = compra.getFaturaReferencia().getId();
                boolean enviadaAgt = faturaRepository.findAll().stream()
                        .anyMatch(f -> f.getCompra() != null && f.getCompra().getId().equals(originalId)
                                && Boolean.TRUE.equals(f.isEnviadaAGT()));
                if (enviadaAgt) {
                    redirectAttributes.addFlashAttribute("mensagemErro",
                            "Esta fatura já foi enviada para a AGT e não pode ser anulada, rectificada ou alterada.");
                    return "redirect:/historico-vendas";
                }
            }

            // 1. Marcar a original como SUBSTITUIDA
            if (compra.getFaturaReferencia() != null) {
                Compra original = compraRepository.findById(compra.getFaturaReferencia().getId()).orElse(null);
                if (original != null) {
                    original.setStatus("SUBSTITUIDA");
                    compraRepository.save(original);
                }
            }

            // 2. Salvar a nova factura
            vendaService.finalizarVenda(compra, "FT");
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura rectificada com sucesso!");
            return "redirect:/historico-vendas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao rectificar factura: " + e.getMessage());
            return "redirect:/historico-vendas";
        }
    }

    @GetMapping("/historico-vendas/restaurar/{id}")
    public String restaurarVenda(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        // Validação de Segurança
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));
        if (!isAdmin) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Acesso negado: Apenas administradores podem restaurar facturas.");
            return "redirect:/historico-vendas";
        }

        // Bloqueio rígido se enviada para a AGT
        boolean enviadaAgt = faturaRepository.findAll().stream()
                .anyMatch(f -> f.getCompra() != null && f.getCompra().getId().equals(id)
                        && Boolean.TRUE.equals(f.isEnviadaAGT()));
        if (enviadaAgt) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Esta fatura já foi enviada para a AGT e não pode ser anulada, rectificada ou alterada.");
            return "redirect:/historico-vendas";
        }

        if (vendaService.restaurarVenda(id)) {
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Factura restaurada com sucesso.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Erro ao restaurar factura.");
        }
        return "redirect:/historico-vendas?tipo=canceladas";
    }

    @GetMapping("/api/compras/fatura-url/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> obterFaturaUrl(@PathVariable Long id) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent()) {
            List<Fatura> faturas = faturaRepository.findAll().stream()
                    .filter(f -> f.getCompra() != null && f.getCompra().getId().equals(id))
                    .sorted((f1, f2) -> f2.getId().compareTo(f1.getId()))
                    .collect(Collectors.toList());

            if (!faturas.isEmpty()) {
                Map<String, String> res = new HashMap<>();
                res.put("url", "/uploads/faturas/" + faturas.get(0).getNumeroFatura() + ".pdf");
                return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(res);
            }
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/historico-vendas/proforma/aprovar/{id}")
    public String aprovarProforma(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent() && "FP".equals(compraOpt.get().getTipoDocumento())) {
            Compra compra = compraOpt.get();
            compra.setStatus("APROVADA");
            compraRepository.save(compra);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Pro-forma aprovada com sucesso.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Pro-forma não encontrada.");
        }
        return "redirect:/historico-vendas?tipo=FP";
    }

    @GetMapping("/historico-vendas/proforma/rejeitar/{id}")
    public String rejeitarProforma(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent() && "FP".equals(compraOpt.get().getTipoDocumento())) {
            Compra compra = compraOpt.get();
            compra.setStatus("REJEITADA");
            compraRepository.save(compra);
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Pro-forma rejeitada.");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Pro-forma não encontrada.");
        }
        return "redirect:/historico-vendas?tipo=FP";
    }

    @GetMapping("/historico-vendas/proforma/duplicar/{id}")
    public String duplicarProforma(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent() && "FP".equals(compraOpt.get().getTipoDocumento())) {
            Compra original = compraOpt.get();
            Compra nova = new Compra();
            nova.setCliente(original.getCliente());
            nova.setNomeCliente(original.getNomeCliente());
            nova.setNifCliente(original.getNifCliente());
            nova.setMoradaCliente(original.getMoradaCliente());
            nova.setTelefoneCliente(original.getTelefoneCliente());
            nova.setEmailCliente(original.getEmailCliente());
            nova.setFormaPagamento(original.getFormaPagamento());
            nova.setEmpresa(original.getEmpresa());
            nova.setUsuario(original.getUsuario());
            nova.setStatus("EMITIDA");

            List<ItemCompra> novosItens = original.getItens().stream().map(i -> {
                ItemCompra ni = new ItemCompra();
                ni.setNomeProduto(i.getNomeProduto());
                ni.setQuantidade(i.getQuantidade());
                ni.setPreco(i.getPreco());
                ni.setSubtotal(i.getSubtotal());
                ni.setIva(i.getIva());
                ni.setIvaPercentual(i.getIvaPercentual());
                ni.setProduto(i.getProduto());
                ni.setCompra(nova);
                return ni;
            }).collect(Collectors.toList());
            nova.setItens(novosItens);

            vendaService.finalizarVenda(nova, "FP");
            redirectAttributes.addFlashAttribute("mensagemSucesso", "Pro-forma duplicada com sucesso!");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Pro-forma não encontrada.");
        }
        return "redirect:/historico-vendas?tipo=FP";
    }

    @GetMapping("/historico-vendas/proforma/converter/{id}")
    public String converterProforma(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent() && "FP".equals(compraOpt.get().getTipoDocumento())) {
            Compra compra = compraOpt.get();
            if ("CONVERTIDA".equals(compra.getStatus())) {
                redirectAttributes.addFlashAttribute("mensagemErro", "Esta Pro-forma já foi convertida.");
                return "redirect:/historico-vendas?tipo=FP";
            }
            compra.setStatus("CONVERTIDA");
            compraRepository.save(compra);

            Compra nova = new Compra();
            nova.setCliente(compra.getCliente());
            nova.setNomeCliente(compra.getNomeCliente());
            nova.setNifCliente(compra.getNifCliente());
            nova.setMoradaCliente(compra.getMoradaCliente());
            nova.setTelefoneCliente(compra.getTelefoneCliente());
            nova.setEmailCliente(compra.getEmailCliente());
            nova.setFormaPagamento(compra.getFormaPagamento());
            nova.setEmpresa(compra.getEmpresa());
            nova.setUsuario(compra.getUsuario());
            nova.setStatus("EMITIDA");
            nova.setFaturaReferencia(compra);

            List<ItemCompra> novosItens = compra.getItens().stream().map(i -> {
                ItemCompra ni = new ItemCompra();
                ni.setNomeProduto(i.getNomeProduto());
                ni.setQuantidade(i.getQuantidade());
                ni.setPreco(i.getPreco());
                ni.setSubtotal(i.getSubtotal());
                ni.setIva(i.getIva());
                ni.setIvaPercentual(i.getIvaPercentual());
                ni.setProduto(i.getProduto());
                ni.setCompra(nova);
                return ni;
            }).collect(Collectors.toList());
            nova.setItens(novosItens);

            vendaService.finalizarVenda(nova, "FT");
            redirectAttributes.addFlashAttribute("mensagemSucesso",
                    "Pro-forma convertida em Factura (FT) com sucesso!");
        } else {
            redirectAttributes.addFlashAttribute("mensagemErro", "Pro-forma não encontrada.");
        }
        return "redirect:/historico-vendas";
    }

    @PostMapping("/historico-vendas/proforma/enviar-email")
    @ResponseBody
    public ResponseEntity<?> enviarEmail(
            @RequestParam("id") Long id,
            @RequestParam("email") String email) {
        try {
            Optional<Compra> compraOpt = compraRepository.findById(id);
            if (!compraOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Venda não encontrada.");
            }
            Compra compra = compraOpt.get();
            List<Fatura> faturas = faturaRepository.findByCompra(compra);
            if (faturas.isEmpty()) {
                return ResponseEntity.badRequest().body("Documento PDF não gerado.");
            }
            Fatura fatura = faturas.get(0);
            String pdfPath = "./uploads/faturas/" + fatura.getNumeroFatura() + ".pdf";
            java.io.File file = new java.io.File(pdfPath);
            if (!file.exists()) {
                file = new java.io.File("src/main/resources/static/uploads/faturas/" + fatura.getNumeroFatura() + ".pdf");
            }
            if (!file.exists()) {
                file = new java.io.File("target/classes/static/uploads/faturas/" + fatura.getNumeroFatura() + ".pdf");
            }
            if (!file.exists()) {
                return ResponseEntity.badRequest().body("Arquivo PDF não encontrado no servidor.");
            }

            Long empresaId = compra.getEmpresa() != null ? compra.getEmpresa().getId() : null;
            String assunto = "Documento Pro-forma #" + compra.getId();
            String corpo = "<p>Olá,</p><p>Segue em anexo o seu documento Pro-forma no valor de " + compra.getTotal()
                    + " Kz.</p><p>Obrigado!</p>";
            dynamicMailService.enviarEmailComAnexo(empresaId, email, assunto, corpo, fatura.getNumeroFatura() + ".pdf",
                    file);

            return ResponseEntity.ok("Email enviado com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erro ao enviar email: " + e.getMessage());
        }
    }

    @GetMapping("/api/compras/proforma/{id}")
    @ResponseBody
    public ResponseEntity<?> obterProformaDetails(@PathVariable Long id) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (compraOpt.isPresent() && "FP".equals(compraOpt.get().getTipoDocumento())) {
            Compra compra = compraOpt.get();
            Map<String, Object> details = new HashMap<>();
            details.put("id", compra.getId());
            details.put("nomeCliente", compra.getNomeCliente());
            details.put("nifCliente", compra.getNifCliente());
            details.put("moradaCliente", compra.getMoradaCliente());
            details.put("telefoneCliente", compra.getTelefoneCliente());
            details.put("emailCliente", compra.getEmailCliente());
            details.put("clienteId", compra.getCliente() != null ? compra.getCliente().getId() : null);

            List<Map<String, Object>> itens = compra.getItens().stream().map(i -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("produtoId", i.getProduto() != null ? i.getProduto().getId() : null);
                itemMap.put("nomeProduto", i.getNomeProduto());
                itemMap.put("preco", i.getPreco());
                itemMap.put("quantidade", i.getQuantidade());
                itemMap.put("ivaPercentual", i.getIvaPercentual());
                itemMap.put("stockMax", i.getProduto() != null ? i.getProduto().getQuantidadeEstoque() : 999999);
                return itemMap;
            }).collect(Collectors.toList());

            details.put("itens", itens);
            return ResponseEntity.ok(details);
        }
        return ResponseEntity.notFound().build();
    }

    // =========================================================
    // CICLO DE VIDA FT
    // =========================================================

    /** Regista pagamento parcial ou total de uma FT */
    @PostMapping("/historico-vendas/registar-pagamento")
    public String registarPagamento(
            @RequestParam("id") Long id,
            @RequestParam("valorPago") Double valorPago,
            @RequestParam(value = "metodoPagamento", required = false) String metodoPagamento,
            @RequestParam(value = "dataVencimento", required = false) String dataVencimento,
            RedirectAttributes redirectAttributes) {

        Optional<Compra> opt = compraRepository.findById(id);
        if (!opt.isPresent()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Factura não encontrada.");
            return "redirect:/historico-vendas";
        }
        Compra compra = opt.get();
        if ("VALIDADA_AGT".equals(compra.getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Factura validada pela AGT não pode ser alterada.");
            return "redirect:/historico-vendas";
        }
        if ("ANULADA".equals(compra.getStatus()) || "CANCELADA".equals(compra.getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Factura anulada não pode registar pagamento.");
            return "redirect:/historico-vendas";
        }

        double acumulado = (compra.getValorPago() != null ? compra.getValorPago() : 0.0) + valorPago;
        compra.setValorPago(acumulado);
        if (metodoPagamento != null && !metodoPagamento.isEmpty()) {
            compra.setMetodoPagamentoRegistado(metodoPagamento);
        }
        if (dataVencimento != null && !dataVencimento.isEmpty()) {
            compra.setDataVencimento(LocalDate.parse(dataVencimento));
        }
        double total = compra.getTotal() != null ? compra.getTotal() : 0.0;
        if (acumulado >= total) {
            compra.setStatus("PAGA");
            compra.setDataPagamento(LocalDateTime.now());
            redirectAttributes.addFlashAttribute("mensagemSucesso",
                    "Pagamento total registado. Factura marcada como PAGA.");
        } else {
            compra.setStatus("PARCIALMENTE_PAGA");
            redirectAttributes.addFlashAttribute("mensagemSucesso",
                    String.format("Pagamento parcial de %.2f Kz registado.", valorPago));
        }
        compraRepository.save(compra);
        return "redirect:/historico-vendas";
    }

    /**
     * Converte FT em Factura-Recibo (FR) directamente:
     * - Localiza a Fatura FT associada à Compra
     * - Chama FaturaService.converterParaFaturaRecibo que:
     * 1. Marca a FT original como PAGA
     * 2. Marca a Compra original como PAGA
     * 3. Emite nova Fatura do tipo FR
     */
    @GetMapping("/historico-vendas/converter-fr/{id}")
    public String converterParaFR(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Compra> opt = compraRepository.findById(id);
        if (!opt.isPresent()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Factura não encontrada.");
            return "redirect:/historico-vendas";
        }
        Compra compra = opt.get();

        // Validação de estado
        if ("VALIDADA_AGT".equals(compra.getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Factura validada pela AGT não pode ser convertida.");
            return "redirect:/historico-vendas";
        }
        if (!"EMITIDA".equals(compra.getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Apenas facturas com estado 'Emitida' podem ser convertidas para Factura-Recibo.");
            return "redirect:/historico-vendas";
        }
        if (!"FT".equals(compra.getTipoDocumento())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Apenas documentos do tipo FT podem ser convertidos.");
            return "redirect:/historico-vendas";
        }

        // Localizar a Fatura FT correspondente à Compra
        List<Fatura> faturasFT = faturaRepository.findAll().stream()
                .filter(f -> f.getCompra() != null && f.getCompra().getId().equals(id)
                        && "FT".equals(f.getTipoDocumento()))
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());

        if (faturasFT.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Não foi encontrada uma Factura (FT) gerada para esta venda.");
            return "redirect:/historico-vendas";
        }

        try {
            Fatura fr = faturaService.converterParaFaturaRecibo(faturasFT.get(0).getId());
            redirectAttributes.addFlashAttribute("mensagemSucesso",
                    "Factura convertida para Factura-Recibo com sucesso! FR gerada: " + fr.getNumeroFatura());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensagemErro",
                    "Erro ao converter para Factura-Recibo: " + e.getMessage());
        }
        return "redirect:/historico-vendas";
    }

    /** Redirige para emissão de Nota de Crédito com factura pré-seleccionada */
    @GetMapping("/historico-vendas/nota-credito/{id}")
    public String emitirNotaCredito(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Compra> opt = compraRepository.findById(id);
        if (!opt.isPresent()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Factura não encontrada.");
            return "redirect:/historico-vendas";
        }
        if ("ANULADA".equals(opt.get().getStatus()) || "CANCELADA".equals(opt.get().getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível emitir nota sobre factura anulada.");
            return "redirect:/historico-vendas";
        }
        return "redirect:/notas/novo?facturaId=" + id + "&tipo=NC";
    }

    /** Redirige para emissão de Nota de Débito com factura pré-seleccionada */
    @GetMapping("/historico-vendas/nota-debito/{id}")
    public String emitirNotaDebito(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Compra> opt = compraRepository.findById(id);
        if (!opt.isPresent()) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Factura não encontrada.");
            return "redirect:/historico-vendas";
        }
        if ("ANULADA".equals(opt.get().getStatus()) || "CANCELADA".equals(opt.get().getStatus())) {
            redirectAttributes.addFlashAttribute("mensagemErro", "Não é possível emitir nota sobre factura anulada.");
            return "redirect:/historico-vendas";
        }
        return "redirect:/notas/novo?facturaId=" + id + "&tipo=ND";
    }

    /** Retorna estado AGT da factura (dados locais) */
    @GetMapping("/api/compras/estado-agt/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> consultarEstadoAgt(@PathVariable Long id) {
        Optional<Compra> compraOpt = compraRepository.findById(id);
        if (!compraOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<Fatura> faturas = faturaRepository.findAll().stream()
                .filter(f -> f.getCompra() != null && f.getCompra().getId().equals(id))
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .collect(Collectors.toList());
        Map<String, Object> res = new HashMap<>();
        res.put("facturaId", id);
        res.put("status", compraOpt.get().getStatus());
        if (!faturas.isEmpty()) {
            Fatura f = faturas.get(0);
            res.put("numeroFatura", f.getNumeroFatura());
            res.put("enviadaAGT", Boolean.TRUE.equals(f.isEnviadaAGT()));
            res.put("codigoAGT", f.getCodigoAgt());
            res.put("hash",
                    f.getHash() != null ? f.getHash().substring(0, Math.min(8, f.getHash().length())) + "..." : null);
            res.put("dataEmissao", f.getDataEmissao());
        } else {
            res.put("enviadaAGT", false);
            res.put("codigoAGT", null);
        }
        return ResponseEntity.ok(res);
    }

    /** Envia FT por email */
    @PostMapping("/historico-vendas/enviar-email-ft")
    @ResponseBody
    public ResponseEntity<?> enviarEmailFT(
            @RequestParam("id") Long id,
            @RequestParam("email") String email) {
        try {
            Optional<Compra> compraOpt = compraRepository.findById(id);
            if (!compraOpt.isPresent())
                return ResponseEntity.badRequest().body("Venda não encontrada.");
            Compra compra = compraOpt.get();
            List<Fatura> faturas = faturaRepository.findByCompra(compra);
            if (faturas.isEmpty())
                return ResponseEntity.badRequest().body("Documento PDF não gerado.");
            Fatura fatura = faturas.get(0);
            String pdfPath = "./uploads/faturas/" + fatura.getNumeroFatura() + ".pdf";
            java.io.File file = new java.io.File(pdfPath);
            if (!file.exists()) {
                file = new java.io.File("src/main/resources/static/uploads/faturas/" + fatura.getNumeroFatura() + ".pdf");
            }
            if (!file.exists()) {
                file = new java.io.File("target/classes/static/uploads/faturas/" + fatura.getNumeroFatura() + ".pdf");
            }
            if (!file.exists())
                return ResponseEntity.badRequest().body("PDF não encontrado no servidor.");
            Long empresaId = compra.getEmpresa() != null ? compra.getEmpresa().getId() : null;
            String assunto = "Factura #" + compra.getId();
            String corpo = "<p>Olá,</p><p>Segue em anexo a Factura no valor de " + compra.getTotal()
                    + " Kz.</p><p>Obrigado!</p>";
            dynamicMailService.enviarEmailComAnexo(empresaId, email, assunto, corpo, fatura.getNumeroFatura() + ".pdf",
                    file);
            return ResponseEntity.ok("Email enviado com sucesso!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro ao enviar email: " + e.getMessage());
        }
    }
}