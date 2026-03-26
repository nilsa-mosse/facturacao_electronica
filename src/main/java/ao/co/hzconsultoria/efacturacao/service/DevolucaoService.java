package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Devolucao;
import ao.co.hzconsultoria.efacturacao.repository.DevolucaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DevolucaoService {
    @Autowired
    private DevolucaoRepository devolucaoRepository;

    public Devolucao registrarDevolucao(Devolucao devolucao) {
        return devolucaoRepository.save(devolucao);
    }

    public List<Devolucao> listarDevolucoes() {
        return devolucaoRepository.findAll();
    }
}
