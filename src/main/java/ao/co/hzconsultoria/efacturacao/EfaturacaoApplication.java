package ao.co.hzconsultoria.efacturacao;

import ao.co.hzconsultoria.efacturacao.model.User;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.UserRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EstabelecimentoRepository;
import ao.co.hzconsultoria.efacturacao.repository.ConfiguracaoSistemaRepository;
import ao.co.hzconsultoria.efacturacao.repository.ImpostoRepository;
import ao.co.hzconsultoria.efacturacao.model.Imposto;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Optional;
import java.math.BigDecimal;

@SpringBootApplication
@EnableScheduling
public class EfaturacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EfaturacaoApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
            EmpresaRepository empresaRepository,
            EstabelecimentoRepository estabRepository,
            ConfiguracaoSistemaRepository configRepo,
            ImpostoRepository impostoRepository,
            ao.co.hzconsultoria.efacturacao.repository.ClienteRepository clienteRepository,
            ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository categoriaRepository,
            ao.co.hzconsultoria.efacturacao.repository.ProdutoRepository produtoRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            // Migração de Emergência: Adicionar coluna 'exibir_datas_validade' se não
            // existir
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE configuracao_sistema ADD COLUMN IF NOT EXISTS exibir_datas_validade BOOLEAN DEFAULT TRUE");
                System.out.println(">>> Migração: Coluna 'exibir_datas_validade' verificada/adicionada com sucesso.");
            } catch (Exception e) {
                System.err.println(">>> Erro ao tentar migrar tabela configuracao_sistema: " + e.getMessage());
            }

            // Migração: Adicionar colunas de controlo de envio AGT
            try {
                jdbcTemplate.execute(
                        "ALTER TABLE configuracao_agt ADD COLUMN IF NOT EXISTS envio_agt_ativo BOOLEAN DEFAULT TRUE");
                jdbcTemplate.execute(
                        "ALTER TABLE configuracao_agt ADD COLUMN IF NOT EXISTS limite_documentos_diarios INT DEFAULT 0");
                jdbcTemplate.execute(
                        "ALTER TABLE configuracao_agt ADD COLUMN IF NOT EXISTS documentos_enviados_hoje INT DEFAULT 0");
                jdbcTemplate.execute("ALTER TABLE configuracao_agt ADD COLUMN IF NOT EXISTS data_ultimo_envio DATE");
                System.out.println(">>> Migração: Colunas de controlo AGT verificadas/adicionadas com sucesso.");
            } catch (Exception e) {
                System.err.println(">>> Erro ao migrar tabela configuracao_agt: " + e.getMessage());
            }

            // Migração: Criar tabela para gestão de licenças
            try {
                jdbcTemplate.execute(
                        "CREATE TABLE IF NOT EXISTS licencas_geradas (" +
                                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                                "machine_id VARCHAR(255), " +
                                "cliente_nome VARCHAR(255), " +
                                "chave_gerada VARCHAR(500), " +
                                "data_emissao TIMESTAMP, " +
                                "data_expiracao TIMESTAMP, " +
                                "ativa BOOLEAN DEFAULT TRUE, " +
                                "observacoes VARCHAR(1000)" +
                                ")");
                System.out.println(">>> Migração: Tabela 'licencas_geradas' verificada/criada com sucesso.");
            } catch (Exception e) {
                System.err.println(">>> Erro ao migrar tabela licencas_geradas: " + e.getMessage());
            }

            // Migração: Garantir integridade referencial mínima na venda_suspensa
            // (corrige valores orphan antes de adicionar a constraint que pode falhar)
            try {
                // Nulificar operador_id que não existe na tabela usuario
                jdbcTemplate.execute("UPDATE venda_suspensa SET operador_id = NULL WHERE operador_id IS NOT NULL AND operador_id NOT IN (SELECT id FROM usuario)");
                // Tentar adicionar a constraint caso não exista (pode lançar se já criada)
                try {
                    jdbcTemplate.execute("ALTER TABLE venda_suspensa ADD CONSTRAINT FKk9vklxihqrcl137jwnwmledu6 FOREIGN KEY (operador_id) REFERENCES usuario (id)");
                    System.out.println(">>> Migração: Constraint FKk9vklxihqrcl137jwnwmledu6 adicionada com sucesso.");
                } catch (Exception e) {
                    // Constraint pode já existir ou o banco pode não permitir ADD sem checagem; ignorar com log
                    System.out.println(">>> Migração: não foi possível adicionar constraint FKk9vklxihqrcl137jwnwmledu6 (provavelmente já existe): " + e.getMessage());
                }
            } catch (Exception e) {
                System.err.println(">>> Migração: falha ao preparar venda_suspensa para constraint: " + e.getMessage());
            }

            // 1. Garantir que, após instalação, apenas exista o SuperAdmin ativo.
            // Outros utilizadores serão desactivados/limpos para deixar o sistema limpo
            // para receber novos dados. Isto evita problemas com FK ao apagar.
            userRepository.findAll().forEach(user -> {
                if ("superadmin".equalsIgnoreCase(user.getLogin())) {
                    // Garantir que o SuperAdmin está activo e com senha resetada
                    user.setAtivo(true);
                    user.setTentativasLogin(0);
                    user.setBloqueadoAte(null);
                    user.setSenha(passwordEncoder.encode("superadmin@2026"));
                    userRepository.save(user);
                } else {
                    // Desactivar e limpar dados sensíveis/relacionamentos para um sistema limpo
                    try {
                        user.setAtivo(false);
                        user.setTentativasLogin(0);
                        user.setBloqueadoAte(null);
                        user.setPermissoes(new HashSet<>());
                        user.setEstabelecimentos(new HashSet<>());
                        user.setEmpresa(null);
                        // opcional: remover password para forçar reset ao criar um novo
                        user.setSenha(null);
                        userRepository.save(user);
                    } catch (Exception ex) {
                        System.err.println(">>> Aviso: não foi possível limpar/activar utilizador " + user.getLogin() + ": " + ex.getMessage());
                    }
                }
            });

            // 2. Desbloquear admin se necessário (emergência)
            Optional<User> adminOpt = userRepository.findByLogin("admin");
            if (adminOpt.isPresent()) {
                User existingAdmin = adminOpt.get();
                boolean changed = false;

                if (existingAdmin.getTentativasLogin() > 0 || existingAdmin.getBloqueadoAte() != null) {
                    existingAdmin.setTentativasLogin(0);
                    existingAdmin.setBloqueadoAte(null);
                    existingAdmin.setAtivo(true);
                    changed = true;
                }
                // If the stored password doesn't match the default and isn't a valid BCrypt
                // hash,
                // reset it to the known default. Use regex to verify real BCrypt format (60
                // chars).
                String stored = existingAdmin.getSenha();
                boolean matchesDefault = (stored != null && passwordEncoder.matches("admin123", stored));
                boolean isValidBcrypt = false;
                if (stored != null) {
                    // BCrypt hash format: $2a$10$<22-char-salt><31-char-hash> total length 60
                    isValidBcrypt = stored.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
                }

                // FORÇAR RESET (Para garantir que admin/admin123 funcione agora)
                existingAdmin.setSenha(passwordEncoder.encode("admin123"));
                existingAdmin.setTentativasLogin(0);
                existingAdmin.setBloqueadoAte(null);
                existingAdmin.setAtivo(true);
                changed = true;

                if (changed) {
                    userRepository.save(existingAdmin);
                }
            }

            // 3. Criar ambiente inicial se não houver utilizadores
            if (userRepository.count() == 0) {
                System.out.println("Iniciando instalação limpa: nenhuma empresa cadastrada por padrão.");
            }

            // 3.1 Criar ou Resetar SuperAdmin de Sistema
            Optional<User> superOpt = userRepository.findByLogin("superadmin");
            if (!superOpt.isPresent()) {
                System.out.println("Criando SuperAdmin de emergência...");
                User superAdmin = new User();
                superAdmin.setLogin("superadmin");
                superAdmin.setSenha(passwordEncoder.encode("superadmin@2026"));
                superAdmin.setNome("Super Administrador");
                superAdmin.setRole("SUPERADMIN");
                superAdmin.setAtivo(true);
                superAdmin.setPermissoes(new HashSet<>(Arrays.asList(
                        "DASHBOARD", "VENDAS", "STOCK", "FACTURACAO", "FINANCEIRO", "ADMINISTRACAO", "PAINEL_GLOBAL",
                        "RH")));
                userRepository.save(superAdmin);
            } else {
                // Forçar reset de senha
                User superAdmin = superOpt.get();
                superAdmin.setSenha(passwordEncoder.encode("superadmin@2026"));
                superAdmin.setAtivo(true);
                superAdmin.setTentativasLogin(0);
                superAdmin.setBloqueadoAte(null);
                userRepository.save(superAdmin);
            }

            // 4. Inicializar Impostos Padrão (se necessário)
            if (impostoRepository.count() == 0) {
                System.out.println("Iniciando criação de impostos padrão...");

                Imposto iva14 = new Imposto();
                iva14.setNome("IVA - Taxa Normal");
                iva14.setPercentagem(new BigDecimal("14.0"));
                iva14.setTipo("IVA");
                iva14.setCodigoAgt("NOR");
                impostoRepository.save(iva14);
                Imposto iva7 = new Imposto();
                iva7.setNome("IVA - Taxa Simplificada");
                iva7.setPercentagem(new BigDecimal("7.0"));
                iva7.setTipo("IVA");
                iva7.setCodigoAgt("SIM");
                impostoRepository.save(iva7);

                Imposto isento = new Imposto();
                isento.setNome("Isento");
                isento.setPercentagem(BigDecimal.ZERO);
                isento.setTipo("IVA");
                isento.setCodigoAgt("ISE");
                isento.setMotivoIsencao("Isenção nos termos da lei");
                impostoRepository.save(isento);
                System.out.println(">>> Impostos padrão criados com sucesso!");
            }

            // 5. Inicializar Dados Padrão (Clientes) para novas instalações
            if (clienteRepository.count() == 0) {
                System.out.println("Populando sistema com dados padrão...");

                Empresa empPadrao = empresaRepository.findAll().stream().findFirst().orElse(null);
                if (empPadrao != null) {
                    // Cliente*
                    ao.co.hzconsultoria.efacturacao.model.Cliente clienteFinal = new ao.co.hzconsultoria.efacturacao.model.Cliente();
                    clienteFinal.setNome("Consumidor Final");
                    clienteFinal.setNif("999999999");
                    clienteFinal.setEmpresa(empPadrao);
                    clienteRepository.save(clienteFinal);
                }
            }
        };
    }
}
