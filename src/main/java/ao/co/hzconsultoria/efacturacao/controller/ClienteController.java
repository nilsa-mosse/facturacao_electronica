package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Cliente;
import ao.co.hzconsultoria.efacturacao.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private MessageSource messageSource;

    // Exibir formulário de cadastro
    @GetMapping("/adicionar")
    public String mostrarFormularioCadastro(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cadastroCliente";
    }

    // Processar cadastro
    @PostMapping("/adicionar")
    public String cadastrarCliente(@ModelAttribute Cliente cliente, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        clienteService.salvar(cliente, empresaId);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.cliente.salvo", null, LocaleContextHolder.getLocale()));
        return "redirect:/clientes/listar";
    }

    // Listar clientes
    @GetMapping("/listar")
    public String listarClientes(Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        model.addAttribute("clientes", clienteService.listarTodos(empresaId));
        return "listarClientes";
    }

    // Exibir formulário de edição
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicao(@PathVariable Long id, Model model) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        Cliente cliente = clienteService.buscarPorId(id, empresaId);
        if (cliente == null) {
            return "redirect:/clientes/listar";
        }
        model.addAttribute("cliente", cliente);
        return "cadastroEdicaoCliente";
    }

    // Atualizar cliente
    @PostMapping("/atualizar")
    public String atualizarCliente(@ModelAttribute Cliente cliente, RedirectAttributes redirectAttributes) {
        Long empresaId = ao.co.hzconsultoria.efacturacao.security.SecurityUtils.getCurrentEmpresaId();
        clienteService.atualizar(cliente, empresaId);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.cliente.atualizado", null, LocaleContextHolder.getLocale()));
        return "redirect:/clientes/listar";
    }
    
    // Exibir formulário de cadastro
    @GetMapping("/historico")
    public String mostrarHistorico(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "historicoComprasCliente";
    }
}
