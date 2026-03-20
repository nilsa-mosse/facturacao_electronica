package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.ItemCarrinho;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.model.ItemCompra;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.service.VendaService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

public class CarrinhoService {
    @Autowired
    private VendaService vendaService;

    public void adicionarProduto(Carrinho carrinho, Produto produto, int quantidade) {
        boolean encontrado = false;
        for (ItemCarrinho item : carrinho.getItens()) {
            if (item.getProduto().getId() != null && item.getProduto().getId().equals(produto.getId())) {
                item.setQuantidade(item.getQuantidade() + quantidade);
                item.setPrecoTotal(item.getProduto().getPreco() * item.getQuantidade());
                encontrado = true;
                break;
            }
        }
        if (!encontrado) {
            ItemCarrinho item = new ItemCarrinho();
            item.setProduto(produto);
            item.setQuantidade(quantidade);
            item.setPrecoTotal(produto.getPreco() * quantidade);
            carrinho.getItens().add(item);
        }
    }

    public void removerProduto(Carrinho carrinho, Produto produto) {
        // Lógica para remover produto
    }

    public double calcularTotal(Carrinho carrinho) {
        double total = 0.0;
        for (ItemCarrinho item : carrinho.getItens()) {
            total += item.getPrecoTotal();
        }
        return total;
    }

    public void salvarItemNoBanco(Carrinho carrinho, Produto produto, int quantidade) {
        // Logic to save the item to the database
        System.out.println("Saving item to database: " + produto.getNome() + ", Quantity: " + quantidade);
        // Add actual database interaction logic here
    }

    public void salvarCompraNoBanco(Carrinho carrinho) {
        // Logic to save the purchase to the database
        System.out.println("Saving purchase to database with total: " + calcularTotal(carrinho));
        // Add actual database interaction logic here
    }

    public void gravarCarrinhoNoBanco(List<ItemCarrinho> itensCarrinho) {
        Compra compra = new Compra();
        List<ItemCompra> itensCompra = itensCarrinho.stream().map(item -> {
            ItemCompra ic = new ItemCompra();
            ic.setNomeProduto(item.getProduto().getNome());
            ic.setQuantidade(item.getQuantidade());
            ic.setPreco(item.getProduto().getPreco());
            ic.setSubtotal(item.getQuantidade() * item.getProduto().getPreco());
            ic.setCompra(compra);
            return ic;
        }).collect(Collectors.toList());
        compra.setItens(itensCompra);
        compra.setTotal(itensCompra.stream().mapToDouble(ItemCompra::getSubtotal).sum());
        compra.setDataCompra(java.time.LocalDateTime.now());
        vendaService.finalizarVenda(compra);
    }
}