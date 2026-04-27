package ao.co.hzconsultoria.efacturacao;

// Fixed: Added JUnit 4 dependency to pom.xml
import ao.co.hzconsultoria.efacturacao.model.Produto;
import org.junit.Assert;
import org.junit.Test;

public class ProdutoTest {
    @Test
    public void testSetAndGetNome() {
        Produto produto = new Produto();
        produto.setNome("Teste");
        Assert.assertEquals("Teste", produto.getNome());
    }
    @Test
    public void testSetAndGetPreco() {
        Produto produto = new Produto();
        produto.setPreco(10.0);
        Assert.assertEquals(10.0, produto.getPreco(), 0.01);
    }
}
