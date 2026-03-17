package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.ItemCarrinho;
import ao.co.hzconsultoria.efacturacao.model.Produto;

public class CarrinhoService {
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
}