package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Caixa;
import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.CaixaRepository;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CaixaService {

    @Autowired
    private CaixaRepository caixaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    public Caixa getCaixaAbertoPorOperador(Long operadorId) {
        Optional<Caixa> caixaOpt = caixaRepository.findFirstByOperador_IdAndEstadoOrderByIdDesc(operadorId, "ABERTO");
        return caixaOpt.orElse(null);
    }

    public Caixa getCaixaAbertoAtual() {
        Long userId = SecurityUtils.getCurrentUserId();
        return getCaixaAbertoPorOperador(userId);
    }

    public boolean isCaixaAberto() {
        return getCaixaAbertoAtual() != null;
    }

    public Caixa abrirCaixa(Double saldoInicial, String observacoes) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long empresaId = SecurityUtils.getCurrentEmpresaId();

        // Verificar se já existe caixa aberto para este operador
        Caixa aberto = getCaixaAbertoPorOperador(userId);
        if (aberto != null) {
            throw new RuntimeException("Já existe um caixa aberto para este operador.");
        }

        User operador = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Operador não encontrado."));
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow(() -> new RuntimeException("Empresa não encontrada."));

        Caixa caixa = new Caixa();
        caixa.setOperador(operador);
        caixa.setEmpresa(empresa);
        // O estabelecimento poderia ser associado se aplicável ao operador
        if (operador.getEstabelecimentos() != null && !operador.getEstabelecimentos().isEmpty()) {
            caixa.setEstabelecimento(operador.getEstabelecimentos().iterator().next());
        }

        caixa.setDataAbertura(LocalDateTime.now());
        caixa.setSaldoInicial(saldoInicial != null ? saldoInicial : 0.0);
        caixa.setEstado("ABERTO");
        caixa.setObservacoes(observacoes);

        return caixaRepository.save(caixa);
    }

    public Caixa fecharCaixa(Double saldoFinalInformado, String observacoesFecho) {
        Caixa caixa = getCaixaAbertoAtual();
        if (caixa == null) {
            throw new RuntimeException("Não existe nenhum caixa aberto para fechar.");
        }

        caixa.setDataFecho(LocalDateTime.now());
        caixa.setEstado("FECHADO");
        caixa.setSaldoFinal(saldoFinalInformado != null ? saldoFinalInformado : 0.0);
        
        // Calcular quebra de caixa (diferença)
        // O total em caixa deveria ser = saldoInicial + totalNumerario (supondo que totalMulticaixa não fica no caixa físico)
        Double totalCalculado = caixa.getSaldoInicial() + (caixa.getTotalNumerario() != null ? caixa.getTotalNumerario() : 0.0);
        caixa.setQuebraCaixa(caixa.getSaldoFinal() - totalCalculado);

        if (observacoesFecho != null && !observacoesFecho.isEmpty()) {
            String obsAtual = caixa.getObservacoes() != null ? caixa.getObservacoes() : "";
            caixa.setObservacoes(obsAtual + " | Fecho: " + observacoesFecho);
        }

        return caixaRepository.save(caixa);
    }
    
    // Método para atualizar valores do caixa quando ocorre uma venda (chamado no FaturaService ou PDV)
    public void registarVendaNoCaixa(Caixa caixa, Double valorNumerario, Double valorMulticaixa) {
        if (caixa != null && "ABERTO".equals(caixa.getEstado())) {
            Double totalN = caixa.getTotalNumerario() != null ? caixa.getTotalNumerario() : 0.0;
            Double totalM = caixa.getTotalMulticaixa() != null ? caixa.getTotalMulticaixa() : 0.0;
            Double totalF = caixa.getTotalFaturado() != null ? caixa.getTotalFaturado() : 0.0;
            
            caixa.setTotalNumerario(totalN + (valorNumerario != null ? valorNumerario : 0.0));
            caixa.setTotalMulticaixa(totalM + (valorMulticaixa != null ? valorMulticaixa : 0.0));
            caixa.setTotalFaturado(totalF + (valorNumerario != null ? valorNumerario : 0.0) + (valorMulticaixa != null ? valorMulticaixa : 0.0));
            
            caixaRepository.save(caixa);
        }
    }
}
