package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Caixa;
import ao.co.hzconsultoria.efacturacao.service.CaixaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/caixa")
public class CaixaController {

    @Autowired
    private CaixaService caixaService;

    @GetMapping("/abertura")
    public String telaAbertura(Model model) {
        if (caixaService.isCaixaAberto()) {
            return "redirect:/pos"; // Já tem caixa aberto, vai direto pro POS
        }
        return "caixa/abertura";
    }

    @PostMapping("/abrir")
    public String abrirCaixa(@RequestParam(value = "saldoInicial", defaultValue = "0") Double saldoInicial,
                             @RequestParam(value = "observacoes", required = false) String observacoes,
                             RedirectAttributes ra) {
        try {
            caixaService.abrirCaixa(saldoInicial, observacoes);
            ra.addFlashAttribute("mensagem", "Caixa aberto com sucesso! Boas vendas.");
            return "redirect:/pos";
        } catch (Exception e) {
            ra.addFlashAttribute("mensagem_erro", "Erro ao abrir o caixa: " + e.getMessage());
            return "redirect:/caixa/abertura";
        }
    }

    @GetMapping("/fecho")
    public String telaFecho(Model model, RedirectAttributes ra) {
        Caixa caixaAberto = caixaService.getCaixaAbertoAtual();
        if (caixaAberto == null) {
            ra.addFlashAttribute("mensagem_erro", "Não existe nenhum caixa aberto no momento.");
            return "redirect:/home";
        }
        
        Double totalSistema = caixaAberto.getSaldoInicial() + (caixaAberto.getTotalNumerario() != null ? caixaAberto.getTotalNumerario() : 0.0);
        
        model.addAttribute("caixa", caixaAberto);
        model.addAttribute("totalSistema", totalSistema);
        
        return "caixa/fecho";
    }

    @PostMapping("/fechar")
    public String fecharCaixa(@RequestParam(value = "saldoFinalInformado", defaultValue = "0") Double saldoFinalInformado,
                              @RequestParam(value = "observacoes", required = false) String observacoes,
                              RedirectAttributes ra) {
        try {
            Caixa caixa = caixaService.fecharCaixa(saldoFinalInformado, observacoes);
            
            String msg = "Caixa fechado com sucesso!";
            if (caixa.getQuebraCaixa() < 0) {
                msg += " Atenção: Houve uma quebra de caixa negativa (falta de dinheiro).";
            } else if (caixa.getQuebraCaixa() > 0) {
                msg += " Atenção: Houve uma sobra de dinheiro no caixa.";
            }
            
            ra.addFlashAttribute("mensagem", msg);
            return "redirect:/home";
        } catch (Exception e) {
            ra.addFlashAttribute("mensagem_erro", "Erro ao fechar o caixa: " + e.getMessage());
            return "redirect:/caixa/fecho";
        }
    }
}
