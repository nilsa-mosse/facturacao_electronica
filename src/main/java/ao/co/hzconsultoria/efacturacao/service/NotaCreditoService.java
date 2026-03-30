package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.NotaCredito;
import ao.co.hzconsultoria.efacturacao.model.ItemNotaCredito;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import ao.co.hzconsultoria.efacturacao.repository.NotaCreditoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotaCreditoService {

    @Autowired
    private NotaCreditoRepository notaCreditoRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Transactional
    public NotaCredito prepararNotaDeFactura(Long facturaId) {
        Compra fatura = compraRepository.findById(facturaId).orElseThrow(() -> new RuntimeException("Fatura não encontrada"));
        
        NotaCredito nota = new NotaCredito();
        nota.setCliente(fatura.getCliente());
        nota.setFaturaOriginal(fatura);
        nota.setDataEmissao(LocalDateTime.now());
        nota.setNumeroNota("NC-" + System.currentTimeMillis());
        nota.setTotalCredito(fatura.getTotal());
        
        List<ItemNotaCredito> itensNota = fatura.getItens().stream().map(item -> {
            ItemNotaCredito inc = new ItemNotaCredito();
            inc.setNomeProduto(item.getNomeProduto());
            inc.setQuantidade(Double.valueOf(item.getQuantidade()));
            inc.setPrecoUnitario(item.getPreco());
            inc.setSubtotal(item.getSubtotal());
            inc.setNotaCredito(nota);
            return inc;
        }).collect(Collectors.toList());
        
        nota.setItens(itensNota);
        return nota; // Retorna para preenchimento de motivo antes de salvar
    }

    @Transactional
    public void salvar(NotaCredito nota) {
        if (nota.getDataEmissao() == null) nota.setDataEmissao(LocalDateTime.now());
        if (nota.getNumeroNota() == null) nota.setNumeroNota("NC-" + System.currentTimeMillis());
        
        // Vínculo bidireccional
        if (nota.getItens() != null) {
            nota.getItens().forEach(i -> i.setNotaCredito(nota));
        }
        
        notaCreditoRepository.save(nota);
    }

    public List<NotaCredito> listarTodas() {
        return notaCreditoRepository.findAll();
    }
}
