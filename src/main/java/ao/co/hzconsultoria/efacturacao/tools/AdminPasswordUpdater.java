package ao.co.hzconsultoria.efacturacao.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

public class AdminPasswordUpdater {
    public static void main(String[] args) {
        String dbFile = (args.length > 0) ? args[0] : "./data/efacturacao";
        String newPassword = (args.length > 1) ? args[1] : "admin123";

        String url = "jdbc:h2:file:" + dbFile + ";MODE=MySQL;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
        String dbUser = "sa";
        String dbPass = "";

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String bcrypt = encoder.encode(newPassword);

        System.out.println("Connecting to H2 DB at: " + url);
        System.out.println("Setting admin password to provided value (bcrypt will be used)." );

        try (Connection conn = DriverManager.getConnection(url, dbUser, dbPass)) {
            conn.setAutoCommit(true);

            // Discover columns present in 'usuario' table
            Set<String> cols = new HashSet<>();
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "usuario", null)) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    cols.add(colName.toLowerCase());
                }
            }

            // Common variants we may set
            List<String> possibleTentativas = Arrays.asList("tentativaslogin", "tentativas_login", "tentativasLogin");
            List<String> possibleBloqueado = Arrays.asList("bloqueadoate", "bloqueado_ate", "bloqueadoAte");
            List<String> possibleAtivo = Arrays.asList("ativo", "is_ativo", "isativo");

            List<String> setClauses = new ArrayList<>();
            List<Object> params = new ArrayList<>();

            // Always set senha
            setClauses.add("senha = ?");
            params.add(bcrypt);

            // tentativas
            for (String p : possibleTentativas) {
                if (cols.contains(p.toLowerCase())) {
                    setClauses.add(p + " = 0");
                    break;
                }
            }
            // bloqueado
            for (String p : possibleBloqueado) {
                if (cols.contains(p.toLowerCase())) {
                    setClauses.add(p + " = NULL");
                    break;
                }
            }
            // ativo
            for (String p : possibleAtivo) {
                if (cols.contains(p.toLowerCase())) {
                    setClauses.add(p + " = TRUE");
                    break;
                }
            }

            // Build UPDATE SQL dynamically
            String updateSql = "UPDATE usuario SET " + String.join(", ", setClauses) + " WHERE LOWER(login) = 'admin'";

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                // set only parameters (senha) - other clauses are constants
                ps.setString(1, bcrypt);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println("Admin password updated successfully (rows updated: " + rows + ").");
                    System.out.println("BCrypt hash: " + bcrypt);
                    return;
                }
            } catch (SQLException e) {
                System.err.println("Update failed: " + e.getMessage());
                // continue to try insert/check
            }

            // If update didn't affect rows, check if admin exists
            boolean adminExists = false;
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM usuario WHERE LOWER(login) = 'admin'")) {
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        adminExists = rs.getInt(1) > 0;
                    }
                }
            }

            if (!adminExists) {
                // Build INSERT using available columns (at least login and senha should be present)
                List<String> insertCols = new ArrayList<>();
                List<String> insertPlaceholders = new ArrayList<>();
                List<Object> insertParams = new ArrayList<>();

                if (cols.contains("login")) {
                    insertCols.add("login"); insertPlaceholders.add("?"); insertParams.add("admin");
                }
                if (cols.contains("senha")) {
                    insertCols.add("senha"); insertPlaceholders.add("?"); insertParams.add(bcrypt);
                }
                if (cols.contains("nome")) {
                    insertCols.add("nome"); insertPlaceholders.add("?"); insertParams.add("Administrador");
                }
                if (cols.contains("role")) {
                    insertCols.add("role"); insertPlaceholders.add("?"); insertParams.add("ADMIN");
                }
                if (cols.contains("ativo")) {
                    insertCols.add("ativo"); insertPlaceholders.add("?"); insertParams.add(Boolean.TRUE);
                }

                if (insertCols.isEmpty()) {
                    System.err.println("Cannot insert admin: no suitable columns found in usuario table.");
                    System.err.println("Columns detected: " + cols);
                    System.err.println("BCrypt hash generated (for manual use): " + bcrypt);
                    return;
                }

                String insSql = "INSERT INTO usuario (" + String.join(",", insertCols) + ") VALUES (" + String.join(",", insertPlaceholders) + ")";
                try (PreparedStatement ins = conn.prepareStatement(insSql)) {
                    for (int i = 0; i < insertParams.size(); i++) {
                        Object val = insertParams.get(i);
                        if (val instanceof String) ins.setString(i+1, (String) val);
                        else if (val instanceof Boolean) ins.setBoolean(i+1, (Boolean) val);
                        else ins.setObject(i+1, val);
                    }
                    int inserted = ins.executeUpdate();
                    System.out.println("Admin user did not exist; inserted new admin (rows inserted: " + inserted + ").");
                    System.out.println("BCrypt hash: " + bcrypt);
                    return;
                } catch (SQLException e) {
                    System.err.println("Failed to insert admin user: " + e.getMessage());
                    System.err.println("Columns detected: " + cols);
                    System.err.println("BCrypt hash generated (for manual use): " + bcrypt);
                    return;
                }
            } else {
                System.err.println("Admin user exists but update affected 0 rows. Schema might differ. Columns detected: " + cols);
                System.err.println("BCrypt hash generated (for manual use): " + bcrypt);
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}