package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class VendaService {

    @Autowired
    private CompraRepository compraRepository;

    public Compra finalizarVenda(Carrinho carrinho) {
        Compra compra = new Compra();
        compra.setDataCompra(LocalDateTime.now());
        compra.setTotal(carrinho.getItens().stream().mapToDouble(item -> item.getPreco() * item.getQuantidade()).sum());

        compra.setItens(carrinho.getItens().stream().map(item -> {
            ItemCompra itemCompra = new ItemCompra();
            itemCompra.setNomeProduto(item.getNome());
            itemCompra.setQuantidade(item.getQuantidade());
            itemCompra.setPreco(item.getPreco());
            itemCompra.setSubtotal(item.getQuantidade() * item.getPreco());
            return itemCompra;
        }).collect(Collectors.toList()));

        return compraRepository.save(compra);
    }

    public Compra finalizarVenda(Compra compra) {
        compra.setDataCompra(LocalDateTime.now());
        compra.setTotal(compra.getItens().stream().mapToDouble(ItemCompra::getSubtotal).sum());
        return compraRepository.save(compra);
    }
}