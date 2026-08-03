package ao.co.hzconsultoria.efacturacao.service.ia;

import ao.co.hzconsultoria.efacturacao.model.Compra;
import ao.co.hzconsultoria.efacturacao.repository.CompraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Serviço de Pesquisa Inteligente em Linguagem Natural.
 * Permite pesquisas como: "Mostra-me as vendas do João em Junho"
 */
@Service
public class PesquisaInteligenteService {

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private GeminiService geminiService;

    public Map<String, Object> pesquisar(String termo, Long empresaId) {
        String termoLower = termo.toLowerCase();
        List<Compra> todas = (empresaId == null) ? compraRepository.findAll() : compraRepository.findByEmpresa_Id(empresaId);

        // Extrair filtro de mês
        Integer mes = extrairMes(termoLower);
        String clienteNome = extrairNomeCliente(termoLower);

        List<Compra> filtradas = todas.stream().filter(c -> {
            boolean bateuMes = true;
            if (mes != null && c.getDataCompra() != null) {
                bateuMes = (c.getDataCompra().toLocalDate().getMonthValue() == mes);
            }

            boolean bateuCliente = true;
            if (clienteNome != null && !clienteNome.isEmpty()) {
                String cNome = c.getCliente() != null && c.getCliente().getNome() != null ? c.getCliente().getNome().toLowerCase() : "";
                bateuCliente = cNome.contains(clienteNome);
            }

            return bateuMes && bateuCliente;
        }).collect(Collectors.toList());

        double totalSoma = filtradas.stream()
                .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getStatus()))
                .mapToDouble(c -> c.getTotal() != null ? c.getTotal() : 0.0)
                .sum();

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("termoOriginal", termo);
        resposta.put("mesIdentificado", mes != null ? getNomeMes(mes) : "Todos");
        resposta.put("clienteIdentificado", clienteNome != null ? clienteNome : "Todos");
        resposta.put("totalEncontrados", filtradas.size());
        resposta.put("valorTotalSoma", totalSoma);

        List<Map<String, Object>> resultadosFormated = new ArrayList<>();
        for (Compra c : filtradas) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("data", c.getDataCompra() != null ? c.getDataCompra().toString().substring(0, 16) : "N/A");
            m.put("cliente", c.getCliente() != null ? c.getCliente().getNome() : "Consumidor Final");
            m.put("total", c.getTotal());
            m.put("status", c.getStatus());
            resultadosFormated.add(m);
        }
        resposta.put("vendas", resultadosFormated);

        // Se Gemini estiver disponível, gerar um pequeno resumo textual inteligente
        if (geminiService.isDisponivel()) {
            String promptSistema = "És um analista de dados financeiro. Resume os dados pesquisados de forma sucinta e profissional.";
            String promptUser = "O utilizador pesquisou '" + termo + "'. Foram encontradas " + filtradas.size() + " vendas perfazendo um total de " + totalSoma + " Kz.";
            resposta.put("resumoIa", geminiService.gerarTexto(promptSistema, promptUser));
        } else {
            resposta.put("resumoIa", "Encontradas " + filtradas.size() + " vendas relativas à sua pesquisa no valor total de " + String.format("%,.2f Kz", totalSoma) + ".");
        }

        return resposta;
    }

    private Integer extrairMes(String texto) {
        String[] meses = {"janeiro", "fevereiro", "março", "marco", "abril", "maio", "junho", 
                          "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};
        for (int i = 0; i < meses.length; i++) {
            if (texto.contains(meses[i])) {
                return (i == 3) ? 3 : (i + 1 > 12 ? 3 : i + 1); // Trata março/marco
            }
        }
        return null;
    }

    private String extrairNomeCliente(String texto) {
        Pattern pattern = Pattern.compile("(?:do|da|de|cliente|para)\\s+([a-zA-ZáéíóúÁÉÍÓÚãõÃÕçÇ]+)");
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            String capturado = matcher.group(1).toLowerCase();
            // Ignorar meses se forem capturados por engano
            if (!capturado.equals("junho") && !capturado.equals("julho") && !capturado.equals("maio") && !capturado.equals("janeiro")) {
                return capturado;
            }
        }
        return null;
    }

    private String getNomeMes(int m) {
        String[] meses = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        return (m >= 1 && m <= 12) ? meses[m - 1] : "Desconhecido";
    }
}
