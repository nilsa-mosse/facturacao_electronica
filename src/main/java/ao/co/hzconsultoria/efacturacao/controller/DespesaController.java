package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.service.DespesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/despesas")
public class DespesaController {

    @Autowired
    private DespesaService despesaService;

    @Autowired
    private MessageSource messageSource;

    // Listar despesas
    @GetMapping("/listar")
    public String listarDespesas(Model model) {
        model.addAttribute("despesas", despesaService.listarTodas());
        return "listarDespesas";
    }

    // Exibir formulário de cadastro
    @GetMapping("/adicionar")
    public String mostrarFormularioCadastro(Model model) {
        model.addAttribute("despesa", new Despesa());
        return "cadastroDespesa";
    }

    // Processar cadastro
    @PostMapping("/adicionar")
    public String cadastrarDespesa(@ModelAttribute Despesa despesa, RedirectAttributes redirectAttributes) {
        despesaService.salvar(despesa);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.despesa.salvo", null, LocaleContextHolder.getLocale()));
        return "redirect:/despesas/listar";
    }

    // Exibir formulário de edição
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicao(@PathVariable Long id, Model model) {
        Despesa despesa = despesaService.buscarPorId(id);
        model.addAttribute("despesa", despesa);
        return "cadastroDespesa";
    }

    // Atualizar despesa
    @PostMapping("/atualizar")
    public String atualizarDespesa(@ModelAttribute Despesa despesa, RedirectAttributes redirectAttributes) {
        despesaService.atualizar(despesa);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.despesa.atualizada", null, LocaleContextHolder.getLocale()));
        return "redirect:/despesas/listar";
    }

    // Excluir despesa
    @GetMapping("/excluir/{id}")
    public String excluirDespesa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        despesaService.excluir(id);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.despesa.apagada", null, LocaleContextHolder.getLocale()));
        return "redirect:/despesas/listar";
    }
}
