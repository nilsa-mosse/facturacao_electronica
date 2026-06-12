/*
 * Exemplos de testes cURL para os novos endpoints de operações com facturas
 * 
 * Para executar estes exemplos, certifique-se de que:
 * 1. O servidor está rodando em http://localhost:8080
 * 2. Substitua {id} pelo ID real de uma factura FT existente
 * 3. Substitua {email} por um email válido
 */

// ─────────────────────────────────────────────────────────────
// 1. IMPRIMIR FACTURA
// ─────────────────────────────────────────────────────────────

curl -X POST http://localhost:8080/api/faturas/operacoes/1/imprimir \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"

// Resposta esperada:
{
  "mensagem": "Factura FT 2026/1 marcada como impressa",
  "numeroFatura": "FT 2026/1"
}


// ─────────────────────────────────────────────────────────────
// 2. ENVIAR POR EMAIL
// ─────────────────────────────────────────────────────────────

curl -X POST http://localhost:8080/api/faturas/operacoes/1/enviar-email \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "email": "cliente@example.com"
  }'

// Resposta esperada:
{
  "mensagem": "Factura FT 2026/1 enviada com sucesso para cliente@example.com",
  "numeroFatura": "FT 2026/1",
  "email": "cliente@example.com"
}


// ─────────────────────────────────────────────────────────────
// 3. GERAR/DESCARREGAR PDF
// ─────────────────────────────────────────────────────────────

curl -X GET http://localhost:8080/api/faturas/operacoes/1/pdf \
  -H "Authorization: Bearer YOUR_TOKEN"

// Resposta esperada:
{
  "mensagem": "PDF gerado com sucesso",
  "numeroFatura": "FT 2026/1",
  "urlPdf": "/uploads/faturas/FT 2026/1.pdf"
}


// ─────────────────────────────────────────────────────────────
// 4. REGISTAR PAGAMENTO PARCIAL
// ─────────────────────────────────────────────────────────────

curl -X POST http://localhost:8080/api/faturas/operacoes/1/pagamento-parcial \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "valor": 500.00,
    "metodo": "MULTICAIXA",
    "referencia": "REF123456"
  }'

// Resposta esperada:
{
  "mensagem": "Pagamento parcial registado com sucesso",
  "numeroFatura": "FT 2026/1",
  "status": "PARCIALMENTE_PAGA",
  "valorPago": 500.00,
  "valorEmAberto": 1500.00
}


// ─────────────────────────────────────────────────────────────
// 5. REGISTAR PAGAMENTO TOTAL
// ─────────────────────────────────────────────────────────────

curl -X POST http://localhost:8080/api/faturas/operacoes/1/pagamento-total \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "metodo": "CASH",
    "referencia": null
  }'

// Resposta esperada:
{
  "mensagem": "Pagamento total registado com sucesso",
  "numeroFatura": "FT 2026/1",
  "status": "PAGA",
  "valorPago": 2000.00,
  "Total": 2000.00
}


// ─────────────────────────────────────────────────────────────
// 6. CONVERTER PARA FACTURA-RECIBO
// ─────────────────────────────────────────────────────────────

curl -X POST http://localhost:8080/api/faturas/operacoes/1/converter-recibo \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN"

// Resposta esperada:
{
  "mensagem": "Factura convertida para Factura-Recibo com sucesso",
  "faturaOriginal": "FT 2026/1",
  "reciboNumero": "FR 2026/1",
  "reciboStatus": "VALIDADA AGT",
  "urlPdf": "/uploads/faturas/FR 2026/1.pdf"
}


// ─────────────────────────────────────────────────────────────
// 7. EMITIR NOTA DE DÉBITO
// ─────────────────────────────────────────────────────────────

curl -X POST http://localhost:8080/api/faturas/operacoes/1/nota-debito \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "valor": 250.00,
    "motivo": "Ajuste de preço conforme contrato"
  }'

// Resposta esperada:
{
  "mensagem": "Nota de Débito emitida com sucesso",
  "faturaReferencia": "FT 2026/1",
  "notaDebito": "ND 2026/1",
  "valor": 250.00,
  "motivo": "Ajuste de preço conforme contrato",
  "urlPdf": "/uploads/faturas/ND 2026/1.pdf"
}


