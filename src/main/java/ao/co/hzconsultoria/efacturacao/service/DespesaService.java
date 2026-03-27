package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.repository.DespesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DespesaService {

    @Autowired
    private DespesaRepository despesaRepository;

    public List<Despesa> listarTodas() {
        return despesaRepository.findAll();
    }

    public Despesa salvar(Despesa despesa) {
        return despesaRepository.save(despesa);
    }

    public Despesa buscarPorId(Long id) {
        return despesaRepository.findById(id).orElse(null);
    }

    public void excluir(Long id) {
        despesaRepository.deleteById(id);
    }

    public void atualizar(Despesa despesa) {
        despesaRepository.save(despesa);
    }
}
