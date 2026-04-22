package ao.co.hzconsultoria.efacturacao.model;

public class ConfiguracaoDatabase {

    private String tipoBD = "MySQL";
    private int connectionTimeout = 30000; // ms
    private int queryTimeout = 60000;      // ms
    private int poolMin = 5;
    private int poolMax = 20;
    private int idleTimeout = 30000;       // ms
    private int maxLifetime = 60000;       // ms
    private String schema = "efacturacao";

    public String getTipoBD() { return tipoBD; }
    public void setTipoBD(String tipoBD) { this.tipoBD = tipoBD; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getQueryTimeout() { return queryTimeout; }
    public void setQueryTimeout(int queryTimeout) { this.queryTimeout = queryTimeout; }

    public int getPoolMin() { return poolMin; }
    public void setPoolMin(int poolMin) { this.poolMin = poolMin; }

    public int getPoolMax() { return poolMax; }
    public void setPoolMax(int poolMax) { this.poolMax = poolMax; }

    public int getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(int idleTimeout) { this.idleTimeout = idleTimeout; }

    public int getMaxLifetime() { return maxLifetime; }
    public void setMaxLifetime(int maxLifetime) { this.maxLifetime = maxLifetime; }

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
}
