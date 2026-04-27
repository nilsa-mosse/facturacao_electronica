# Documentação: Sistema de Configurações por Empresa

## Visão Geral

Este documento descreve a arquitetura do novo sistema de configurações por empresa implementado no sistema de facturação eletrônica.

## Problema Resolvido

**Antes:** As configurações (SMTP, Storage, Segurança, AGT) eram globais para todo o sistema, armazenadas em `ConfiguracaoSistemaEntity`.

**Depois:** Cada empresa tem suas próprias configurações independentes, permitindo que múltiplas empresas coexistam no sistema com suas configurações específicas.

## Arquitetura

### 1. Modelo de Dados

#### ConfiguracaoEmpresa (Nova Entidade)
```
ConfiguracaoEmpresa
├── id: Long (Chave Primária)
├── empresa: Empresa (FK One-to-One)
├── Configurações de Email/SMTP
│   ├── emailSmtpHost
│   ├── emailSmtpPorta
│   ├── emailSmtpUsername
│   ├── emailSmtpPassword
│   ├── emailSegurancaTipo
│   ├── emailRemetente
│   ├── emailNomeRemetente
│   └── emailHabilitado
├── Configurações de Storage
│   ├── storageTipo
│   ├── storageCaminhoBase
│   ├── storageTamanhoMaxFicheiro
│   ├── storageTamanhoMaxRequest
│   ├── storageEstrategiaBackup
│   ├── storageCloudProvider
│   ├── storageCloudBucket
│   ├── storageCloudRegion
│   └── storageBackupHabilitado
├── Configurações de Segurança
│   ├── segTempoExpiracaoSessao
│   ├── segTwoFactorAtivo
│   ├── segRequireUppercase
│   ├── segRequireNumbers
│   ├── segRequireSpecialChars
│   ├── segComprimentoMinPassword
│   ├── segIpWhitelist
│   └── segLogAcessosAtivo
├── Configurações de Notificação
│   ├── notificacaoEmailHabilitada
│   ├── notificacaoSmsHabilitada
│   ├── notificacaoSmsProvider
│   └── notificacaoSmsApiKey
├── Configurações de AGT
│   ├── agtIntegracaoHabilitada
│   ├── agtUrlServico
│   ├── agtUsuario
│   ├── agtSenha
│   └── agtCertificado
└── Preferências de Documentos
    ├── usarLogotipoEmDocumentos
    ├── usarCabeçalhoPersonalizadoEmDocumentos
    ├── usarRodapéPersonalizadoEmDocumentos
    └── rodapePersonalizado
```

#### Relação User → Empresa → ConfiguracaoEmpresa
```
User (Utilizador)
  └─ ManyToOne
      └─ Empresa
          └─ OneToOne
              └─ ConfiguracaoEmpresa
```

**Resultado:** Cada utilizador automaticamente tem acesso às configurações da sua empresa através desta hierarquia de relacionamentos.

### 2. Camada de Serviço

#### ConfiguracaoEmpresaService
Principais responsabilidades:
- `obterConfiguracao(Long empresaId)`: Obtém ou cria (com padrões) as configurações
- `salvarConfiguracao(ConfiguracaoEmpresa)`: Persiste alterações
- `atualizarConfiguracaoEmail(...)`: Atualiza apenas parâmetros de email
- `atualizarConfiguracaoStorage(...)`: Atualiza apenas parâmetros de storage
- `atualizarPoliticaSeguranca(...)`: Atualiza apenas parâmetros de segurança
- `atualizarConfiguracaoAGT(...)`: Atualiza apenas parâmetros de AGT
- `temEmailConfigurado(Long empresaId)`: Valida se email está pronto
- `temAgtConfigurada(Long empresaId)`: Valida se AGT está pronta

### 3. Controllers

#### ConfiguracaoController (Atualizado)
Novos endpoints adicionados:

- `GET /configuracoes/empresa/configuracoes`
  - Exibe formulário com as configurações da empresa do utilizador autenticado

- `POST /configuracoes/empresa/salvar-email`
  - Salva configurações de SMTP para a empresa
  - Parâmetros: smtpHost, smtpPorta, smtpUsername, smtpPassword, segurancaTipo, remetente, nomeRemetente
  - Resposta JSON: `{sucesso: boolean, mensagem: string}`

- `POST /configuracoes/empresa/salvar-storage`
  - Salva configurações de armazenamento
  - Parâmetros: storageTipo, caminhoBase, tamanhoMaxFicheiro, tamanhoMaxRequest
  - Resposta JSON: `{sucesso: boolean, mensagem: string}`

- `POST /configuracoes/empresa/salvar-seguranca`
  - Salva políticas de segurança
  - Parâmetros: tempoExpiracaoSessao, twoFactorAtivo, requireUppercase, requireNumbers, requireSpecialChars, comprimentoMinPassword
  - Resposta JSON: `{sucesso: boolean, mensagem: string}`

