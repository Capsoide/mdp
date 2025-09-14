package it.unicam.cs.mpgc.jbudget122631.infrastructure.persistence;

import it.unicam.cs.mpgc.jbudget122631.domain.model.Budget;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Category;
import it.unicam.cs.mpgc.jbudget122631.domain.model.Period;
import it.unicam.cs.mpgc.jbudget122631.domain.repository.BudgetRepository;
import it.unicam.cs.mpgc.jbudget122631.infrastructure.config.HibernateConfig;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class JpaBudgetRepository implements BudgetRepository {

    private final SessionFactory sessionFactory;

    public JpaBudgetRepository() {
        this.sessionFactory = HibernateConfig.getSessionFactory();
    }

    @Override
    public Budget save(Budget budget) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();

            // DEBUG: Prima del salvataggio
            System.out.println("REPOSITORY - BEFORE SAVE:");
            System.out.println("  - Budget ID: " + budget.getId());
            System.out.println("  - Budget actualIncome: €" + budget.getActualIncome());
            System.out.println("  - Budget actualExpenses: €" + budget.getActualExpenses());

            session.saveOrUpdate(budget);

            // DEBUG: Dopo saveOrUpdate ma prima del commit
            System.out.println("REPOSITORY - AFTER saveOrUpdate, BEFORE commit:");
            System.out.println("  - Budget ID: " + budget.getId());
            System.out.println("  - Budget actualIncome: €" + budget.getActualIncome());
            System.out.println("  - Budget actualExpenses: €" + budget.getActualExpenses());

            transaction.commit();

            // DEBUG: Dopo il commit
            System.out.println("REPOSITORY - AFTER COMMIT:");
            System.out.println("  - Budget ID: " + budget.getId());
            System.out.println("  - Budget actualIncome: €" + budget.getActualIncome());
            System.out.println("  - Budget actualExpenses: €" + budget.getActualExpenses());

            return budget;
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            System.err.println("REPOSITORY - Errore nel salvataggio: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Errore salvataggio budget", e);
        }
    }

    @Override
    public Optional<Budget> findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            // IMPORTANTE: Usa JOIN FETCH per caricare Period e Category nella stessa query
            Query<Budget> query = session.createQuery(
                    "SELECT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "LEFT JOIN FETCH b.category " +
                            "WHERE b.id = :id",
                    Budget.class);
            query.setParameter("id", id);

            List<Budget> results = query.getResultList();
            if (!results.isEmpty()) {
                Budget budget = results.get(0);
                System.out.println("REPOSITORY - Budget caricato con Period: " + budget.getPeriod().getName() +
                        " e Category: " + (budget.getCategory() != null ? budget.getCategory().getName() : "null"));

                // DEBUG: Valori caricati dal database
                System.out.println("REPOSITORY - findById - Valori caricati:");
                System.out.println("  - actualIncome: €" + budget.getActualIncome());
                System.out.println("  - actualExpenses: €" + budget.getActualExpenses());

                return Optional.of(budget);
            }
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("REPOSITORY - Errore ricerca budget per ID: " + e.getMessage());
            throw new RuntimeException("Errore ricerca budget per ID", e);
        }
    }

    @Override
    public List<Budget> findAll() {
        try (Session session = sessionFactory.openSession()) {
            // IMPORTANTE: Usa JOIN FETCH per caricare Period e Category insieme al Budget
            Query<Budget> query = session.createQuery(
                    "SELECT DISTINCT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "LEFT JOIN FETCH b.category " +
                            "ORDER BY b.period.startDate DESC",
                    Budget.class);
            List<Budget> budgets = query.getResultList();
            System.out.println("REPOSITORY - Caricati " + budgets.size() + " budget con relazioni complete");

            // DEBUG: Mostra i valori di tutti i budget caricati
            System.out.println("REPOSITORY - findAll - Budget caricati:");
            for (Budget budget : budgets) {
                String categoryName = budget.getCategory() != null ? budget.getCategory().getName() : "Generale";
                System.out.println("  - " + categoryName + " (" + budget.getPeriod().getName() + "): " +
                        "Income=€" + budget.getActualIncome() + ", Expenses=€" + budget.getActualExpenses());
            }

            return budgets;
        } catch (Exception e) {
            throw new RuntimeException("Errore recupero tutti i budget", e);
        }
    }

    @Override
    public void delete(Budget budget) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(budget);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione budget", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            Budget budget = session.get(Budget.class, id);
            if (budget != null) {
                session.delete(budget);
            }
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            throw new RuntimeException("Errore eliminazione budget per ID", e);
        }
    }

    @Override
    public Optional<Budget> findByPeriodAndCategory(Period period, Category category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Budget> query;
            if (category == null) {
                query = session.createQuery(
                        "SELECT b FROM Budget b " +
                                "LEFT JOIN FETCH b.period " +
                                "WHERE b.period = :period AND b.category IS NULL",
                        Budget.class);
                query.setParameter("period", period);
            } else {
                query = session.createQuery(
                        "SELECT b FROM Budget b " +
                                "LEFT JOIN FETCH b.period " +
                                "LEFT JOIN FETCH b.category " +
                                "WHERE b.period = :period AND b.category = :category",
                        Budget.class);
                query.setParameter("period", period);
                query.setParameter("category", category);
            }
            List<Budget> results = query.getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca budget per periodo e categoria", e);
        }
    }

    @Override
    public List<Budget> findByPeriod(Period period) {
        try (Session session = sessionFactory.openSession()) {
            Query<Budget> query = session.createQuery(
                    "SELECT DISTINCT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "LEFT JOIN FETCH b.category " +
                            "WHERE b.period = :period ORDER BY b.category.name",
                    Budget.class);
            query.setParameter("period", period);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca budget per periodo", e);
        }
    }

    @Override
    public List<Budget> findByCategory(Category category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Budget> query = session.createQuery(
                    "SELECT DISTINCT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "LEFT JOIN FETCH b.category " +
                            "WHERE b.category = :category ORDER BY b.period.startDate DESC",
                    Budget.class);
            query.setParameter("category", category);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca budget per categoria", e);
        }
    }

    @Override
    public List<Budget> findGeneralBudgets() {
        try (Session session = sessionFactory.openSession()) {
            Query<Budget> query = session.createQuery(
                    "SELECT DISTINCT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "WHERE b.category IS NULL ORDER BY b.period.startDate DESC",
                    Budget.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca budget generali", e);
        }
    }

    @Override
    public List<Budget> findActiveBudgets() {
        try (Session session = sessionFactory.openSession()) {
            Query<Budget> query = session.createQuery(
                    "SELECT DISTINCT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "LEFT JOIN FETCH b.category " +
                            "WHERE b.active = true ORDER BY b.period.startDate DESC",
                    Budget.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca budget attivi", e);
        }
    }

    @Override
    public List<Budget> findOverBudgets() {
        try (Session session = sessionFactory.openSession()) {
            Query<Budget> query = session.createQuery(
                    "SELECT DISTINCT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "LEFT JOIN FETCH b.category " +
                            "WHERE b.actualExpenses > b.plannedExpenses ORDER BY (b.actualExpenses - b.plannedExpenses) DESC",
                    Budget.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca budget sforati", e);
        }
    }

    @Override
    public List<Budget> findByPeriodOrderByVariance(Period period) {
        try (Session session = sessionFactory.openSession()) {
            Query<Budget> query = session.createQuery(
                    "SELECT DISTINCT b FROM Budget b " +
                            "LEFT JOIN FETCH b.period " +
                            "LEFT JOIN FETCH b.category " +
                            "WHERE b.period = :period ORDER BY ((b.actualIncome - b.actualExpenses) - (b.plannedIncome - b.plannedExpenses)) DESC",
                    Budget.class);
            query.setParameter("period", period);
            return query.getResultList();
        } catch (Exception e) {
            throw new RuntimeException("Errore ricerca budget per varianza", e);
        }
    }

    @Override
    public long count() {
        try (Session session = sessionFactory.openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(*) FROM Budget", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RuntimeException("Errore conteggio budget", e);
        }
    }
}