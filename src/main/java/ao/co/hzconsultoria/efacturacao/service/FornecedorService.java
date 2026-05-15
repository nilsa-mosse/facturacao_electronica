package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Fornecedor;
import ao.co.hzconsultoria.efacturacao.repository.FornecedorRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FornecedorService {

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    public List<Fornecedor> listarTodos(Long empresaId) {
        return fornecedorRepository.findByEmpresa_Id(empresaId);
    }

    public Fornecedor salvar(Fornecedor fornecedor, Long empresaId) {
        fornecedor.setEmpresa(empresaRepository.findById(empresaId).orElse(null));
        return fornecedorRepository.save(fornecedor);
    }

    public Fornecedor buscarPorId(Long id, Long empresaId) {
        Fornecedor fornecedor = fornecedorRepository.findById(id).orElse(null);
        if (fornecedor != null && fornecedor.getEmpresa() != null && fornecedor.getEmpresa().getId().equals(empresaId)) {
            return fornecedor;
        }
        return null;
    }

    public void atualizar(Fornecedor fornecedor, Long empresaId) {
        Fornecedor existente = buscarPorId(fornecedor.getId(), empresaId);
        if (existente != null) {
            fornecedor.setEmpresa(existente.getEmpresa());
            fornecedorRepository.save(fornecedor);
        }
    }

    public void excluir(Long id, Long empresaId) {
        Fornecedor fornecedor = buscarPorId(id, empresaId);
        if (fornecedor != null) {
            fornecedorRepository.delete(fornecedor);
        }
    }
}
