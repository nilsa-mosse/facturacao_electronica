package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import ao.co.hzconsultoria.efacturacao.security.CustomUserDetails;
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
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConfiguracaoSistemaRepository configuracaoSistemaRepository;

    private Empresa empresa;
    private User user;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void testApiAdicionarCliente() throws Exception {
        String payload = "{\n" +
                "  \"nome\": \"Novo Cliente API\",\n" +
                "  \"nif\": \"123456789\",\n" +
                "  \"endereco\": \"Bairro das Flores, Luanda\",\n" +
                "  \"telefone\": \"923111222\",\n" +
                "  \"email\": \"api@cliente.com\"\n" +
                "}";

        mockMvc.perform(post("/clientes/api/adicionar")
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Verificar na BD
        List<Cliente> clientes = clienteRepository.findByEmpresa_Id(empresa.getId());
        assertFalse(clientes.isEmpty());
        Cliente salvo = clientes.stream()
                .filter(c -> "Novo Cliente API".equals(c.getNome()))
                .findFirst()
                .orElse(null);

        assertNotNull(salvo);
        assertEquals("123456789", salvo.getNif());
        assertEquals("Bairro das Flores, Luanda", salvo.getEndereco());
        assertEquals("923111222", salvo.getTelefone());
        assertEquals("api@cliente.com", salvo.getEmail());
        assertEquals(empresa.getId(), salvo.getEmpresa().getId());
    }

    @Test
    void testApiAdicionarClienteSemNome() throws Exception {
        String payload = "{\n" +
                "  \"nome\": \"\",\n" +
                "  \"nif\": \"123456789\",\n" +
                "  \"telefone\": \"923111222\"\n" +
                "}";

        mockMvc.perform(post("/clientes/api/adicionar")
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApiAdicionarClienteSemNif() throws Exception {
        String payload = "{\n" +
                "  \"nome\": \"Cliente Sem NIF\",\n" +
                "  \"nif\": \"\",\n" +
                "  \"telefone\": \"923111222\"\n" +
                "}";

        mockMvc.perform(post("/clientes/api/adicionar")
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testApiAdicionarClienteSemTelefone() throws Exception {
        String payload = "{\n" +
                "  \"nome\": \"Cliente Sem Telefone\",\n" +
                "  \"nif\": \"123456789\",\n" +
                "  \"telefone\": \"\"\n" +
                "}";

        mockMvc.perform(post("/clientes/api/adicionar")
                .with(user(userDetails))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest());
    }
}
