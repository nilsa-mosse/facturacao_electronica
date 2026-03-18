package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Client;
import ao.co.hzconsultoria.efacturacao.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clientes")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @GetMapping("/detalhes")
    public String listClients(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "listarClientes";
    }
    
    @GetMapping("/listar")
    public String listar(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "listarClientes";
    }


    @GetMapping("/adicionar")
    public String showClientForm(Model model) {
        model.addAttribute("client", new Client());
        return "cadastroEdicaoCliente";
    }

    @PostMapping("/save")
    public String saveClient(@ModelAttribute Client client) {
        clientService.saveClient(client);
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editClient(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.getClientById(id));
        return "cadastroEdicaoCliente";
    }

    @PostMapping("/update")
    public String updateClient(@ModelAttribute Client client) {
        clientService.updateClient(client);
        return "redirect:/clientes";
    }

    @GetMapping("/all")
    public String getAllClients(Model model) {
        model.addAttribute("clients", clientService.getAllClients());
        return "listarClientes";
    }
}