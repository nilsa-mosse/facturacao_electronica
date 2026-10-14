package ao.co.hzconsultoria.efacturacao.dto;

import java.util.Date;

public class RecebimentoDashboardDTO {

    public static class RecebimentoItemDTO {
        private Long id;
        private String numeroDocumento;
        private String tipoDocumento; // "RC" ou "FR"
        private Date dataEmissao;
        private String dataFormatada = "-";
        private String horaFormatada = "-";
        private String faturaReferenciaNumero = "-";
        private String nomeCliente = "Consumidor Final";
        private String nifCliente = "-";
        private String formaPagamento = "CASH";
        private Double valor = 0.0;
        private String status = "EMITIDA";
        private String usuarioNome = "Sistema";
        private boolean temPdf = false;
        private String urlPdf;

        public String getIniciais() {
            if (nomeCliente == null || nomeCliente.trim().isEmpty()) return "CF";
            String trimmed = nomeCliente.trim();
            return (trimmed.length() > 1 ? trimmed.substring(0, 2) : trimmed).toUpperCase();
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getNumeroDocumento() { return numeroDocumento; }
        public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
        public String getTipoDocumento() { return tipoDocumento; }
        public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }
        public Date getDataEmissao() { return dataEmissao; }
        public void setDataEmissao(Date dataEmissao) { this.dataEmissao = dataEmissao; }
        public String getDataFormatada() { return dataFormatada; }
        public void setDataFormatada(String dataFormatada) { this.dataFormatada = dataFormatada; }
        public String getHoraFormatada() { return horaFormatada; }
        public void setHoraFormatada(String horaFormatada) { this.horaFormatada = horaFormatada; }
        public String getFaturaReferenciaNumero() { return faturaReferenciaNumero; }
        public void setFaturaReferenciaNumero(String faturaReferenciaNumero) { this.faturaReferenciaNumero = faturaReferenciaNumero; }
        public String getNomeCliente() { return nomeCliente; }
        public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
        public String getNifCliente() { return nifCliente; }
        public void setNifCliente(String nifCliente) { this.nifCliente = nifCliente; }
        public String getFormaPagamento() { return formaPagamento; }
        public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
        public Double getValor() { return valor != null ? valor : 0.0; }
        public void setValor(Double valor) { this.valor = valor; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getUsuarioNome() { return usuarioNome; }
        public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }
        public boolean isTemPdf() { return temPdf; }
        public void setTemPdf(boolean temPdf) { this.temPdf = temPdf; }
        public String getUrlPdf() { return urlPdf; }
        public void setUrlPdf(String urlPdf) { this.urlPdf = urlPdf; }
    }

    public static class ContaReceberDTO {
        private Long id;
        private Long compraId;
        private String numeroFatura;
        private Date dataEmissao;
        private Date dataVencimento;
        private String dataEmissaoFormatada = "-";
        private String dataVencimentoFormatada = "Sem Prazo";
        private String nomeCliente = "Cliente Não Identificado";
        private String nifCliente = "-";
        private String telefoneCliente = "-";
        private String emailCliente = "-";
        private Double total = 0.0;
        private Double valorPago = 0.0;
        private Double valorEmAberto = 0.0;
        private Double percentualPago = 0.0;
        private long diasAtraso = 0;
        private String statusVencimento = "SEM_DATA"; // "NO_PRAZO", "VENCE_HOJE", "VENCIDA", "SEM_DATA"
        private String statusFatura = "EMITIDA";

