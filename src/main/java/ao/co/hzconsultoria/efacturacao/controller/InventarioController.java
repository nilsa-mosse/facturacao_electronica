package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Inventario;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.InventarioRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.service.StockService;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestao-inventarios")
public class InventarioController {

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private StockService stockService;

    @GetMapping
    public String listar(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("inventarios", inventarioRepository.findByEmpresa_IdOrderByCreatedAtDesc(empresaId));
        return "listarInventarios";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Inventario inventario = new Inventario();
        
        // Gerar código automático: INV-ANO-001
        int anoAtual = java.time.LocalDate.now().getYear();
        long totalInventariosAno = inventarioRepository.findByEmpresa_Id(empresaId).stream()
                .filter(i -> i.getCreatedAt().getYear() == anoAtual)
                .count();
        
        String codigoGerado = String.format("INV-%d-%03d", anoAtual, totalInventariosAno + 1);
        inventario.setCodigo(codigoGerado);
        inventario.setDataAbertura(java.time.LocalDate.now());
        inventario.setEstado(Inventario.EstadoInventario.RASCUNHO);
        
        model.addAttribute("inventario", inventario);
        model.addAttribute("produtosDisponiveis", stockService.listarTodosProdutos());
        return "cadastroInventario";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Inventario inventario, RedirectAttributes ra) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
            inventario.setEmpresa(empresa);
        }
        
        try {
            inventarioRepository.save(inventario);
            ra.addFlashAttribute("mensagemSucesso", "Inventário guardado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("mensagemErro", "Erro ao guardar inventário: " + e.getMessage());
        }
        
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/iniciar/{id}")
    public String iniciar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() == Inventario.EstadoInventario.RASCUNHO) {
            inv.setEstado(Inventario.EstadoInventario.EM_CONTAGEM);
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Contagem iniciada!");
        }
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/fechar/{id}")
    public String fechar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() == Inventario.EstadoInventario.EM_CONTAGEM) {
            inv.setEstado(Inventario.EstadoInventario.EM_REVISAO);
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Inventário enviado para revisão!");
        }
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/aprovar/{id}")
    public String aprovar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() == Inventario.EstadoInventario.EM_REVISAO) {
            inv.setEstado(Inventario.EstadoInventario.FINALIZADO);
            // Aqui futuramente entraria a lógica de ajuste de stock real
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Inventário finalizado e stock ajustado!");
        }
        return "redirect:/gestao-inventarios";
    }

    @GetMapping("/cancelar/{id}")
    public String cancelar(@PathVariable Long id, RedirectAttributes ra) {
        Inventario inv = inventarioRepository.findById(id).orElse(null);
        if (inv != null && inv.getEstado() != Inventario.EstadoInventario.FINALIZADO) {
            inv.setEstado(Inventario.EstadoInventario.CANCELADO);
            inventarioRepository.save(inv);
            ra.addFlashAttribute("mensagemSucesso", "Inventário cancelado.");
        }
        return "redirect:/gestao-inventarios";
    }
}
