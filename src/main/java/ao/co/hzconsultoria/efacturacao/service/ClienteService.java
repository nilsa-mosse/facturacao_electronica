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

    @Autowired
    private ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository empresaRepository;

    public void salvar(Cliente cliente, Long empresaId) {
        ao.co.hzconsultoria.efacturacao.model.Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        cliente.setEmpresa(empresa);
        clienteRepository.save(cliente);
    }

    public List<Cliente> listarTodos(Long empresaId) {
        return clienteRepository.findByEmpresa_Id(empresaId);
    }

    public Cliente buscarPorId(Long id, Long empresaId) {
        Cliente cliente = clienteRepository.findById(id).orElse(null);
        if (cliente != null && cliente.getEmpresa() != null && cliente.getEmpresa().getId().equals(empresaId)) {
            return cliente;
        }
        return null;
    }

    public void atualizar(Cliente cliente, Long empresaId) {
        Cliente original = clienteRepository.findById(cliente.getId()).orElse(null);
        if (original != null && original.getEmpresa() != null && original.getEmpresa().getId().equals(empresaId)) {
            cliente.setEmpresa(original.getEmpresa());
            clienteRepository.save(cliente);
        }
    }
}
