package ao.co.hzconsultoria.efacturacao.model;

import java.util.ArrayList;
import java.util.List;

public class Carrinho {
    private List<ItemCarrinho> itens = new ArrayList<>();
    private Cliente cliente;

    // Métodos para adicionar/remover itens, calcular total, etc.

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
}