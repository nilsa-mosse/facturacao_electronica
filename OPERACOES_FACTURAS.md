# Operações e Estados de Facturas FT

## Descrição

Este documento descreve as operações que podem ser realizadas sobre facturas do tipo "FT - Fatura" e os estados possíveis de uma factura no sistema.

## Estados de uma Factura

Uma factura no sistema pode estar em um dos seguintes estados:

| Estado | Descrição |
|--------|-----------|
| **RASCUNHO** | Factura acabada de criar, não foi ainda emitida |
| **EMITIDA** | Factura foi emitida mas não foi validada pela AGT |
| **VALIDADA AGT** | Factura foi validada e registada na Autoridade Geral Tributária (AGT) |
| **PARCIALMENTE PAGA** | Foram efectuados pagamentos parciais, mas ainda existe saldo em aberto |
| **PAGA** | Factura foi totalmente paga |
| **VENCIDA** | Factura ultrapassou a data de vencimento sem ser paga ou parcialmente paga |
| **ANULADA** | Factura foi anulada (apenas facturas em Rascunho ou Emitida podem ser anuladas) |

## Operações Disponíveis

Após uma factura ser criada com status "FT - Fatura", as seguintes operações podem ser realizadas:

### 1. **Imprimir**
Marca a factura como impressa no sistema.

**Endpoint:** `POST /api/faturas/operacoes/{id}/imprimir`

**Resposta:**
```json
{
  "mensagem": "Factura FT 2026/1 marcada como impressa",
  "numeroFatura": "FT 2026/1"
}
```

**Restrições:** 
- A factura deve estar em qualquer estado (excepto Anulada)

---

### 2. **Enviar por Email**
Envia a factura por email em formato PDF.

**Endpoint:** `POST /api/faturas/operacoes/{id}/enviar-email`

**Payload:**
```json
{
  "email": "cliente@example.com"
}
```

**Resposta:**
```json
{
  "mensagem": "Factura FT 2026/1 enviada com sucesso para cliente@example.com",
  "numeroFatura": "FT 2026/1",
  "email": "cliente@example.com"
}
```

**Restrições:**
- Email válido é obrigatório
- A factura não deve estar validada pela AGT (protegida contra alterações)

---

### 3. **Gerar PDF**
Gera ou regenera o PDF da factura.

**Endpoint:** `GET /api/faturas/operacoes/{id}/pdf`

**Resposta:**
```json
{
  "mensagem": "PDF gerado com sucesso",
  "numeroFatura": "FT 2026/1",
  "urlPdf": "/uploads/faturas/FT 2026/1.pdf"
}
```

---

### 4. **Registar Pagamento Parcial**
Registra um pagamento parcial da factura.

**Endpoint:** `POST /api/faturas/operacoes/{id}/pagamento-parcial`

**Payload:**
```json
{
  "valor": 500.00,
  "metodo": "MULTICAIXA",
  "referencia": "REF123456"
}
```

**Resposta:**
```json
{
  "mensagem": "Pagamento parcial registado com sucesso",
  "numeroFatura": "FT 2026/1",
  "status": "PARCIALMENTE_PAGA",
  "valorPago": 500.00,
  "valorEmAberto": 1500.00
}
```

**Restrições:**
- O valor deve ser positivo e não exceder o total em aberto
- A factura não deve estar validada pela AGT
- Gera automaticamente um Recibo (FR) para o pagamento

**Metodos suportados:** `CASH`, `MULTICAIXA`, `TPA`, `CARD`

---

### 5. **Registar Pagamento Total**
Marca a factura como completamente paga.

**Endpoint:** `POST /api/faturas/operacoes/{id}/pagamento-total`

**Payload:**
```json
{
  "metodo": "CASH",
  "referencia": null
}
```

**Resposta:**
```json
{
  "mensagem": "Pagamento total registado com sucesso",
  "numeroFatura": "FT 2026/1",
  "status": "PAGA",
  "valorPago": 2000.00,
  "Total": 2000.00
}
```

---

### 6. **Converter para Factura-Recibo**
Converte uma Factura (FT) em Factura-Recibo (FR) após recebimento do pagamento.

**Endpoint:** `POST /api/faturas/operacoes/{id}/converter-recibo`

**Resposta:**
```json
{
  "mensagem": "Factura convertida para Factura-Recibo com sucesso",
  "faturaOriginal": "FT 2026/1",
  "reciboNumero": "FR 2026/1",
  "reciboStatus": "VALIDADA AGT",
  "urlPdf": "/uploads/faturas/FR 2026/1.pdf"
}
```

---

### 7. **Emitir Nota de Crédito**
Emite uma Nota de Crédito (NC) em resposta a uma devolução.

**Endpoint:** `POST /api/devolucoes/{id}/nota-credito`

**Restrições:**
- Requer uma Devolucao registada
- Implementado no controlador de Devoluções

---

### 8. **Emitir Nota de Débito**
Emite uma Nota de Débito (ND) para incrementar o valor devido após emissão da FT.

**Endpoint:** `POST /api/faturas/operacoes/{id}/nota-debito`

**Payload:**
```json
{
  "valor": 250.00,
  "motivo": "Ajuste de preço conforme contrato"
}
```

