# Exemplos de Uso - API de Configurações por Empresa

## 🔗 Exemplos de Requests HTTP

### Base URL
```
http://localhost:8080/configuracoes/empresa
```

### 1. Obter Configurações da Empresa

#### Request
```bash
curl -X GET "http://localhost:8080/configuracoes/empresa/configuracoes" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

#### Response (200 OK)
```json
{
  "id": 1,
  "empresa": {
    "id": 1,
    "nome": "Minha Empresa Ltda",
    "nif": "123456789",
    "email": "contato@empresa.com"
  },
  "emailSmtpHost": "smtp.gmail.com",
  "emailSmtpPorta": 587,
  "emailSmtpUsername": "usuario@empresa.com",
  "emailSegurancaTipo": "TLS",
  "emailRemetente": "noreply@empresa.com",
  "emailNomeRemetente": "Minha Empresa",
  "emailHabilitado": true,
  "storageTipo": "LOCAL",
  "storageCaminhoBase": "./uploads/",
  "storageTamanhoMaxFicheiro": 50,
  "storageTamanhoMaxRequest": 100,
  "segTempoExpiracaoSessao": 30,
  "segTwoFactorAtivo": false,
  "usarLogotipoEmDocumentos": true,
  "usarRodapéPersonalizadoEmDocumentos": false
}
```

---

### 2. Configurar Email/SMTP

#### Request
```bash
curl -X POST "http://localhost:8080/configuracoes/empresa/salvar-email" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "smtpHost=smtp.seuservidor.com" \
  -d "smtpPorta=587" \
  -d "smtpUsername=usuario@empresa.com" \
  -d "smtpPassword=SenhaSegura123!" \
  -d "segurancaTipo=TLS" \
  -d "remetente=noreply@empresa.com" \
  -d "nomeRemetente=Minha Empresa"
```

#### Response (200 OK)
```json
{
  "sucesso": true,
  "mensagem": "Configuração salva com sucesso"
}
```

#### Response (403 Forbidden - Acesso Negado)
```json
{
  "sucesso": false,
  "mensagem": "Acesso negado"
}
```

---

### 3. Configurar Storage

#### Request
```bash
curl -X POST "http://localhost:8080/configuracoes/empresa/salvar-storage" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "storageTipo=S3" \
  -d "caminhoBase=s3://meu-bucket/uploads/" \
  -d "tamanhoMaxFicheiro=100" \
  -d "tamanhoMaxRequest=500"
```

#### Response (200 OK)
```json
{
  "sucesso": true,
  "mensagem": "Configuração salva com sucesso"
}
```

---

### 4. Configurar Políticas de Segurança

#### Request
```bash
curl -X POST "http://localhost:8080/configuracoes/empresa/salvar-seguranca" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "tempoExpiracaoSessao=60" \
  -d "twoFactorAtivo=true" \
  -d "requireUppercase=true" \
  -d "requireNumbers=true" \
  -d "requireSpecialChars=true" \
  -d "comprimentoMinPassword=12"
```

#### Response (200 OK)
```json
{
  "sucesso": true,
  "mensagem": "Configuração salva com sucesso"
}
```

---

### 5. Configurar Integração com AGT

#### Request
```bash
curl -X POST "http://localhost:8080/configuracoes/empresa/salvar-agt" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "habilitada=true" \
  -d "urlServico=https://api.agt.gov.ao/v1" \
  -d "usuario=seu_usuario_agt" \
  -d "senha=sua_senha_criptografada" \
  -d "certificado=caminho/certificado.pem"
