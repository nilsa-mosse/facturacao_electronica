package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.service.DespesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/despesas")
public class DespesaController {

    @Autowired
    private DespesaService despesaService;

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
    public String cadastrarDespesa(@ModelAttribute Despesa despesa) {
        despesaService.salvar(despesa);
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
    public String atualizarDespesa(@ModelAttribute Despesa despesa) {
        despesaService.atualizar(despesa);
        return "redirect:/despesas/listar";
    }

    // Excluir despesa
    @GetMapping("/excluir/{id}")
    public String excluirDespesa(@PathVariable Long id) {
        despesaService.excluir(id);
        return "redirect:/despesas/listar";
    }
}
