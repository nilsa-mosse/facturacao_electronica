package ao.co.hzconsultoria.efacturacao.model;

import java.util.Date;
import java.util.List;

public class Venda {
    private Long id;
    private Cliente cliente;
    private List<ItemCarrinho> itens;
    private double total;
    private Date dataVenda;

    // Getters e Setters
}
