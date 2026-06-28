package ao.co.hzconsultoria.efacturacao.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Executa migrações de schema manuais e idempotentes no arranque da aplicação.
 * Necessário para colunas adicionadas a entidades existentes quando o Hibernate
 * ddl-auto=update não actualizou o schema em sessões anteriores.
 */
@Component
public class DatabaseMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            adicionarColunaSeFaltando(conn, "caixa", "codigo", "VARCHAR(50)");
            adicionarColunaSeFaltando(conn, "configuracao_empresa", "setup_completo", "BOOLEAN DEFAULT FALSE");
        } catch (Exception e) {
            log.error("[DatabaseMigrationRunner] Erro durante migração do schema: {}", e.getMessage(), e);
        }
    }

    /**
     * Verifica se a coluna existe na tabela e, caso contrário, adiciona-a.
     * Opera de forma idempotente — seguro para executar múltiplas vezes.
     */
    private void adicionarColunaSeFaltando(Connection conn, String tabela, String coluna, String tipoColunaSQL) {
        try {
            DatabaseMetaData meta = conn.getMetaData();

            // H2 com CASE_INSENSITIVE_IDENTIFIERS=TRUE guarda em maiúsculas internamente
            String tabelaCheck = tabela.toUpperCase();
            String colunaCheck = coluna.toUpperCase();

            ResultSet rs = meta.getColumns(null, null, tabelaCheck, colunaCheck);
            boolean existe = rs.next();
            rs.close();

            if (!existe) {
                String sql = "ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + tipoColunaSQL;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    log.info("[DatabaseMigrationRunner] Coluna '{}.{}' adicionada com sucesso.", tabela, coluna);
                }
            } else {
                log.debug("[DatabaseMigrationRunner] Coluna '{}.{}' já existe — sem alterações.", tabela, coluna);
            }
        } catch (Exception e) {
            log.error("[DatabaseMigrationRunner] Falha ao verificar/adicionar coluna '{}.{}': {}", tabela, coluna, e.getMessage());
        }
    }
}
