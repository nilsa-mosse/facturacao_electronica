package ao.co.hzconsultoria.efacturacao.controller;


import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalControllerAdvice {

    // Nota: Em um cenário real, você buscaria isso de um banco de dados ou service
    @ModelAttribute
    public void addAttributes(Model model) {
        String tema = "light";
        if (ConfiguracaoController.sistemaConfig != null) {
            String temp = ConfiguracaoController.sistemaConfig.getTema();
            if (temp != null) {
                if (temp.equalsIgnoreCase("escuro") || temp.equalsIgnoreCase("dark")) {
                    tema = "dark";
                } else {
                    tema = "light";
                }
            }
        }
        model.addAttribute("globalTema", tema);
    }
}