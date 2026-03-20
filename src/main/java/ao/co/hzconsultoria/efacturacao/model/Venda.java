package ao.co.hzconsultoria.efacturacao.model;

import java.util.Date;
import java.util.List;

public class Venda {
    private Long id;
    private Cliente cliente;
    private List<ItemCarrinho> itens;
    private double total;
    private Date dataVenda;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Cliente getCliente() {
		return cliente;
	}
	public void setCliente(Cliente cliente) {
		this.cliente = cliente;
	}
	public List<ItemCarrinho> getItens() {
		return itens;
	}
	public void setItens(List<ItemCarrinho> itens) {
		this.itens = itens;
	}
	public double getTotal() {
		return total;
	}
	public void setTotal(double total) {
		this.total = total;
	}
	public Date getDataVenda() {
		return dataVenda;
	}
	public void setDataVenda(Date dataVenda) {
		this.dataVenda = dataVenda;
	}

    // Getters e Setters
    
}


