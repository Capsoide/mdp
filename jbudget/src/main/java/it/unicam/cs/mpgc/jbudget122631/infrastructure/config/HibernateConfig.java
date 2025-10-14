package it.unicam.cs.mpgc.jbudget122631.infrastructure.config;

import it.unicam.cs.mpgc.jbudget122631.domain.model.AmortizationPlan;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Budget;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Movement;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Period;
import it.unicam.cs.mpgc.jbudget122631.domain.model.ScheduledExpense;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.io.InputStream;
import java.util.Properties;

public class HibernateConfig {

    private static volatile SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            synchronized (HibernateConfig.class) {
                if (sessionFactory == null) {
                    sessionFactory = buildSessionFactory();
                }
            }
        }
        return sessionFactory;
    }

    private static SessionFactory buildSessionFactory() {
        try {
            Properties props = new Properties();
            try (InputStream in = HibernateConfig.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (in != null) props.load(in);
            }

            String url = props.getProperty("jbudget.database.url", "jdbc:h2:./data/jbudget;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1");
            String user = props.getProperty("jbudget.database.user", "sa");
            String pass = props.getProperty("jbudget.database.password", "");
            String driver = props.getProperty("jbudget.database.driver", "org.h2.Driver");

            Configuration cfg = new Configuration();

            cfg.setProperty("hibernate.connection.driver_class", driver);
            cfg.setProperty("hibernate.connection.url", url);
            cfg.setProperty("hibernate.connection.username", user);
            cfg.setProperty("hibernate.connection.password", pass);

            cfg.setProperty("hibernate.dialect", props.getProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect"));
            cfg.setProperty("hibernate.hbm2ddl.auto", props.getProperty("hibernate.hbm2ddl.auto", "update"));
            cfg.setProperty("hibernate.show_sql", props.getProperty("hibernate.show_sql", "false"));
            cfg.setProperty("hibernate.format_sql", props.getProperty("hibernate.format_sql", "true"));
            cfg.setProperty("hibernate.use_sql_comments", props.getProperty("hibernate.use_sql_comments", "false"));

            cfg.setProperty("hibernate.connection.provider_class",
                    "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
            cfg.setProperty("hibernate.hikari.maximumPoolSize", props.getProperty("hikari.maximum-pool-size", "10"));
            cfg.setProperty("hibernate.hikari.minimumIdle", props.getProperty("hikari.minimum-idle", "2"));
            cfg.setProperty("hibernate.hikari.connectionTimeout", props.getProperty("hikari.connection-timeout", "20000"));

            cfg.addAnnotatedClass(Movement.class);
            cfg.addAnnotatedClass(Category.class);
            cfg.addAnnotatedClass(Budget.class);
            cfg.addAnnotatedClass(ScheduledExpense.class);
            cfg.addAnnotatedClass(Period.class);
            cfg.addAnnotatedClass(AmortizationPlan.class);

            return cfg.buildSessionFactory();
        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'inizializzazione di Hibernate SessionFactory", e);
        }
    }

    public static void shutdown() {
        if (sessionFactory != null) {
            try {
                sessionFactory.close();
            } catch (Exception ignored) { }
            sessionFactory = null;
        }
    }
}
