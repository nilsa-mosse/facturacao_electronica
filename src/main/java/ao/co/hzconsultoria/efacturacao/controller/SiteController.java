package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SiteController {

    @Autowired
    private ProdutoRepository produtoRepository;

    @GetMapping("/licenca-expirada")
    public String licencaExpirada() {
        return "licenca-expirada";
    }
}