// ─────────────────────────────────────────────────────────────
// 8. CONSULTAR ESTADO AGT
// ─────────────────────────────────────────────────────────────

curl -X GET http://localhost:8080/api/faturas/operacoes/1/estado-agt \
  -H "Authorization: Bearer YOUR_TOKEN"

// Resposta esperada:
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


// ─────────────────────────────────────────────────────────────
// 9. ANULAR FACTURA
// ─────────────────────────────────────────────────────────────

curl -X POST http://localhost:8080/api/faturas/operacoes/1/anular \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "motivo": "Factura emitida por erro"
  }'

// Resposta esperada:
{
  "mensagem": "Factura anulada com sucesso",
  "numeroFatura": "FT 2026/1",
  "status": "ANULADA",
  "motivo": "Factura emitida por erro"
}


// ═════════════════════════════════════════════════════════════════════════
// TESTES EM JAVASCRIPT (Exemplo usando Fetch API)
// ═════════════════════════════════════════════════════════════════════════

// Função auxiliar para fazer requisições
const apiCall = async (method, endpoint, data = null) => {
  const options = {
    method: method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer YOUR_TOKEN'
    }
  };
  
  if (data) {
    options.body = JSON.stringify(data);
  }
  
  try {
    const response = await fetch(`http://localhost:8080${endpoint}`, options);
    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Erro:', error);
  }
};

// Teste 1: Registar pagamento parcial
async function testarPagamentoParcial() {
  const resultado = await apiCall(
    'POST',
    '/api/faturas/operacoes/1/pagamento-parcial',
    {
      valor: 500.00,
      metodo: 'MULTICAIXA',
      referencia: 'REF123456'
    }
  );
  console.log(resultado);
}

// Teste 2: Enviar por email
async function testarEnviarEmail() {
  const resultado = await apiCall(
    'POST',
    '/api/faturas/operacoes/1/enviar-email',
    {
      email: 'cliente@example.com'
    }
  );
  console.log(resultado);
}

// Teste 3: Consultar estado AGT
async function testarEstadoAgt() {
  const resultado = await apiCall(
    'GET',
    '/api/faturas/operacoes/1/estado-agt'
  );
  console.log(resultado);
}

// Teste 4: Anular factura
async function testarAnularFatura() {
  const resultado = await apiCall(
    'POST',
    '/api/faturas/operacoes/1/anular',
    {
      motivo: 'Factura emitida por erro'
    }
  );
  console.log(resultado);
}


// ═════════════════════════════════════════════════════════════════════════
// TESTES EM POSTMAN (JSON para importar)
// ═════════════════════════════════════════════════════════════════════════

{
  "info": {
    "name": "Operações de Facturas FT",
    "description": "Testes para os novos endpoints de operações com facturas",
    "version": "1.0"
  },
  "item": [
    {
      "name": "Imprimir",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/imprimir"
      }
    },
    {
      "name": "Enviar Email",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/enviar-email",
        "body": {
          "mode": "raw",
          "raw": "{\"email\": \"cliente@example.com\"}"
        }
      }
    },
    {
      "name": "Gerar PDF",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/pdf"
      }
    },
    {
      "name": "Pagamento Parcial",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/pagamento-parcial",
        "body": {
          "mode": "raw",
          "raw": "{\"valor\": 500.00, \"metodo\": \"MULTICAIXA\", \"referencia\": \"REF123456\"}"
        }
      }
    },
    {
      "name": "Pagamento Total",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/pagamento-total",
        "body": {
          "mode": "raw",
          "raw": "{\"metodo\": \"CASH\"}"
        }
      }
    },
    {
      "name": "Converter para Recibo",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/converter-recibo"
      }
    },
    {
      "name": "Nota de Débito",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/nota-debito",
        "body": {
          "mode": "raw",
          "raw": "{\"valor\": 250.00, \"motivo\": \"Ajuste de preço\"}"
        }
      }
    },
    {
      "name": "Estado AGT",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/estado-agt"
      }
    },
    {
      "name": "Anular Factura",
      "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/faturas/operacoes/{{faturaId}}/anular",
        "body": {
          "mode": "raw",
          "raw": "{\"motivo\": \"Factura emitida por erro\"}"
        }
      }
    }
  ]
}
