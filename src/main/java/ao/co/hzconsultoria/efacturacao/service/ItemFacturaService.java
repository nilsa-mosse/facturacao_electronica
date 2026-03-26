package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.ItemFactura;
import ao.co.hzconsultoria.efacturacao.repository.ItemFacturaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemFacturaService {
    @Autowired
    private ItemFacturaRepository itemFacturaRepository;

    public List<ItemFactura> listarPorFactura(Long facturaId) {
        return itemFacturaRepository.findByFacturaId(facturaId);
    }

    public ItemFactura salvar(ItemFactura itemFactura) {
        return itemFacturaRepository.save(itemFactura);
    }
}
