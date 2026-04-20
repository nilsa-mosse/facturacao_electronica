package ao.co.hzconsultoria.efacturacao.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/configuracoes/saft")
public class SaftViewController {

    @GetMapping
    public String exibirTelaSaft(Model model) {
        model.addAttribute("titulo", "Gestão de Ficheiro SAF-T (AO)");
        return "configuracoes/saft";
    }
}