        public String getIniciais() {
            if (nomeCliente == null || nomeCliente.trim().isEmpty()) return "CL";
            String trimmed = nomeCliente.trim();
            return (trimmed.length() > 1 ? trimmed.substring(0, 2) : trimmed).toUpperCase();
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getCompraId() { return compraId; }
        public void setCompraId(Long compraId) { this.compraId = compraId; }
        public String getNumeroFatura() { return numeroFatura; }
        public void setNumeroFatura(String numeroFatura) { this.numeroFatura = numeroFatura; }
        public Date getDataEmissao() { return dataEmissao; }
        public void setDataEmissao(Date dataEmissao) { this.dataEmissao = dataEmissao; }
        public Date getDataVencimento() { return dataVencimento; }
        public void setDataVencimento(Date dataVencimento) { this.dataVencimento = dataVencimento; }
        public String getDataEmissaoFormatada() { return dataEmissaoFormatada; }
        public void setDataEmissaoFormatada(String dataEmissaoFormatada) { this.dataEmissaoFormatada = dataEmissaoFormatada; }
        public String getDataVencimentoFormatada() { return dataVencimentoFormatada; }
        public void setDataVencimentoFormatada(String dataVencimentoFormatada) { this.dataVencimentoFormatada = dataVencimentoFormatada; }
        public String getNomeCliente() { return nomeCliente; }
        public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
        public String getNifCliente() { return nifCliente; }
        public void setNifCliente(String nifCliente) { this.nifCliente = nifCliente; }
        public String getTelefoneCliente() { return telefoneCliente; }
        public void setTelefoneCliente(String telefoneCliente) { this.telefoneCliente = telefoneCliente; }
        public String getEmailCliente() { return emailCliente; }
        public void setEmailCliente(String emailCliente) { this.emailCliente = emailCliente; }
        public Double getTotal() { return total != null ? total : 0.0; }
        public void setTotal(Double total) { this.total = total; }
        public Double getValorPago() { return valorPago != null ? valorPago : 0.0; }
        public void setValorPago(Double valorPago) { this.valorPago = valorPago; }
        public Double getValorEmAberto() { return valorEmAberto != null ? valorEmAberto : 0.0; }
        public void setValorEmAberto(Double valorEmAberto) { this.valorEmAberto = valorEmAberto; }
        public Double getPercentualPago() { return percentualPago != null ? percentualPago : 0.0; }
        public void setPercentualPago(Double percentualPago) { this.percentualPago = percentualPago; }
        public long getDiasAtraso() { return diasAtraso; }
        public void setDiasAtraso(long diasAtraso) { this.diasAtraso = diasAtraso; }
        public long getDiasRestantes() { return Math.abs(diasAtraso); }
        public String getStatusVencimento() { return statusVencimento; }
        public void setStatusVencimento(String statusVencimento) { this.statusVencimento = statusVencimento; }
        public String getStatusFatura() { return statusFatura; }
        public void setStatusFatura(String statusFatura) { this.statusFatura = statusFatura; }
    }

    public static class MetodoPagamentoResumoDTO {
        private String metodo;
        private String label;
        private Double total = 0.0;
        private long quantidade = 0;
        private Double percentual = 0.0;
        private String cor = "#10b981";
        private String icone = "fa-money-bill-wave";

        public MetodoPagamentoResumoDTO() {}

        public MetodoPagamentoResumoDTO(String metodo, String label, String cor, String icone) {
            this.metodo = metodo;
            this.label = label;
            this.cor = cor;
            this.icone = icone;
        }

        public String getMetodo() { return metodo; }
        public void setMetodo(String metodo) { this.metodo = metodo; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Double getTotal() { return total != null ? total : 0.0; }
        public void setTotal(Double total) { this.total = total; }
        public long getQuantidade() { return quantidade; }
        public void setQuantidade(long quantidade) { this.quantidade = quantidade; }
        public Double getPercentual() { return percentual != null ? percentual : 0.0; }
        public void setPercentual(Double percentual) { this.percentual = percentual; }
        public String getCor() { return cor; }
        public void setCor(String cor) { this.cor = cor; }
        public String getIcone() { return icone; }
        public void setIcone(String icone) { this.icone = icone; }
    }

    public static class ClienteDevedorDTO {
        private Long clienteId;
        private String nome = "Consumidor Final";
        private String nif = "-";
        private String telefone = "-";
        private String email = "-";
        private Double totalEmDivida = 0.0;
        private int totalFaturas = 0;
        private int faturasVencidas = 0;
        private Double percentualDoTotal = 0.0;

        public String getIniciais() {
            if (nome == null || nome.trim().isEmpty()) return "CL";
            String trimmed = nome.trim();
            return (trimmed.length() > 1 ? trimmed.substring(0, 2) : trimmed).toUpperCase();
        }

