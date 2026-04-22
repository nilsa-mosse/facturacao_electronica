package ao.co.hzconsultoria.efacturacao.model;

public class ConfiguracaoStorage {

    private String tipoStorage = "LOCAL"; // LOCAL, CLOUD
    private String caminhoBase = "uploads/";
    private int tamanhoMaxFicheiro = 10;   // MB
    private int tamanhoMaxRequest = 20;    // MB
    private String estrategiaBackup = "DIARIO"; // DIARIO, SEMANAL, MENSAL, DESATIVADO
    private String cloudProvider = "";     // S3, GCS, AZURE
    private String cloudBucket = "";
    private String cloudRegion = "";

    public String getTipoStorage() { return tipoStorage; }
    public void setTipoStorage(String tipoStorage) { this.tipoStorage = tipoStorage; }

    public String getCaminhoBase() { return caminhoBase; }
    public void setCaminhoBase(String caminhoBase) { this.caminhoBase = caminhoBase; }

    public int getTamanhoMaxFicheiro() { return tamanhoMaxFicheiro; }
    public void setTamanhoMaxFicheiro(int tamanhoMaxFicheiro) { this.tamanhoMaxFicheiro = tamanhoMaxFicheiro; }

    public int getTamanhoMaxRequest() { return tamanhoMaxRequest; }
    public void setTamanhoMaxRequest(int tamanhoMaxRequest) { this.tamanhoMaxRequest = tamanhoMaxRequest; }

    public String getEstrategiaBackup() { return estrategiaBackup; }
    public void setEstrategiaBackup(String estrategiaBackup) { this.estrategiaBackup = estrategiaBackup; }

    public String getCloudProvider() { return cloudProvider; }
    public void setCloudProvider(String cloudProvider) { this.cloudProvider = cloudProvider; }

    public String getCloudBucket() { return cloudBucket; }
    public void setCloudBucket(String cloudBucket) { this.cloudBucket = cloudBucket; }

    public String getCloudRegion() { return cloudRegion; }
    public void setCloudRegion(String cloudRegion) { this.cloudRegion = cloudRegion; }
}
