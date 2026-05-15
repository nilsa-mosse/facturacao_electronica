package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.service.DevolucaoService;
import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/devolucoes")
public class DevolucaoController {

    @Autowired
    private DevolucaoService devolucaoService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping
    public String listarDevolucoes(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("devolucoes", devolucaoService.listarTodas(empresaId));
        return "listarDevolucoes";
    }

    @GetMapping("/novo")
    public String novoFormulario(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        List<Produto> produtos = produtoRepository.findByEmpresa_Id(empresaId);
        
        model.addAttribute("devolucao", new Devolucao());
        model.addAttribute("produtos", produtos);
        return "cadastroDevolucao";
    }

    @PostMapping("/salvar")
    public String salvar(@ModelAttribute Devolucao devolucao, RedirectAttributes redirectAttributes) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Long usuarioId = SecurityUtils.getCurrentUserId();
        
        try {
            // Remover itens nulos (caso o JS envie algum vazio)
            if (devolucao.getItens() != null) {
                devolucao.getItens().removeIf(item -> item.getProduto() == null || item.getProduto().getId() == null);
            }

            if (devolucao.getItens() == null || devolucao.getItens().isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Adicione pelo menos um produto para devolver.");
                return "redirect:/devolucoes/novo";
            }

            devolucaoService.registrarDevolucao(devolucao, empresaId, usuarioId);
            redirectAttributes.addFlashAttribute("mensagem", "Devolução registrada com sucesso! Stock atualizado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao registrar devolução: " + e.getMessage());
            return "redirect:/devolucoes/novo";
        }
        
        return "redirect:/devolucoes";
    }
}