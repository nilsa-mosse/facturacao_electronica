package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Cliente;
import ao.co.hzconsultoria.efacturacao.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // Exibir formulário de cadastro
    @GetMapping("/adicionar")
    public String mostrarFormularioCadastro(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cadastroCliente";
    }

    // Processar cadastro
    @PostMapping("/adicionar")
    public String cadastrarCliente(@ModelAttribute Cliente cliente) {
        clienteService.salvar(cliente);
        return "redirect:/clientes/listar";
    }

    // Listar clientes
    @GetMapping("/listar")
    public String listarClientes(Model model) {
        model.addAttribute("clientes", clienteService.listarTodos());
        return "listarClientes";
    }

    // Exibir formulário de edição
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicao(@PathVariable Long id, Model model) {
        Cliente cliente = clienteService.buscarPorId(id);
        model.addAttribute("cliente", cliente);
        return "cadastroEdicaoCliente";
    }

    // Atualizar cliente
    @PostMapping("/atualizar")
    public String atualizarCliente(@ModelAttribute Cliente cliente) {
        clienteService.atualizar(cliente);
        return "redirect:/clientes/listar";
    }
    
    // Exibir formulário de cadastro
    @GetMapping("/historico")
    public String mostrarHistorico(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "historicoComprasCliente";
    }
}
