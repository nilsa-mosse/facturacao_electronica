package ao.co.hzconsultoria.efacturacao;

import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Produto;
import ao.co.hzconsultoria.efacturacao.service.CarrinhoService;
import org.junit.Assert;
import org.junit.Test;

public class CarrinhoServiceTest {
    @Test
    public void testAdicionarProduto() {
        Carrinho carrinho = new Carrinho();
        Produto produto = new Produto();
        produto.setNome("Teste");
        produto.setPreco(10.0);
        CarrinhoService service = new CarrinhoService();
        service.adicionarProduto(carrinho, produto, 2);
        Assert.assertEquals(1, carrinho.getItens().size());
    }

    @Test
    public void testCalcularTotal() {
        Carrinho carrinho = new Carrinho();
        Produto produto = new Produto();
        produto.setNome("Teste");
        produto.setPreco(10.0);
        CarrinhoService service = new CarrinhoService();
        service.adicionarProduto(carrinho, produto, 2);
        double total = service.calcularTotal(carrinho);
        Assert.assertEquals(20.0, total, 0.01);
    }
}
