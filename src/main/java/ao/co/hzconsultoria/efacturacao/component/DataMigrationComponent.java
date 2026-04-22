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
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> Iniciando Migração e Seed de Dados Multi-Empresa...");

        // 1. Garantir Empresa Padrão
        Empresa empresaPadrao = empresaRepository.findByNif("5000000000");
        if (empresaPadrao == null) {
            empresaPadrao = new Empresa();
            empresaPadrao.setNome("HZ Consultoria Lda");
            empresaPadrao.setNif("5000000000");
            empresaPadrao.setRegimeFiscal("GERAL");
            empresaPadrao.setEndereco("Luanda, Angola");
            empresaPadrao = empresaRepository.save(empresaPadrao);
            System.out.println(">>> Empresa padrão (HZ Consultoria) criada.");
        }

        final Empresa target = empresaPadrao;

        // 2. Garantir Estabelecimento Padrão
        Estabelecimento sede = null;
        List<Estabelecimento> estabelecimentos = estabelecimentoRepository.findByEmpresa_Id(target.getId());
        if (estabelecimentos.isEmpty()) {
            sede = new Estabelecimento();
            sede.setNome("Sede Luanda");
            sede.setTipo("LOJA");
            sede.setEmpresa(target);
            sede = estabelecimentoRepository.save(sede);
            System.out.println(">>> Estabelecimento sede criado.");
        } else {
            sede = estabelecimentos.get(0);
        }

        // 3. Garantir Utilizadores Padrão
        if (!userRepository.findByLogin("admin").isPresent()) {
            User admin = new User();
            admin.setLogin("admin");
            admin.setNome("Administrador");
            admin.setSenha(passwordEncoder.encode("123"));
            admin.setRole("ROLE_ADMIN");
            admin.setEmpresa(target);
            admin.getEstabelecimentos().add(sede);
            userRepository.save(admin);
            System.out.println(">>> Utilizador 'admin' criado (senha: 123).");
        }

        if (!userRepository.findByLogin("superadmin").isPresent()) {
            User superAdmin = new User();
            superAdmin.setLogin("superadmin");
            superAdmin.setNome("Super Administrador");
            superAdmin.setSenha(passwordEncoder.encode("super123"));
            superAdmin.setRole("ROLE_SUPERADMIN");
            superAdmin.setEmpresa(target); // SuperAdmin também precisa de uma empresa base
            userRepository.save(superAdmin);
            System.out.println(">>> Utilizador 'superadmin' criado (senha: super123).");
        }

        // 4. Migração de dados órfãos (se existirem de versões anteriores)
        migrarDadosOrfaos(target);

        System.out.println(">>> Migração e Seed concluídos com sucesso.");
    }

    private void migrarDadosOrfaos(Empresa target) {
        userRepository.findAll().stream().filter(u -> u.getEmpresa() == null).forEach(u -> { u.setEmpresa(target); userRepository.save(u); });
        produtoRepository.findAll().stream().filter(p -> p.getEmpresa() == null).forEach(p -> { p.setEmpresa(target); produtoRepository.save(p); });
        clienteRepository.findAll().stream().filter(c -> c.getEmpresa() == null).forEach(c -> { c.setEmpresa(target); clienteRepository.save(c); });
        categoriaRepository.findAll().stream().filter(cat -> cat.getEmpresa() == null).forEach(cat -> { cat.setEmpresa(target); categoriaRepository.save(cat); });
        faturaRepository.findAll().stream().filter(f -> f.getEmpresa() == null).forEach(f -> { f.setEmpresa(target); faturaRepository.save(f); });
        compraRepository.findAll().stream().filter(comp -> comp.getEmpresa() == null).forEach(comp -> { comp.setEmpresa(target); compraRepository.save(comp); });
        serieRepository.findAll().stream().filter(s -> s.getEmpresa() == null).forEach(s -> { s.setEmpresa(target); serieRepository.save(s); });
        despesaRepository.findAll().stream().filter(d -> d.getEmpresa() == null).forEach(d -> { d.setEmpresa(target); despesaRepository.save(d); });
    }
}
