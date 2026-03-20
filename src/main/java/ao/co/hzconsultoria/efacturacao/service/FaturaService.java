package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Fatura;
import ao.co.hzconsultoria.efacturacao.model.Carrinho;
import ao.co.hzconsultoria.efacturacao.model.Compra;
import org.springframework.stereotype.Service;

@Service
public class FaturaService {
    public Fatura emitirFatura(Compra compra) {
        // Lógica para emitir fatura
        return null;
    }

    public void gerarFatura(Carrinho carrinho) {
        // Lógica para gerar fatura a partir do carrinho
        // (implementar conforme necessidade do negócio)
    }

    public String gerarFatura(Compra compra) {
        StringBuilder fatura = new StringBuilder();
        fatura.append("Fatura\n");
        fatura.append("Data: ").append(compra.getDataCompra()).append("\n");
        fatura.append("Itens:\n");

        compra.getItens().forEach(item -> {
            fatura.append(item.getNomeProduto())
                  .append(" - Qtd: ").append(item.getQuantidade())
                  .append(" - Preço: ").append(item.getPreco())
                  .append(" - Subtotal: ").append(item.getSubtotal())
                  .append("\n");
        });

        fatura.append("Total: ").append(compra.getTotal()).append("\n");
        return fatura.toString();
    }
}