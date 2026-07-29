package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Auditoria;
import ao.co.hzconsultoria.efacturacao.service.AuditoriaService;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR', 'SUPERADMIN')")
    @GetMapping("/auditoria")
    public String listarAuditoria(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        Page<Auditoria> paginaAuditoria = auditoriaService.listarPorEmpresa(empresaId, page, size);

        model.addAttribute("pagina", paginaAuditoria);
        model.addAttribute("registos", paginaAuditoria.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", paginaAuditoria.getTotalPages());
        model.addAttribute("totalItems", paginaAuditoria.getTotalElements());

        return "auditoria/listar";
    }
}