        public Long getClienteId() { return clienteId; }
        public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getNif() { return nif; }
        public void setNif(String nif) { this.nif = nif; }
        public String getTelefone() { return telefone; }
        public void setTelefone(String telefone) { this.telefone = telefone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Double getTotalEmDivida() { return totalEmDivida != null ? totalEmDivida : 0.0; }
        public void setTotalEmDivida(Double totalEmDivida) { this.totalEmDivida = totalEmDivida; }
        public int getTotalFaturas() { return totalFaturas; }
        public void setTotalFaturas(int totalFaturas) { this.totalFaturas = totalFaturas; }
        public int getFaturasVencidas() { return faturasVencidas; }
        public void setFaturasVencidas(int faturasVencidas) { this.faturasVencidas = faturasVencidas; }
        public Double getPercentualDoTotal() { return percentualDoTotal != null ? percentualDoTotal : 0.0; }
        public void setPercentualDoTotal(Double percentualDoTotal) { this.percentualDoTotal = percentualDoTotal; }
    }

    public static class ClienteRecebimentoDTO {
        private Long clienteId;
        private String nome = "Consumidor Final";
        private String nif = "-";
        private Double totalPago = 0.0;
        private int totalRecibos = 0;

        public String getIniciais() {
            if (nome == null || nome.trim().isEmpty()) return "CL";
            String trimmed = nome.trim();
            return (trimmed.length() > 1 ? trimmed.substring(0, 2) : trimmed).toUpperCase();
        }

        public Long getClienteId() { return clienteId; }
        public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        public String getNif() { return nif; }
        public void setNif(String nif) { this.nif = nif; }
        public Double getTotalPago() { return totalPago != null ? totalPago : 0.0; }
        public void setTotalPago(Double totalPago) { this.totalPago = totalPago; }
        public int getTotalRecibos() { return totalRecibos; }
        public void setTotalRecibos(int totalRecibos) { this.totalRecibos = totalRecibos; }
    }

    public static class AgingContasReceberDTO {
        private Double noPrazo = 0.0;
        private Double vencido1a30 = 0.0;
        private Double vencido31a60 = 0.0;
        private Double vencido61a90 = 0.0;
        private Double vencidoMais90 = 0.0;

        private int qtdNoPrazo = 0;
        private int qtdVencido1a30 = 0;
        private int qtdVencido31a60 = 0;
        private int qtdVencido61a90 = 0;
        private int qtdVencidoMais90 = 0;

        public Double getNoPrazo() { return noPrazo != null ? noPrazo : 0.0; }
        public void setNoPrazo(Double noPrazo) { this.noPrazo = noPrazo; }
        public Double getVencido1a30() { return vencido1a30 != null ? vencido1a30 : 0.0; }
        public void setVencido1a30(Double vencido1a30) { this.vencido1a30 = vencido1a30; }
        public Double getVencido31a60() { return vencido31a60 != null ? vencido31a60 : 0.0; }
        public void setVencido31a60(Double vencido31a60) { this.vencido31a60 = vencido31a60; }
        public Double getVencido61a90() { return vencido61a90 != null ? vencido61a90 : 0.0; }
        public void setVencido61a90(Double vencido61a90) { this.vencido61a90 = vencido61a90; }
        public Double getVencidoMais90() { return vencidoMais90 != null ? vencidoMais90 : 0.0; }
        public void setVencidoMais90(Double vencidoMais90) { this.vencidoMais90 = vencidoMais90; }

        public int getQtdNoPrazo() { return qtdNoPrazo; }
        public void setQtdNoPrazo(int qtdNoPrazo) { this.qtdNoPrazo = qtdNoPrazo; }
        public int getQtdVencido1a30() { return qtdVencido1a30; }
        public void setQtdVencido1a30(int qtdVencido1a30) { this.qtdVencido1a30 = qtdVencido1a30; }
        public int getQtdVencido31a60() { return qtdVencido31a60; }
        public void setQtdVencido31a60(int qtdVencido31a60) { this.qtdVencido31a60 = qtdVencido31a60; }
        public int getQtdVencido61a90() { return qtdVencido61a90; }
        public void setQtdVencido61a90(int qtdVencido61a90) { this.qtdVencido61a90 = qtdVencido61a90; }
        public int getQtdVencidoMais90() { return qtdVencidoMais90; }
        public void setQtdVencidoMais90(int qtdVencidoMais90) { this.qtdVencidoMais90 = qtdVencidoMais90; }
    }

