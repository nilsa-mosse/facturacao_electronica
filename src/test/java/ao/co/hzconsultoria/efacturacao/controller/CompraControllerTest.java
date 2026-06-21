package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
import ao.co.hzconsultoria.efacturacao.service.CaixaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaixaService caixaService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    private Empresa empresa;
    private User user;
    private Produto produto;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Garantir que a licença está configurada e válida
        if (!configuracaoSistemaRepository.existsById(1L)) {
            ConfiguracaoSistemaEntity config = new ConfiguracaoSistemaEntity();
            config.setId(1L);
            config.setDataInstalacao(java.time.LocalDateTime.now());
            configuracaoSistemaRepository.save(config);
        } else {
            ConfiguracaoSistemaEntity config = configuracaoSistemaRepository.findById(1L).get();
            config.setDataInstalacao(java.time.LocalDateTime.now());
            configuracaoSistemaRepository.save(config);
        }

        // Criar Empresa
        empresa = new Empresa();
        empresa.setNome("Empresa Teste Lda");
        empresa.setNif("500099999");
        empresa.setEndereco("Luanda, Angola");
        empresa = empresaRepository.save(empresa);

        // Criar Categoria
        Categoria categoria = new Categoria();
        categoria.setNome("Geral");
        categoria.setEmpresa(empresa);
        categoria = categoriaRepository.save(categoria);

        // Criar Produto
        produto = new Produto();
        produto.setNome("Produto Teste");
        produto.setPreco(1000.0);
        produto.setIvaPercentual(14.0);
        produto.setQuantidadeEstoque(100.0);
        produto.setEmpresa(empresa);
        produto.setCategoria(categoria);
        produto = produtoRepository.save(produto);

        // Criar Utilizador
        user = new User();
        user.setNome("Operador");
        user.setLogin("operador");
        user.setSenha("senha123");
        user.setRole("OPERADOR");
        user.setEmpresa(empresa);
        user = userRepository.save(user);

        // Configurar Autenticação
        userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Abrir Caixa se não estiver aberto
        if (!caixaService.isCaixaAberto()) {
            caixaService.abrirCaixa(10000.0, "Abertura Teste");
        }
    }

    @Test
    void testCheckoutWithExistingCustomer() throws Exception {
        // Criar Cliente Existente
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Registado");
        cliente.setNif("123456789");
        cliente.setEndereco("Bairro Azul, Luanda");
        cliente.setTelefone("923000001");
        cliente.setEmail("cliente@registado.com");
        cliente.setEmpresa(empresa);
        cliente = clienteRepository.save(cliente);

        // Montar Payload
        String payload = "{\n" +
                "  \"itens\": [\n" +
                "    {\n" +
                "      \"produtoId\": " + produto.getId() + ",\n" +
                "      \"nomeProduto\": \"" + produto.getNome() + "\",\n" +
                "      \"quantidade\": 2,\n" +
                "      \"preco\": 1000.0,\n" +
                "      \"subtotal\": 2000.0\n" +
                "    }\n" +
                "  ],\n" +
                "  \"cliente\": {\n" +
                "    \"id\": " + cliente.getId() + "\n" +
                "  },\n" +
                "  \"nomeCliente\": \"Cliente Registado\",\n" +
                "  \"nifCliente\": \"123456789\",\n" +
                "  \"moradaCliente\": \"\",\n" +
                "  \"telefoneCliente\": \"\",\n" +
                "  \"emailCliente\": \"\",\n" +
                "  \"formaPagamento\": \"CASH\",\n" +
                "  \"tipoDocumento\": \"FR\"\n" +
                "}";

        mockMvc.perform(post("/api/compras/single")
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Verificar no Banco de Dados
        List<Compra> compras = compraRepository.findAll();
        assertFalse(compras.isEmpty());
        Compra compraSalva = compras.get(compras.size() - 1);

        assertEquals("Cliente Registado", compraSalva.getNomeCliente());
        assertEquals("123456789", compraSalva.getNifCliente());
        assertEquals("Bairro Azul, Luanda", compraSalva.getMoradaCliente());
        assertEquals("923000001", compraSalva.getTelefoneCliente());
        assertEquals("cliente@registado.com", compraSalva.getEmailCliente());

        // Verify PDF content
        String pdfPath = "./uploads/faturas/FR 2026/1.pdf";
        java.io.File pdfFile = new java.io.File(pdfPath);
        assertTrue(pdfFile.exists());
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdfPath);
        com.lowagie.text.pdf.parser.PdfTextExtractor extractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        String pdfText = extractor.getTextFromPage(1);
        System.out.println("--- PDF TEXT START ---");
        System.out.println(pdfText);
        System.out.println("--- PDF TEXT END ---");
        assertTrue(pdfText.contains("Cliente Registado"));
        assertTrue(pdfText.contains("123456789"));
        assertTrue(pdfText.contains("Bairro Azul"));
    }

    @Test
    void testCheckoutWithNewCustomer() throws Exception {
        // Montar Payload de Novo Cliente
        String payload = "{\n" +
                "  \"itens\": [\n" +
                "    {\n" +
                "      \"produtoId\": " + produto.getId() + ",\n" +
                "      \"nomeProduto\": \"" + produto.getNome() + "\",\n" +
                "      \"quantidade\": 1,\n" +
                "      \"preco\": 1000.0,\n" +
                "      \"subtotal\": 1000.0\n" +
                "    }\n" +
                "  ],\n" +
                "  \"cliente\": null,\n" +
                "  \"nomeCliente\": \"Novo Cliente Teste\",\n" +
                "  \"nifCliente\": \"987654321\",\n" +
                "  \"moradaCliente\": \"Bairro Vermelho, Luanda\",\n" +
                "  \"telefoneCliente\": \"923000002\",\n" +
                "  \"emailCliente\": \"novo@cliente.com\",\n" +
                "  \"formaPagamento\": \"CASH\",\n" +
                "  \"tipoDocumento\": \"FR\"\n" +
                "}";

        mockMvc.perform(post("/api/compras/single")
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Verificar no Banco de Dados
        List<Compra> compras = compraRepository.findAll();
        assertFalse(compras.isEmpty());
        Compra compraSalva = compras.get(compras.size() - 1);

        assertEquals("Novo Cliente Teste", compraSalva.getNomeCliente());
        assertEquals("987654321", compraSalva.getNifCliente());
        assertEquals("Bairro Vermelho, Luanda", compraSalva.getMoradaCliente());
        assertEquals("923000002", compraSalva.getTelefoneCliente());
        assertEquals("novo@cliente.com", compraSalva.getEmailCliente());

        // Verificar se novo cliente foi criado na tabela Cliente
        assertNotNull(compraSalva.getCliente());
        Cliente novoClienteSalvo = clienteRepository.findById(compraSalva.getCliente().getId()).orElse(null);
        assertNotNull(novoClienteSalvo);
        assertEquals("Novo Cliente Teste", novoClienteSalvo.getNome());
        assertEquals("987654321", novoClienteSalvo.getNif());
        assertEquals("Bairro Vermelho, Luanda", novoClienteSalvo.getEndereco());
    }
}
