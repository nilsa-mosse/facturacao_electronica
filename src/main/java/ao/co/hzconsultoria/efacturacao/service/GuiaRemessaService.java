package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.GuiaRemessa;
import ao.co.hzconsultoria.efacturacao.model.ItemGuiaRemessa;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.GuiaRemessaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GuiaRemessaService {

    @Autowired
    private GuiaRemessaRepository guiaRemessaRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Transactional
    public GuiaRemessa gerarGuiaAPartirDeFatura(Long facturaId) {
        Compra fatura = compraRepository.findById(facturaId).orElseThrow(() -> new RuntimeException("Fatura não encontrada"));
        
        GuiaRemessa guia = new GuiaRemessa();
        guia.setCliente(fatura.getCliente());
        guia.setDataEmissao(LocalDateTime.now());
        guia.setFaturaOrigem(fatura);
        guia.setNumeroGuia("GR-" + System.currentTimeMillis());
        guia.setStatus("ATIVA");
        
        List<ItemGuiaRemessa> itensGuia = fatura.getItens().stream().map(item -> {
            ItemGuiaRemessa ig = new ItemGuiaRemessa();
            ig.setNomeProduto(item.getNomeProduto());
            ig.setQuantidade(Double.valueOf(item.getQuantidade()));
            ig.setUnidadeMedida("UN");
            ig.setGuiaRemessa(guia);
            return ig;
        }).collect(Collectors.toList());
        
        guia.setItens(itensGuia);
        return guiaRemessaRepository.save(guia);
    }

    public List<GuiaRemessa> listarTodas() {
        return guiaRemessaRepository.findAll();
    }

    public GuiaRemessa buscarPorId(Long id) {
        return guiaRemessaRepository.findById(id).orElse(null);
    }

    @Transactional
    public void salvar(GuiaRemessa guia) {
        if (guia.getDataEmissao() == null) guia.setDataEmissao(LocalDateTime.now());
        if (guia.getNumeroGuia() == null) guia.setNumeroGuia("GR-" + System.currentTimeMillis());
        
        // Garantir vínculo bidireccional dos itens
        if (guia.getItens() != null) {
            guia.getItens().forEach(i -> i.setGuiaRemessa(guia));
        }
        
        guiaRemessaRepository.save(guia);
    }
}
