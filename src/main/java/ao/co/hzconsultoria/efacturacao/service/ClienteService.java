package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Cliente;
import ao.co.hzconsultoria.efacturacao.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository clienteRepository;

    public void salvar(Cliente cliente) {
        clienteRepository.save(cliente);
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id).orElse(null);
    }

    public void atualizar(Cliente cliente) {
        clienteRepository.save(cliente);
    }
}
