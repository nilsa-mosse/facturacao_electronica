package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Fornecedor;
import ao.co.hzconsultoria.efacturacao.service.FornecedorService;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fornecedores")
public class FornecedorController {

    @Autowired
    private FornecedorService fornecedorService;

    @Autowired
    private MessageSource messageSource;

    @GetMapping("/listar")
    public String listarFornecedores(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("fornecedores", fornecedorService.listarTodos(empresaId));
        return "listarFornecedores";
    }

    @GetMapping("/adicionar")
    public String mostrarFormularioCadastro(Model model) {
        model.addAttribute("fornecedor", new Fornecedor());
        return "cadastroFornecedor";
    }

    @PostMapping("/adicionar")
    public String cadastrarFornecedor(@ModelAttribute Fornecedor fornecedor, RedirectAttributes redirectAttributes) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        fornecedorService.salvar(fornecedor, empresaId);
        redirectAttributes.addFlashAttribute("mensagem", "Fornecedor cadastrado com sucesso!");
        return "redirect:/fornecedores/listar";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicao(@PathVariable Long id, Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Fornecedor fornecedor = fornecedorService.buscarPorId(id, empresaId);
        if (fornecedor == null) {
            return "redirect:/fornecedores/listar";
        }
        model.addAttribute("fornecedor", fornecedor);
        return "cadastroEdicaoFornecedor";
    }

    @PostMapping("/atualizar")
    public String atualizarFornecedor(@ModelAttribute Fornecedor fornecedor, RedirectAttributes redirectAttributes) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        fornecedorService.atualizar(fornecedor, empresaId);
        redirectAttributes.addFlashAttribute("mensagem", "Fornecedor atualizado com sucesso!");
        return "redirect:/fornecedores/listar";
    }

    @GetMapping("/delete/{id}")
    public String excluirFornecedor(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        fornecedorService.excluir(id, empresaId);
        redirectAttributes.addFlashAttribute("mensagem", "Fornecedor removido com sucesso!");
        return "redirect:/fornecedores/listar";
    }
}