**Resposta:**
```json
{
  "mensagem": "Nota de Débito emitida com sucesso",
  "faturaReferencia": "FT 2026/1",
  "notaDebito": "ND 2026/1",
  "valor": 250.00,
  "motivo": "Ajuste de preço conforme contrato",
  "urlPdf": "/uploads/faturas/ND 2026/1.pdf"
}
```

**Restrições:**
- Só pode ser emitida contra uma Factura (FT)
- O valor deve ser positivo

---

### 9. **Consultar Estado AGT**
Consulta o estado atual da factura junto à Autoridade Geral Tributária.

**Endpoint:** `GET /api/faturas/operacoes/{id}/estado-agt`

**Resposta:**
```json
{
  "numeroFatura": "FT 2026/1",
  "tipoDocumento": "FT",
  "dataEmissao": "2026-06-11T10:30:00",
  "enviadaAgt": true,
  "validadaAgt": true,
  "status": "VALIDADA AGT",
  "codigoAgt": "AGT123456",
  "hash": "abc123def456...",
  "mensagemAgt": "Factura já foi validada na AGT",
  "sucessoAgt": true
}
```

---

### 10. **Anular Factura**
Anula uma factura (apenas em estados Rascunho ou Emitida).

**Endpoint:** `POST /api/faturas/operacoes/{id}/anular`

**Payload:**
```json
{
  "motivo": "Factura emitida por erro"
}
```

**Resposta:**
```json
{
  "mensagem": "Factura anulada com sucesso",
  "numeroFatura": "FT 2026/1",
  "status": "ANULADA",
  "motivo": "Factura emitida por erro"
}
```

**Restrições:**
- Apenas facturas em estado "RASCUNHO" ou "EMITIDA" podem ser anuladas
- Uma vez anulada, a factura fica imutável

---

## Proteção de Dados Após Validação AGT

Uma aspecto crítico do sistema é que **após uma factura ser validada pela AGT (status VALIDADA AGT), ela não pode ser alterada nem eliminada do sistema**. 

Esta restrição é imposta para garantir:
- ✅ Integridade dos registos fiscais
- ✅ Conformidade com regulamentos da AGT
- ✅ Rastreabilidade das transações
- ✅ Auditoria

Qualquer tentativa de modificar uma factura validada resultará em erro:
```json
{
  "erro": "Factura FT 2026/1 foi validada pela AGT e não pode ser alterada ou eliminada."
}
```

---

## Fluxo Típico de uma Factura

```
RASCUNHO 
   ↓
(Emitir)
   ↓
EMITIDA → [Enviar Email] → [Imprimir]
   ↓
(Enviar para AGT)
   ↓
VALIDADA AGT [IMUTÁVEL - Protegida]
   ↓
(Registar Pagamento)
   ↓
PARCIALMENTE PAGA → (Continuar Pagando) → PAGA
   ↓
[Status fecha-se]
```

---

## Campos Registados Automaticamente

Para cada operação realizada, o sistema regista automaticamente:

- **dataImpressao**: Data e hora da impressão
- **dataEmail**: Data e hora do envio por email
- **valorPago**: Valor total pago até o momento
- **valorEmAberto**: Saldo em aberto
- **validadaAgt**: Flag indicando validação pela AGT

---

## Exemplos de Utilização

### Exemplo 1: Fluxo Completo de Pagamento

```bash
# 1. Criar e emitir factura (via outro endpoint)
POST /api/compras/{compraId}/emitir-factura

# 2. Registar pagamento parcial
POST /api/faturas/operacoes/{id}/pagamento-parcial
{
  "valor": 1000.00,
  "metodo": "MULTICAIXA"
}

# 3. Registar pagamento final
POST /api/faturas/operacoes/{id}/pagamento-total
{
  "metodo": "CASH"
}

# 4. Consultar estado
GET /api/faturas/operacoes/{id}/estado-agt
```

### Exemplo 2: Emitir Nota de Débito

```bash
# 1. Emitir Nota de Débito
POST /api/faturas/operacoes/{id}/nota-debito
{
  "valor": 150.00,
  "motivo": "Taxa de processamento adicional"
}

# 2. O cliente deverá pagar o novo valor (original + ND)
```

---

## Códigos de Erro Comuns

| Código | Mensagem | Solução |
|--------|----------|---------|
| 400 | Factura não encontrada | Verifique o ID da factura |
| 400 | Factura foi validada pela AGT e não pode ser alterada | Operação não permitida em facturas imutáveis |
| 400 | Valor de pagamento excede o total da factura | Reduza o valor |
| 500 | Erro ao enviar email | Verifique configurações SMTP |
| 500 | Erro ao gerar PDF | Verifique disposição dos ficheiros |

---

## Tecnologia e Padrões

- **Framework:** Spring Boot
- **Banco de Dados:** MySQL/H2 Database
- **Formato de Resposta:** JSON
- **Assinatura Criptográfica:** RSA SHA1
- **Formato de Documentos:** PDF (iText)
- **Armazenamento:** Sistema de ficheiros local (`./uploads/faturas/`)

---

## Próximas Melhorias

- [ ] Suporte para parcelamentos de pagamento
- [ ] Relatórios de devoluções automáticas
- [ ] Integração com processos de cobrança
- [ ] Alertas automáticos para facturas vencidas
- [ ] Exportação em outros formatos (XML, EDI)