    public static class RecebimentosResumoDTO {
        private Double totalRecebidoMes = 0.0;
        private Double totalRecebidoHoje = 0.0;
        private Double totalRecebidoPeriodo = 0.0;
        private Double totalEmAberto = 0.0;
        private Double totalVencido = 0.0;
        private Double totalAVencer = 0.0;
        private Double taxaCobranca = 100.0;
        private Double ticketMedio = 0.0;
        private long qtdRecibosMes = 0;
        private long qtdRecibosHoje = 0;
        private long qtdRecibosPeriodo = 0;
        private long qtdContasPendentes = 0;
        private long qtdContasVencidas = 0;

        public Double getTotalRecebidoMes() { return totalRecebidoMes != null ? totalRecebidoMes : 0.0; }
        public void setTotalRecebidoMes(Double totalRecebidoMes) { this.totalRecebidoMes = totalRecebidoMes; }
        public Double getTotalRecebidoHoje() { return totalRecebidoHoje != null ? totalRecebidoHoje : 0.0; }
        public void setTotalRecebidoHoje(Double totalRecebidoHoje) { this.totalRecebidoHoje = totalRecebidoHoje; }
        public Double getTotalRecebidoPeriodo() { return totalRecebidoPeriodo != null ? totalRecebidoPeriodo : 0.0; }
        public void setTotalRecebidoPeriodo(Double totalRecebidoPeriodo) { this.totalRecebidoPeriodo = totalRecebidoPeriodo; }
        public Double getTotalEmAberto() { return totalEmAberto != null ? totalEmAberto : 0.0; }
        public void setTotalEmAberto(Double totalEmAberto) { this.totalEmAberto = totalEmAberto; }
        public Double getTotalVencido() { return totalVencido != null ? totalVencido : 0.0; }
        public void setTotalVencido(Double totalVencido) { this.totalVencido = totalVencido; }
        public Double getTotalAVencer() { return totalAVencer != null ? totalAVencer : 0.0; }
        public void setTotalAVencer(Double totalAVencer) { this.totalAVencer = totalAVencer; }
        public Double getTaxaCobranca() { return taxaCobranca != null ? taxaCobranca : 100.0; }
        public void setTaxaCobranca(Double taxaCobranca) { this.taxaCobranca = taxaCobranca; }
        public double getTaxaCobrancaProgress() {
            if (taxaCobranca == null) return 100.0;
            if (taxaCobranca > 100.0) return 100.0;
            if (taxaCobranca < 0.0) return 0.0;
            return taxaCobranca;
        }
        public Double getTicketMedio() { return ticketMedio != null ? ticketMedio : 0.0; }
        public void setTicketMedio(Double ticketMedio) { this.ticketMedio = ticketMedio; }
        public long getQtdRecibosMes() { return qtdRecibosMes; }
        public void setQtdRecibosMes(long qtdRecibosMes) { this.qtdRecibosMes = qtdRecibosMes; }
        public long getQtdRecibosHoje() { return qtdRecibosHoje; }
        public void setQtdRecibosHoje(long qtdRecibosHoje) { this.qtdRecibosHoje = qtdRecibosHoje; }
        public long getQtdRecibosPeriodo() { return qtdRecibosPeriodo; }
        public void setQtdRecibosPeriodo(long qtdRecibosPeriodo) { this.qtdRecibosPeriodo = qtdRecibosPeriodo; }
        public long getQtdContasPendentes() { return qtdContasPendentes; }
        public void setQtdContasPendentes(long qtdContasPendentes) { this.qtdContasPendentes = qtdContasPendentes; }
        public long getQtdContasVencidas() { return qtdContasVencidas; }
        public void setQtdContasVencidas(long qtdContasVencidas) { this.qtdContasVencidas = qtdContasVencidas; }
    }
}
