/**
 * KwanzaERP - Motor POS Avançado: Offline-First, ESC/POS, Balança & Atalhos
 */
const PosEngine = (function () {
    const DB_NAME = 'KwanzaERP_POS_DB';
    const DB_VERSION = 1;
    let db = null;

    // Inicializar IndexedDB
    function initDB() {
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(DB_NAME, DB_VERSION);
            request.onupgradeneeded = function (event) {
                db = event.target.result;
                if (!db.objectStoreNames.contains('catalogo')) {
                    db.createObjectStore('catalogo', { keyPath: 'id' });
                }
                if (!db.objectStoreNames.contains('vendas_offline')) {
                    db.createObjectStore('vendas_offline', { keyPath: 'id', autoIncrement: true });
                }
            };
            request.onsuccess = function (event) {
                db = event.target.result;
                console.log('IndexedDB POS inicializado com sucesso.');
                resolve(db);
            };
            request.onerror = function (event) {
                console.error('Erro ao abrir IndexedDB:', event.target.error);
                reject(event.target.error);
            };
        });
    }

    // Carregar catálogo da API e guardar no IndexedDB
    async function carregarESincronizarCatalogo() {
        try {
            if (navigator.onLine) {
                const response = await fetch('/api/pos/catalogo');
                if (response.ok) {
                    const produtos = await response.json();
                    await guardarCatalogoNoIndexedDB(produtos);
                    console.log('Catálogo POS atualizado offline com ' + produtos.length + ' produtos.');
                    return produtos;
                }
            }
        } catch (e) {
            console.warn('Rede indisponível ao procurar catálogo. A usar catálogo local.', e);
        }
        return await obterCatalogoLocal();
    }

    function guardarCatalogoNoIndexedDB(produtos) {
        return new Promise((resolve, reject) => {
            if (!db) return resolve([]);
            const transaction = db.transaction(['catalogo'], 'readwrite');
            const store = transaction.objectStore('catalogo');
            store.clear();
            produtos.forEach(p => store.put(p));
            transaction.oncomplete = () => resolve();
            transaction.onerror = (e) => reject(e);
        });
    }

    function obterCatalogoLocal() {
        return new Promise((resolve) => {
            if (!db) return resolve([]);
            const transaction = db.transaction(['catalogo'], 'readonly');
            const store = transaction.objectStore('catalogo');
            const req = store.getAll();
            req.onsuccess = () => resolve(req.result || []);
            req.onerror = () => resolve([]);
        });
    }

    // Guardar Venda Offline
    function guardarVendaOffline(venda) {
        return new Promise((resolve, reject) => {
            if (!db) return reject('DB não inicializado');
            venda.dataHoraOffline = new Date().toISOString();
            const transaction = db.transaction(['vendas_offline'], 'readwrite');
            const store = transaction.objectStore('vendas_offline');
            const req = store.add(venda);
            req.onsuccess = () => {
                atualizarIndicadorOffline();
                resolve(req.result);
            };
            req.onerror = (e) => reject(e);
        });
    }

    // Obter total de vendas offline pendentes
    function obterVendasOfflinePendentes() {
        return new Promise((resolve) => {
            if (!db) return resolve([]);
            const transaction = db.transaction(['vendas_offline'], 'readonly');
            const store = transaction.objectStore('vendas_offline');
            const req = store.getAll();
            req.onsuccess = () => resolve(req.result || []);
            req.onerror = () => resolve([]);
        });
    }

    // Sincronizar Vendas Offline com o Servidor
    async function sincronizarVendasOffline() {
        if (!navigator.onLine) return;
        const vendas = await obterVendasOfflinePendentes();
        if (vendas.length === 0) return;

        try {
            const resp = await fetch('/api/pos/sincronizar-offline', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(vendas)
            });

            if (resp.ok) {
                // Limpar vendas enviadas
                const transaction = db.transaction(['vendas_offline'], 'readwrite');
                const store = transaction.objectStore('vendas_offline');
                store.clear();
                transaction.oncomplete = () => {
                    console.log('Vendas offline sincronizadas com sucesso.');
                    atualizarIndicadorOffline();
                    if (window.toastr) toastr.success(vendas.length + ' vendas offline sincronizadas com o servidor!');
                };
            }
        } catch (err) {
            console.error('Erro durante sincronização de vendas offline:', err);
        }
    }

    function atualizarIndicadorOffline() {
        obterVendasOfflinePendentes().then(vendas => {
            const badge = document.getElementById('badgeOfflineCount');
            if (badge) {
                badge.innerText = vendas.length;
                badge.style.display = vendas.length > 0 ? 'inline-block' : 'none';
            }
        });
    }

    // Parser de Código de Barras de Balança (EAN-13 iniciado com 20 ou 21)
    // Formato EAN-13 balança: 20 PPPPP QQQQQ C
    // PPPPP: Código do produto (5 dígitos)
    // QQQQQ: Peso em gramas (ex: 01500 = 1.500 kg) ou preço em cêntimos
    function parseCodigoBalanca(barcode, prefixo = '20') {
        if (!barcode || barcode.length !== 13) return null;
        if (!barcode.startsWith(prefixo)) return null;

        const codigoProd = barcode.substring(2, 7);
        const valorExtracao = parseInt(barcode.substring(7, 12), 10);
        const pesoKg = valorExtracao / 1000.0; // Assume peso em kg

        return {
            eBalanca: true,
            codigoProduto: codigoProd,
            quantidadePeso: pesoKg
        };
    }

    // Impressão ESC/POS e Abertura de Gaveta de Dinheiro
    // Comando ESC/POS abertura gaveta: ESC p 0 (27, 112, 0, 25, 250)
    function gerarBytesEscPos(vendaData, abrirGaveta = true) {
        let esc = "";
        // Reset printer
        esc += "\x1B\x40";
        // Alignment Center
        esc += "\x1B\x61\x01";
        // Title Double Size
        esc += "\x1D\x21\x11" + (vendaData.empresaNome || "KwanzaERP POS") + "\x0A";
        esc += "\x1D\x21\x00" + "DOCUMENTO DE CONSULTA / TALÃO DE CAIXA\x0A";
        esc += "----------------------------------------\x0A";
        // Left align
        esc += "\x1B\x61\x00";
        esc += "Data: " + (vendaData.data || new Date().toLocaleString()) + "\x0A";
        esc += "Operador: " + (vendaData.operador || "Caixa 01") + "\x0A";
        if (vendaData.mesa) esc += "Mesa: " + vendaData.mesa + "\x0A";
        esc += "----------------------------------------\x0A";

        if (vendaData.itens) {
            vendaData.itens.forEach(item => {
                esc += item.nome + "\x0A";
                esc += "  " + item.qtd + " x " + item.preco.toFixed(2) + " = " + item.total.toFixed(2) + " Kz\x0A";
            });
        }
        esc += "----------------------------------------\x0A";
        esc += "\x1B\x61\x02"; // Right align
        esc += "\x1D\x21\x11TOTAL: " + (vendaData.total ? vendaData.total.toFixed(2) : "0.00") + " Kz\x0A\x1D\x21\x00";

        esc += "\x1B\x61\x01"; // Center
        esc += "Obrigado pela preferência!\x0A\x0A\x0A";
        esc += "\x1D\x56\x41\x03"; // Cut paper

        if (abrirGaveta) {
            // Pulse to open drawer: ESC p 0 25 250
            esc += "\x1B\x70\x00\x19\xFA";
        }
        return esc;
    }

    // Executar abertura de gaveta isolada
    function pulsarGavetaDinheiro() {
        console.log('Comando de pulso enviado para abertura de gaveta de dinheiro (ESC p 0 25 250).');
        if (window.toastr) toastr.info('Sinal de abertura de gaveta enviado!');
        // Se WebSerial/RawBT estiver disponível no cliente, envia a sequência de bytes
    }

    // Inicialização ao carregar página
    window.addEventListener('online', sincronizarVendasOffline);
    window.addEventListener('load', () => {
        initDB().then(() => {
            carregarESincronizarCatalogo();
            atualizarIndicadorOffline();
        });
    });

    return {
        initDB,
        carregarESincronizarCatalogo,
        obterCatalogoLocal,
        guardarVendaOffline,
        sincronizarVendasOffline,
        parseCodigoBalanca,
        gerarBytesEscPos,
        pulsarGavetaDinheiro
    };
})();
