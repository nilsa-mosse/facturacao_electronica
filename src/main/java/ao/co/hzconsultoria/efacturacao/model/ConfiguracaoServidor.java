package ao.co.hzconsultoria.efacturacao.model;

public class ConfiguracaoServidor {

    private int porta = 8080;
    private String hostname = "localhost";
    private String baseUrl = "http://localhost:8080";
    private boolean proxyHabilitado = false;
    private String proxyHost = "";
    private int proxyPorta = 80;
    private String corsOrigensPermitidas = "*";

    public int getPorta() { return porta; }
    public void setPorta(int porta) { this.porta = porta; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public boolean isProxyHabilitado() { return proxyHabilitado; }
    public void setProxyHabilitado(boolean proxyHabilitado) { this.proxyHabilitado = proxyHabilitado; }

    public String getProxyHost() { return proxyHost; }
    public void setProxyHost(String proxyHost) { this.proxyHost = proxyHost; }

    public int getProxyPorta() { return proxyPorta; }
    public void setProxyPorta(int proxyPorta) { this.proxyPorta = proxyPorta; }

    public String getCorsOrigensPermitidas() { return corsOrigensPermitidas; }
    public void setCorsOrigensPermitidas(String corsOrigensPermitidas) { this.corsOrigensPermitidas = corsOrigensPermitidas; }
}
