package ao.co.hzconsultoria.efacturacao.component;

import ao.co.hzconsultoria.efacturacao.model.*;
import ao.co.hzconsultoria.efacturacao.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataMigrationComponent implements CommandLineRunner {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FaturaRepository faturaRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private SerieRepository serieRepository;

    @Autowired
    private DespesaRepository despesaRepository;

    @Autowired
    private EstabelecimentoRepository estabelecimentoRepository;
    
    @Autowired
    private PermissaoModuloRepository permissaoRepo;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> Iniciando Migração e Seed de Dados Multi-Empresa...");

        // 1. Garantir apenas que o superadmin existe e não tem empresa associada
        if (!userRepository.findByLogin("superadmin").isPresent()) {
            User superAdmin = new User();
            superAdmin.setLogin("superadmin");
            superAdmin.setNome("Super Administrador");
            superAdmin.setSenha(passwordEncoder.encode("superadmin@2026"));
            superAdmin.setRole("ROLE_SUPERADMIN");
            superAdmin.setEmpresa(null); // SuperAdmin nunca tem empresa associada
            userRepository.save(superAdmin);
            System.out.println(">>> Utilizador 'superadmin' criado com sucesso.");
        } else {
            // Garantir que o SuperAdmin existente não tem empresa associada
            userRepository.findByLogin("superadmin").ifPresent(sa -> {
                if (sa.getEmpresa() != null) {
                    sa.setEmpresa(null);
                    userRepository.save(sa);
                    System.out.println(">>> SuperAdmin desassociado da empresa (correcção automática).");
                }
            });
        }

        // 2. Garantir Permissões de Módulos Padrão para utilizadores existentes
        inicializarPermissoesModulos();

        System.out.println(">>> Migração e Seed concluídos com sucesso.");
    }

    private void inicializarPermissoesModulos() {
        String[] modulos = {"DASHBOARD", "VENDAS", "STOCK", "ENTIDADES", "FACTURACAO", "FINANCEIRO", "ADMINISTRACAO"};
        List<User> usuarios = userRepository.findAll();
        
        for (User user : usuarios) {
            if ("ROLE_SUPERADMIN".equals(user.getRole())) continue; // SuperAdmin não precisa de registos de permissão
            
            for (String modulo : modulos) {
                if (!permissaoRepo.findByModuloAndUsuario_Id(modulo, user.getId()).isPresent()) {
                    boolean isAdmin = "ROLE_ADMIN".equals(user.getRole());
                    // ADMIN tem acesso a tudo. Outros apenas a VENDAS por padrão.
                    boolean ativo = isAdmin || (modulo.equals("VENDAS"));
                    
                    permissaoRepo.save(new PermissaoModulo(modulo, user, ativo));
                }
            }
        }
        System.out.println(">>> Permissões de módulos inicializadas por utilizador.");
    }
}
