package it.unicam.cs.mpgc.jbudget122631.infrastructure.config;

import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    private static final String DB_URL = "jdbc:h2:./data/jbudget;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static DataSource dataSource;

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConfig.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                    initializeDatabase();
                }
            }
        }
        return dataSource;
    }

    private static DataSource createDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(DB_URL);
        ds.setUser(DB_USER);
        ds.setPassword(DB_PASSWORD);
        return ds;
    }

    private static void initializeDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Abilita supporto per H2 database
            stmt.execute("CREATE SCHEMA IF NOT EXISTS jbudget");

            System.out.println("Database H2 inizializzato correttamente");
            System.out.println("Database URL: " + DB_URL);

        } catch (SQLException e) {
            throw new RuntimeException("Errore inizializzazione database", e);
        }
    }

    public static void closeDataSource() {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                System.err.println("Errore chiusura datasource: " + e.getMessage());
            }
        }
    }
}