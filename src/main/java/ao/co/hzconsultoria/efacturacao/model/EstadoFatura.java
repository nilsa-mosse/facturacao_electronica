package ao.co.hzconsultoria.efacturacao.model;

/**
 * Estados possíveis de uma Factura
 */
public enum EstadoFatura {
    RASCUNHO("Rascunho"),
    EMITIDA("Emitida"),
    VALIDADA_AGT("Validada AGT"),
    PARCIALMENTE_PAGA("Parcialmente Paga"),
    PAGA("Paga"),
    VENCIDA("Vencida"),
    ANULADA("Anulada");

    private final String descricao;

    EstadoFatura(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    public static EstadoFatura fromString(String value) {
        if (value == null || value.isEmpty()) {
            return RASCUNHO;
        }
        try {
            return EstadoFatura.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Mapeamento de valores antigos para novos
            switch (value.toUpperCase()) {
                case "PENDENTE":
                    return RASCUNHO;
                case "VALIDADA":
                    return VALIDADA_AGT;
                case "FALHA_ENVIO":
                    return EMITIDA;
                case "EMITIDA_OFFLINE":
                    return EMITIDA;
                default:
                    return RASCUNHO;
            }
        }
    }
}