```

#### Response (200 OK)
```json
{
  "sucesso": true,
  "mensagem": "Configuração salva com sucesso"
}
```

#### Response (400 Bad Request)
```json
{
  "sucesso": false,
  "mensagem": "Erro ao salvar: URL do serviço AGT inválida"
}
```

---

## 🧪 Exemplos com JavaScript/Fetch API

### Obter Configurações
```javascript
async function obterConfiguracoes() {
  try {
    const response = await fetch('/configuracoes/empresa/configuracoes', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${tokenJWT}`,
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const config = await response.json();
    console.log('Configurações da empresa:', config);
    return config;
  } catch (error) {
    console.error('Erro ao obter configurações:', error);
  }
}
```

### Atualizar Email
```javascript
async function salvarConfiguracaoEmail(dados) {
  try {
    const formData = new FormData();
    formData.append('smtpHost', dados.smtpHost);
    formData.append('smtpPorta', dados.smtpPorta);
    formData.append('smtpUsername', dados.smtpUsername);
    formData.append('smtpPassword', dados.smtpPassword);
    formData.append('segurancaTipo', dados.segurancaTipo);
    formData.append('remetente', dados.remetente);
    formData.append('nomeRemetente', dados.nomeRemetente);

    const response = await fetch('/configuracoes/empresa/salvar-email', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${tokenJWT}`
      },
      body: formData
    });

    const resultado = await response.json();
    if (resultado.sucesso) {
      alert('Email configurado com sucesso!');
    } else {
      alert(`Erro: ${resultado.mensagem}`);
    }
    return resultado;
  } catch (error) {
    console.error('Erro ao salvar email:', error);
  }
}
```

### Atualizar Segurança
```javascript
async function salvarPoliticaSeguranca(dados) {
  try {
    const formData = new FormData();
    formData.append('tempoExpiracaoSessao', dados.tempoExpiracaoSessao);
    formData.append('twoFactorAtivo', dados.twoFactorAtivo);
    formData.append('requireUppercase', dados.requireUppercase);
    formData.append('requireNumbers', dados.requireNumbers);
    formData.append('requireSpecialChars', dados.requireSpecialChars);
    formData.append('comprimentoMinPassword', dados.comprimentoMinPassword);

    const response = await fetch('/configuracoes/empresa/salvar-seguranca', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${tokenJWT}`
      },
      body: formData
    });

    const resultado = await response.json();
    if (resultado.sucesso) {
      alert('Política de segurança atualizada!');
    } else {
      alert(`Erro: ${resultado.mensagem}`);
    }
    return resultado;
  } catch (error) {
    console.error('Erro ao salvar segurança:', error);
  }
}
```

---

## 📱 Exemplos com React

### Hook para Obter Configurações
```javascript
import { useEffect, useState } from 'react';
import { useAuth } from './AuthContext';

export const useConfiguracaoEmpresa = () => {
  const [config, setConfig] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { token } = useAuth();

  useEffect(() => {
    const carregarConfig = async () => {
      try {
        const response = await fetch('/configuracoes/empresa/configuracoes', {
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          }
        });

        if (!response.ok) {
          throw new Error('Erro ao carregar configurações');
        }

        const dados = await response.json();
        setConfig(dados);
      } catch (err) {
        setError(err.message);
      } finally {
        setLoading(false);
      }
    };

    if (token) {
      carregarConfig();
    }
  }, [token]);

  return { config, loading, error };
};
```

### Componente de Formulário
```javascript
import React, { useState } from 'react';
import { useAuth } from './AuthContext';

export const FormularioEmailEmpresa = () => {
  const { token } = useAuth();
  const [formData, setFormData] = useState({
    smtpHost: 'smtp.gmail.com',
    smtpPorta: 587,
    smtpUsername: '',
    smtpPassword: '',
    segurancaTipo: 'TLS',
    remetente: '',
    nomeRemetente: ''
  });
  const [enviando, setEnviando] = useState(false);
  const [mensagem, setMensagem] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'smtpPorta' ? parseInt(value) : value
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setEnviando(true);
    setMensagem('');

    try {
      const formDataObj = new FormData();
      Object.keys(formData).forEach(key => {
        formDataObj.append(key, formData[key]);
      });

      const response = await fetch('/configuracoes/empresa/salvar-email', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        },
        body: formDataObj
      });

      const resultado = await response.json();

      if (resultado.sucesso) {
        setMensagem('✅ ' + resultado.mensagem);
      } else {
        setMensagem('❌ ' + resultado.mensagem);
      }
    } catch (error) {
      setMensagem('❌ Erro ao salvar: ' + error.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="formulario-email">
      <div className="form-group">
        <label>SMTP Host:</label>
        <input
          type="text"
          name="smtpHost"
          value={formData.smtpHost}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form-group">
        <label>Porta SMTP:</label>
        <input
          type="number"
          name="smtpPorta"
          value={formData.smtpPorta}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form-group">
        <label>Usuário SMTP:</label>
        <input
          type="email"
          name="smtpUsername"
          value={formData.smtpUsername}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form-group">
        <label>Senha SMTP:</label>
        <input
          type="password"
          name="smtpPassword"
          value={formData.smtpPassword}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form-group">
        <label>Segurança:</label>
        <select
          name="segurancaTipo"
          value={formData.segurancaTipo}
          onChange={handleChange}
        >
          <option value="TLS">TLS</option>
          <option value="SSL">SSL</option>
          <option value="NONE">Nenhuma</option>
        </select>
      </div>

      <div className="form-group">
        <label>Email Remetente:</label>
        <input
          type="email"
          name="remetente"
          value={formData.remetente}
          onChange={handleChange}
          required
        />
      </div>

      <div className="form-group">
        <label>Nome do Remetente:</label>
        <input
          type="text"
          name="nomeRemetente"
          value={formData.nomeRemetente}
          onChange={handleChange}
          required
        />
      </div>

      <button type="submit" disabled={enviando}>
        {enviando ? 'Salvando...' : 'Salvar Configuração'}
      </button>

      {mensagem && <div className="mensagem">{mensagem}</div>}
    </form>
  );
};
```

---

## 🔐 Códigos de Erro Comuns

| Código | Significado | Solução |
|--------|-------------|---------|
| 200 | OK - Operação bem-sucedida | ✅ Nenhuma ação necessária |
| 400 | Bad Request - Parâmetros inválidos | Verificar parâmetros enviados |
| 401 | Unauthorized - Token inválido | Fazer login novamente |
| 403 | Forbidden - Sem acesso à empresa | Pertence a outra empresa |
| 404 | Not Found - Endpoint não existe | Verificar URL |
| 500 | Server Error - Erro interno | Contactar suporte |

---

## 📝 Checklist de Implementação

Ao implementar a integração com essas APIs:

- [ ] Token JWT válido no Authorization header
- [ ] Content-Type correto (application/x-www-form-urlencoded)
- [ ] Validar resposta JSON antes de usar
- [ ] Tratar erros de rede e timeouts
- [ ] Implementar cache de configurações (opcional)
- [ ] Criptografar senhas antes de enviar (HTTPS obrigatório)
- [ ] Validar dados no lado do cliente
- [ ] Exibir mensagens de sucesso/erro ao utilizador
- [ ] Implementar retry em caso de falha
- [ ] Logar erros para debugging

---

## 🚀 Dicas de Performance

1. **Cache Local**: Armazene as configurações em localStorage
2. **Polling**: Recarregue a cada 5-10 minutos
3. **WebSocket**: Para atualizações em tempo real
4. **Compress**: Use gzip para respostas
5. **CDN**: Para arquivos estáticos

---

## 📚 Referências Úteis

- Documentação: `/CONFIGURACOES_EMPRESA.md`
- Implementação: `/IMPLEMENTACAO_FINAL.md`
- Testes: `ConfiguracaoEmpresaServiceTest.java`
- SQL: `V2__Create_Configuracao_Empresa.sql`