- `POST /configuracoes/empresa/salvar-agt`
  - Salva configuração de integração com AGT
  - Parâmetros: habilitada, urlServico, usuario, senha, certificado
  - Resposta JSON: `{sucesso: boolean, mensagem: string}`

### 4. Utilitários de Segurança

#### SecurityUtils (Expandido)
Novos métodos:
- `getCurrentUser()`: Retorna os dados completos do utilizador autenticado
- `temAcessoEmpresa(Long empresaId)`: Valida se o utilizador tem acesso à empresa

**Uso:** Previne que utilizadores acessem ou modifiquem configurações de empresas às quais não pertencem.

## Fluxo de Acesso

### 1. Utilizador Autentica
```
Login → CustomUserDetailsService → CustomUserDetails (contém empresaId)
```

### 2. Acesso às Configurações
```
GET /configuracoes/empresa/configuracoes
  ↓
SecurityUtils.getCurrentEmpresaId() → Obtém ID da empresa do utilizador
  ↓
ConfiguracaoEmpresaService.obterConfiguracao(empresaId)
  ↓
ConfiguracaoEmpresaRepository.findByEmpresa_Id(empresaId)
  ↓
Retorna ConfiguracaoEmpresa específica da empresa
```

### 3. Atualizar Configurações
```
POST /configuracoes/empresa/salvar-email
  ↓
SecurityUtils.temAcessoEmpresa(empresaId) → Valida acesso
  ↓
ConfiguracaoEmpresaService.atualizarConfiguracaoEmail(...)
  ↓
ConfiguracaoEmpresaRepository.save(config)
  ↓
Retorna {sucesso: true}
```

## Benefícios

✅ **Isolamento de Dados:** Cada empresa tem suas configurações independentes  
✅ **Segurança:** Utilizadores só podem acessar configurações da sua empresa  
✅ **Escalabilidade:** Suporta múltiplas empresas sem conflitos  
✅ **Flexibilidade:** Cada empresa pode usar SMTP, storage e políticas diferentes  
✅ **Auditoria:** Configurações podem ser rastreadas por empresa  

## Migração de Dados

Para migrar configurações de `ConfiguracaoSistemaEntity` para `ConfiguracaoEmpresa`:

```sql
INSERT INTO configuracao_empresa (empresa_id, emailSmtpHost, emailSmtpPorta, ...)
SELECT 1, emailSmtpHost, emailSmtpPorta, ... FROM configuracao_sistema WHERE id = 1;
```

## Exemplo de Uso

### Em um Serviço
```java
@Autowired
private ConfiguracaoEmpresaService configuracaoEmpresaService;

public void enviarEmail(Long empresaId) {
    ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresaId);
    
    if (configuracaoEmpresaService.temEmailConfigurado(empresaId)) {
        String smtpHost = config.getEmailSmtpHost();
        int porta = config.getEmailSmtpPorta();
        // ... enviar email com SMTP da empresa específica
    }
}
```

### Em um Controller
```java
@GetMapping("/minhas-configuracoes")
public String minhasConfiguracoes(Model model) {
    Long empresaId = SecurityUtils.getCurrentEmpresaId();
    ConfiguracaoEmpresa config = configuracaoEmpresaService.obterConfiguracao(empresaId);
    model.addAttribute("config", config);
    return "minhas-configuracoes";
}
```

## Testes Recomendados

1. **Teste de Isolamento:** Criar 2 empresas com configurações diferentes, verificar que cada uma vê apenas suas próprias
2. **Teste de Segurança:** Tentar acessar /configuracoes/empresa/salvar-email com token de outra empresa (deve ser negado)
3. **Teste de Padrões:** Criar nova empresa sem configurações, verificar que padrões são aplicados
4. **Teste de Atualização:** Atualizar email de uma empresa, verificar que não afeta outras

## Estrutura de Arquivos Criados

```
src/main/java/ao/co/hzconsultoria/efacturacao/
├── model/
│   ├── ConfiguracaoEmpresa.java (NOVO)
│   └── Empresa.java (MODIFICADO)
├── repository/
│   ├── ConfiguracaoEmpresaRepository.java (NOVO)
│   └── ... (outros repositórios)
├── service/
│   ├── ConfiguracaoEmpresaService.java (NOVO)
│   └── ... (outros serviços)
├── controller/
│   └── ConfiguracaoController.java (MODIFICADO)
└── security/
    └── SecurityUtils.java (MODIFICADO)
```

## Compatibilidade

- Spring Boot 2.x+
- JPA/Hibernate
- PostgreSQL, MySQL, H2 (qualquer BD suportada por Spring Data JPA)

## Próximos Passos Sugeridos

1. Criar template HTML para exibição das configurações (`configuracoes/empresa-config.html`)
2. Implementar endpoints para listar histórico de alterações nas configurações
3. Adicionar validação de SMTP (teste de conexão)
4. Implementar criptografia de senhas sensíveis (emailSmtpPassword, agtSenha)
5. Criar endpoints de teste para validar configurações antes de salvar
