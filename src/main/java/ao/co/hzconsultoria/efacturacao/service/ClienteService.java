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

    public Cliente salvar(Cliente cliente, Long empresaId) {
        ao.co.hzconsultoria.efacturacao.model.Empresa empresa = null;
        if (empresaId != null) {
            empresa = empresaRepository.findById(empresaId).orElse(null);
        }
        if (empresa == null) {
            empresa = empresaRepository.findAll().stream().findFirst().orElse(null);
        }
        if (empresa != null) {
            cliente.setEmpresa(empresa);
            empresaId = empresa.getId();
        }

        if (cliente.getNif() != null && !cliente.getNif().trim().isEmpty() && !"999999999".equals(cliente.getNif().trim())) {
            String nifLimpo = cliente.getNif().trim();
            java.util.Optional<Cliente> existente;
            if (empresaId != null) {
                existente = clienteRepository.findByNifAndEmpresa_Id(nifLimpo, empresaId);
            } else {
                existente = clienteRepository.findAll().stream()
                        .filter(c -> nifLimpo.equalsIgnoreCase(c.getNif()))
                        .findFirst();
            }
            if (existente != null && existente.isPresent()) {
                Cliente c = existente.get();
                c.setNome(cliente.getNome());
                c.setEndereco(cliente.getEndereco());
                c.setTelefone(cliente.getTelefone());
                c.setEmail(cliente.getEmail());
                return clienteRepository.save(c);
            }
        }
        return clienteRepository.save(cliente);
    }

    public List<Cliente> listarTodos(Long empresaId) {
        if (empresaId != null) {
            return clienteRepository.findByEmpresa_Id(empresaId);
        }
        return clienteRepository.findAll();
    }

    public Cliente buscarPorId(Long id, Long empresaId) {
        if (id == null) return null;
        Cliente cliente = clienteRepository.findById(id).orElse(null);
        if (cliente != null && (empresaId == null || (cliente.getEmpresa() != null && cliente.getEmpresa().getId().equals(empresaId)))) {
            return cliente;
        }
        return null;
    }

    public void atualizar(Cliente cliente, Long empresaId) {
        if (cliente == null || cliente.getId() == null) return;
        Cliente original = clienteRepository.findById(cliente.getId()).orElse(null);
        if (original != null && (empresaId == null || (original.getEmpresa() != null && original.getEmpresa().getId().equals(empresaId)))) {
            cliente.setEmpresa(original.getEmpresa());
            clienteRepository.save(cliente);
        }
    }
}
