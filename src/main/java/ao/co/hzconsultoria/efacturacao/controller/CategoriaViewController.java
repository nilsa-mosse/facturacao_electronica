package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/categorias")
public class CategoriaViewController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping("/listar")
    public String listarCategorias(Model model) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId != null) {
            List<Categoria> categorias = categoriaRepository.findByEmpresa_Id(empresaId);
            model.addAttribute("categorias", categorias);
        }
        return "listarCategorias";
    }
}
