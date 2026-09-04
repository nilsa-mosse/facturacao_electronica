package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.model.PedidoCozinha;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import ao.co.hzconsultoria.efacturacao.service.PosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/pos")
public class KdsController {

    @Autowired
    private PosService posService;

    @Autowired
    private EmpresaRepository empresaRepository;

    @GetMapping("/kds")
    public String exibirMonitorKds(Model model) {
        CustomUserDetails user = SecurityUtils.getCurrentUser();
        Empresa e = null;
        if (user != null && user.getEmpresaId() != null) {
            e = empresaRepository.findById(user.getEmpresaId()).orElse(null);
        }
        if (e == null) {
            e = empresaRepository.findAll().stream().findFirst().orElse(null);
        }

        List<PedidoCozinha> pedidos = posService.listarPedidosCozinhaAtivos(e != null ? e.getId() : 1L);
        model.addAttribute("pedidos", pedidos);
        model.addAttribute("empresa", e);
        return "pos/kds";
    }
}
