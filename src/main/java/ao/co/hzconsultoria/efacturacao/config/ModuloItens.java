package ao.co.hzconsultoria.efacturacao.config;

import java.util.*;

/**
 * Definição central dos itens (funcionalidades) de cada módulo.
 * Utilizado no Controlo de Acesso e na inicialização de permissões.
 */
public class ModuloItens {

    /**
     * Mapa imutável: módulo → lista ordenada de items (chave, rótulo).
     */
    public static final Map<String, List<ItemDef>> ITENS_POR_MODULO;
    public static final Map<String, String> MODULO_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("DASHBOARD", "DASHBOARD");
        labels.put("VENDAS", "VENDAS & OPERAÇÕES");
        labels.put("STOCK", "GESTÃO DE STOCK");
        labels.put("ENTIDADES", "ENTIDADES");
        labels.put("FACTURACAO", "FACTURAÇÃO & FISCAL");
        labels.put("FINANCEIRO", "FINANCEIRO");
        labels.put("ADMINISTRACAO", "ADMINISTRAÇÃO");
        labels.put("PAINEL_GLOBAL", "PAINEL GLOBAL (SaaS)");
        MODULO_LABELS = Collections.unmodifiableMap(labels);

        Map<String, List<ItemDef>> m = new LinkedHashMap<>();

        m.put("DASHBOARD", Arrays.asList(
                new ItemDef("RESUMO_GERAL",      "Resumo Geral",          "fas fa-chart-pie"),
                new ItemDef("INDICADORES_KPI",   "Indicadores KPI",       "fas fa-tachometer-alt"),
                new ItemDef("ALERTAS",           "Alertas do Sistema",    "fas fa-bell")
        ));

        m.put("VENDAS", Arrays.asList(
                new ItemDef("NOVA_VENDA",        "Nova Venda (PDV)",      "fas fa-cash-register"),
                new ItemDef("HISTORICO_VENDAS",  "Histórico de Vendas",   "fas fa-history"),
                new ItemDef("DEVOLUCOES",        "Devoluções",            "fas fa-undo"),
                new ItemDef("GUIAS_REMESSA",     "Guias de Remessa",      "fas fa-truck"),
                new ItemDef("NOTAS_CREDITO",     "Notas de Crédito",      "fas fa-file-invoice-dollar")
        ));

        m.put("STOCK", Arrays.asList(
                new ItemDef("PRODUTOS",          "Produtos / Catálogo",   "fas fa-box"),
                new ItemDef("MOVIMENTOS_STOCK",  "Movimentos de Stock",   "fas fa-exchange-alt"),
                new ItemDef("INVENTARIO",        "Inventário",            "fas fa-clipboard-list"),
                new ItemDef("COMPRAS",           "Compras / Fornecedores","fas fa-shopping-cart"),
                new ItemDef("CATEGORIAS",        "Categorias",            "fas fa-tags")
        ));

        m.put("ENTIDADES", Arrays.asList(
                new ItemDef("CLIENTES",          "Clientes",              "fas fa-users"),
                new ItemDef("FORNECEDORES",      "Fornecedores",          "fas fa-building"),
                new ItemDef("ESTADO_ENTIDADES",  "Estados de Entidades",  "fas fa-toggle-on")
        ));

        m.put("FACTURACAO", Arrays.asList(
                new ItemDef("EMITIR_FATURA",     "Emitir Factura",        "fas fa-file-invoice"),
                new ItemDef("LISTAR_FATURAS",    "Listar Facturas",       "fas fa-list"),
                new ItemDef("SERIES",            "Séries de Facturação",  "fas fa-sort-numeric-up"),
                new ItemDef("SAFT",              "SAF-T / AGT",           "fas fa-file-export")
        ));

        m.put("FINANCEIRO", Arrays.asList(
                new ItemDef("DESPESAS",          "Despesas",              "fas fa-money-bill-wave"),
                new ItemDef("RELATORIOS_FIN",    "Relatórios Financeiros","fas fa-chart-bar"),
                new ItemDef("IMPOSTOS",          "Impostos e Taxas",      "fas fa-percentage"),
                new ItemDef("MOEDAS",            "Moedas e Câmbios",      "fas fa-coins")
        ));

        m.put("ADMINISTRACAO", Arrays.asList(
                new ItemDef("UTILIZADORES",      "Utilizadores",          "fas fa-user-cog"),
                new ItemDef("ESTABELECIMENTOS",  "Estabelecimentos",      "fas fa-store"),
                new ItemDef("CONTROLO_ACESSO",   "Controlo de Acesso",    "fas fa-user-shield"),
                new ItemDef("CONFIGURACOES",     "Configurações Gerais",  "fas fa-sliders-h"),
                new ItemDef("CONFIGURACOES_AGT", "Comunicação AGT",       "fas fa-plug")
        ));

        m.put("PAINEL_GLOBAL", Arrays.asList(
                new ItemDef("EMPRESAS",          "Gestão de Empresas",    "fas fa-city"),
                new ItemDef("PLANOS",            "Planos e Subscrições",  "fas fa-credit-card"),
                new ItemDef("LOGS_SISTEMA",       "Logs do Sistema",       "fas fa-file-medical-alt"),
                new ItemDef("RECURSOS_SRV",      "Recursos do Servidor",  "fas fa-microchip")
        ));

        ITENS_POR_MODULO = Collections.unmodifiableMap(m);
    }

    /** Retorna todos os items de um módulo, ou lista vazia se desconhecido. */
    public static List<ItemDef> getItens(String modulo) {
        return ITENS_POR_MODULO.getOrDefault(modulo, Collections.emptyList());
    }

    // ─── Inner class ─────────────────────────────────────────────────────────

    public static class ItemDef {
        private final String chave;
        private final String rotulo;
        private final String icone;

        public ItemDef(String chave, String rotulo, String icone) {
            this.chave = chave;
            this.rotulo = rotulo;
            this.icone = icone;
        }

        public String getChave()  { return chave; }
        public String getRotulo() { return rotulo; }
        public String getIcone()  { return icone; }
    }
}
